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

import eu.automateeverything.data.hardware.PortValue
import eu.automateeverything.domain.events.EventBus
import eu.automateeverything.domain.hardware.*
import java.util.*

abstract class OneWirePort<V : PortValue>(
    factoryId: String,
    adapterId: String,
    portId: String,
    eventBus: EventBus,
    valueClazz: Class<V>,
    capabilities: PortCapabilities,
    lastSeenTimestamp: Long,
) : Port<V>(factoryId, adapterId, portId, eventBus, valueClazz, capabilities, 10 + 1000L) {

    init {
        updateLastSeenTimeStamp(lastSeenTimestamp)
    }

    abstract val address: ByteArray
    abstract var value: V
    var lastUpdateMs: Long = Calendar.getInstance().timeInMillis

    override fun readInternal(): V {
        return value
    }

    fun update(now: Long, value: V) {
        lastUpdateMs = now
        this.value = value
    }
}

class OneWireTemperatureInputPort(
    factoryId: String,
    adapterId: String,
    portId: String,
    eventBus: EventBus,
    override val address: ByteArray,
    override var value: Temperature,
    lastSeenTimestamp: Long
) :
    OneWirePort<Temperature>(
        factoryId,
        adapterId,
        portId,
        eventBus,
        Temperature::class.java,
        PortCapabilities(canRead = true, canWrite = false),
        lastSeenTimestamp
    )

class OneWireBinaryInputPort(
    factoryId: String,
    adapterId: String,
    portId: String,
    eventBus: EventBus,
    val channel: Int,
    override val address: ByteArray,
    override var value: BinaryInput,
    lastSeenTimestamp: Long
) :
    OneWirePort<BinaryInput>(
        factoryId,
        adapterId,
        portId,
        eventBus,
        BinaryInput::class.java,
        PortCapabilities(canRead = true, canWrite = false),
        lastSeenTimestamp
    )

class OneWireRelayPort(
    factoryId: String,
    adapterId: String,
    portId: String,
    eventBus: EventBus,
    val channel: Int,
    override val address: ByteArray,
    override var value: Relay,
    lastSeenTimestamp: Long
) :
    OneWirePort<Relay>(
        factoryId,
        adapterId,
        portId,
        eventBus,
        Relay::class.java,
        PortCapabilities(canRead = true, canWrite = true),
        lastSeenTimestamp
    ) {

    fun commit() {
        if (requestedValue != null) {
            value = Relay(requestedValue!!.value)
            reset()
        }
    }
}
