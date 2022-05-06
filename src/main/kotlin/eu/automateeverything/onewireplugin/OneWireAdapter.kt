/*
 * Copyright (c) 2019-2022 Tomasz Babiuk
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.automateeverything.onewireplugin

import com.dalsemi.onewire.OneWireException
import com.dalsemi.onewire.adapter.DSPortAdapter
import com.dalsemi.onewire.adapter.OneWireIOException
import com.dalsemi.onewire.adapter.USerialAdapter
import com.dalsemi.onewire.container.OneWireContainer
import com.dalsemi.onewire.container.SwitchContainer
import com.dalsemi.onewire.container.TemperatureContainer
import eu.automateeverything.domain.events.EventBus
import eu.automateeverything.domain.events.PortUpdateType
import eu.automateeverything.domain.hardware.*
import eu.automateeverything.onewireplugin.helpers.SwitchContainerHelper
import eu.automateeverything.onewireplugin.helpers.TemperatureContainerHelper
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class OneWireAdapter(
    owningPluginId: String,
    private val serialPortName: String,
    eventBus: EventBus,
    private val ds2408AsRelays: List<String>
)
    : HardwareAdapterBase<OneWirePort<*>>(owningPluginId, "1-WIRE $serialPortName", eventBus) {

    private val logger = LoggerFactory.getLogger(OneWireAdapter::class.java)
    private var mapper: OneWireSensorToPortMapper

    private val executionQueue = ConcurrentLinkedQueue<ValueSnapshot>()

    init {
        val portIdBuilder = PortIdBuilder(owningPluginId)
        mapper = OneWireSensorToPortMapper(owningPluginId, portIdBuilder, eventBus, ds2408AsRelays)
    }

    var operationScope: CoroutineScope? = null


    override fun executePendingChanges() {
        val portsGroupedByAddress = ports
            .values
            .filterIsInstance<OneWireRelayPort>()
            .groupBy { it.address }

        portsGroupedByAddress.forEach { (address, u) ->
            val channels = u.size
            val sortedPorts = u.sortedBy { it.channel }

            val prevValues = Array(channels) { i ->
                val bigDecimal = sortedPorts[i].value.value
                bigDecimal == BigDecimal.ONE
            }
            val newValues = Array(channels) { i ->
                val bigDecimal = sortedPorts[i].requestedValue?.value ?: sortedPorts[i].value.value
                bigDecimal == BigDecimal.ONE
            }

            val changedItems = newValues.filterIndexed { index, newValue -> prevValues[index] != newValue }
            if (changedItems.isNotEmpty()) {
                val valueSnapshot = ValueSnapshot(address, prevValues, newValues)
                executionQueue.add(valueSnapshot)
            }
        }
    }

    @Throws(OneWireException::class)
    private fun initializeAdapter(port: String): USerialAdapter {
        val adapter = USerialAdapter()
        adapter.selectPort(port)
        adapter.reset()
        adapter.beginExclusive(true)
        adapter.setSearchAllDevices()
        adapter.targetAllFamilies()
        adapter.speed = DSPortAdapter.SPEED_REGULAR
        return adapter
    }

    private fun freeAdapter(adapter: USerialAdapter) {
        adapter.endExclusive()
        adapter.freePort()
    }

    override fun start() {
        if (operationScope != null) {
            operationScope!!.cancel("Adapter already started")
        }

        //discovery
        val now = Calendar.getInstance()
        val adapterForDiscoveryOnly = initializeAdapter(serialPortName)
        val allContainers = searchForOneWireSensors(adapterForDiscoveryOnly)
        allContainers
            .mapNotNull { mapper.map(it, now)}
            .flatten()
            .forEach { ports[it.id] = it }
        freeAdapter(adapterForDiscoveryOnly)

        operationScope = CoroutineScope(Dispatchers.IO)

        operationScope?.launch {
            while (isActive) {
                maintenanceLoop()
                delay(100)
            }
        }
    }

    private fun searchForOneWireSensors(adapter: USerialAdapter): List<OneWireContainer> {
        return adapter
            .allDeviceContainers
            .toList()
            .filterIsInstance<OneWireContainer>()
    }

    override fun stop() {
        operationScope?.cancel("Stop called")
    }

    override suspend fun internalDiscovery(mode: DiscoveryMode) {
        logDiscovery("Discovery of 1-wire adapters is disabled. Devices are discovered only once (on initial startup)")
        addPotentialNewPorts(ports.values.toList())
    }

    private suspend fun maintenanceLoop() = coroutineScope {
        val now = Calendar.getInstance()
        var adapter: USerialAdapter? = null
        try {
            adapter = initializeAdapter(serialPortName)

            maintainExecutionOfRelays(adapter)

            val discoveredAddresses = ports.values.map { it.address }.distinct()
            discoveredAddresses.forEach {
                if (executionQueue.isEmpty()) {
                    val container = adapter.getDeviceContainer(it)
                    maintainAsThermometer(container, now)
                    maintainAsBinaryInput(container, now)
                }
            }

            freeAdapter(adapter)
        }
        catch (ex: OneWireIOException) {
            logger.error("Unhandled exception from OneWire", ex)

            try {
                adapter?.reset()
            } catch (nex: Exception) {
                logger.info("Trying to reset the adapter that has failed!")
            }

            if (ex.message == "OneWireContainer28-temperature conversion not complete") {
               logger.info(ex.message)
            } else {
                ports.values.forEach {
                    broadcastPortUpdate(PortUpdateType.LastSeenChange, it)
                }
            }
        }
    }

    private suspend fun maintainExecutionOfRelays(adapter: USerialAdapter) {
        if (!executionQueue.isEmpty()) {
            val snapshot = executionQueue.poll()
            val container = adapter.getDeviceContainer(snapshot.containerAddress) as SwitchContainer
            val newValuesInverted = Array(snapshot.newValues.size) {
                !snapshot.newValues[it]
            }
            SwitchContainerHelper.execute(container, newValuesInverted)

            ports
                .values
                .filter { port -> port.address.contentEquals(snapshot.containerAddress) }
                .filterIsInstance<OneWireRelayPort>()
                .forEach { port ->
                    val oldValue = port.value.value
                    port.commit()
                    val newValue = port.value.value
                    if (newValue != oldValue) {
                        broadcastPortUpdate(PortUpdateType.ValueChange, port)
                    }
                }
        }
    }

    private fun maintainAsThermometer(container: OneWireContainer?, now: Calendar) {
        if (container is TemperatureContainer) {
            ports.values
                .filter { port -> port.address.contentEquals(container.address) }
                .map { port ->
                    port.lastSeenTimestamp = now.timeInMillis
                    port
                }
                .filterIsInstance<OneWireTemperatureInputPort>()
                .filter { port -> port.lastUpdateMs + 30000 < now.timeInMillis }
                .forEach { port ->
                    val newTemperatureK = TemperatureContainerHelper.read(container).toBigDecimal() + 273.15.toBigDecimal()
                    if (newTemperatureK != port.value.value) {
                        port.update(now.timeInMillis, Temperature(newTemperatureK))
                        broadcastPortUpdate(PortUpdateType.ValueChange, port)
                    }
                }
        }
    }

    private fun maintainAsBinaryInput(container: OneWireContainer?, now: Calendar) {
        if (container is SwitchContainer) {
            val isRelay = ds2408AsRelays.contains(container.addressAsString)
            if (isRelay) {
                ports.values
                    .filter { port -> port.address.contentEquals(container.address) }
                    .forEach { port ->
                        port.lastSeenTimestamp = now.timeInMillis
                    }
            } else {
                val readings = SwitchContainerHelper.read(container, true)

                ports.values
                    .filter { port -> port.address.contentEquals(container.address) }
                    .map { port ->
                        port.lastSeenTimestamp = now.timeInMillis
                        port
                    }
                    .filterIsInstance<OneWireBinaryInputPort>()
                    .forEach { port ->
                        val channel = port.channel
                        val reading = readings[channel]
                        val newBinaryReading = if (reading.isSensed) !port.value.value else reading.level
                        if (newBinaryReading != port.value.value) {
                            port.update(now.timeInMillis, BinaryInput(newBinaryReading))
                            broadcastPortUpdate(PortUpdateType.ValueChange, port)
                        }
                    }
            }
        }
    }

    data class ValueSnapshot(
        val containerAddress: ByteArray,
        val previousValues: Array<Boolean>,
        val newValues: Array<Boolean>) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ValueSnapshot

            if (!containerAddress.contentEquals(other.containerAddress)) return false
            if (!previousValues.contentEquals(other.previousValues)) return false
            if (!newValues.contentEquals(other.newValues)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = containerAddress.contentHashCode()
            result = 31 * result + previousValues.contentHashCode()
            result = 31 * result + newValues.contentHashCode()
            return result
        }
    }
}
