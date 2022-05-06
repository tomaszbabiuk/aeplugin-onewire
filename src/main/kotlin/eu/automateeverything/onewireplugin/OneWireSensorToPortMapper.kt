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

import com.dalsemi.onewire.container.*
import eu.automateeverything.domain.events.EventBus
import eu.automateeverything.domain.hardware.BinaryInput
import eu.automateeverything.domain.hardware.PortIdBuilder
import eu.automateeverything.domain.hardware.Relay
import eu.automateeverything.domain.hardware.Temperature
import eu.automateeverything.onewireplugin.helpers.SwitchContainerHelper
import eu.automateeverything.onewireplugin.helpers.TemperatureContainerHelper
import java.util.*

class OneWireSensorToPortMapper(
    private val owningPluginId: String,
    private val portIdBuilder: PortIdBuilder,
    private val eventBus: EventBus,
    private val ds2408AsRelays: List<String>
) {
    fun map(sensor: OneWireContainer, now: Calendar): List<OneWirePort<*>>? {
        when (sensor) {
            is TemperatureContainer -> {
                return mapTemperatureContainer(sensor, now)
            }
            is SwitchContainer -> {
                return mapSwitchContainer(sensor, now)
            }
            else -> {
                broadcastMessage("${sensor.name}/${sensor.addressAsString}: unsupported (ignoring)")
            }
        }

        return null
    }

    private fun mapSwitchContainer(switch: SwitchContainer, now: Calendar): List<OneWirePort<*>>? {
        switch as OneWireContainer
        val isRelay = ds2408AsRelays.contains(switch.addressAsString)
        try {
            if (isRelay) {
                broadcastMessage("${switch.name}/${switch.addressAsString} discovered as Relay board!")

                val channelsCount = SwitchContainerHelper.readChannelsCount(switch)
                val inputReadings = SwitchContainerHelper.read(switch, false)
                return (0 until channelsCount)
                    .map { channel ->
                        val inputPortId = portIdBuilder.buildPortId(switch.addressAsString, channel.toString(), "R")
                        val initialValueRaw = inputReadings[channel].level
                        val initialValueInverted = Relay(!initialValueRaw)
                        OneWireRelayPort(inputPortId, channel, switch.address, initialValueInverted, now.timeInMillis)
                    }
                    .toList()
            } else {
                broadcastMessage("${switch.name}/${switch.addressAsString} discovered as Input board!")

                val channelsCount = SwitchContainerHelper.readChannelsCount(switch)
                val inputReadings = SwitchContainerHelper.read(switch, false)
                return (0 until channelsCount)
                    .map { channel ->
                        val inputPortId = portIdBuilder.buildPortId(switch.addressAsString, channel.toString(), "B")
                        val initialValueRaw = inputReadings[channel].level
                        val initialValue = BinaryInput(initialValueRaw)
                        OneWireBinaryInputPort(inputPortId, channel, switch.address, initialValue, now.timeInMillis)
                    }
                    .toList()
            }
        } catch (ex: Exception) {
            broadcastMessage("There's been a problem reading container: ${switch.name}/${switch.addressAsString}")
        }

        return null
    }

    private fun broadcastMessage(message: String) {
        eventBus.broadcastDiscoveryEvent(owningPluginId, message)
    }

    private fun mapTemperatureContainer(temperatureSensor: TemperatureContainer, now: Calendar): List<OneWireTemperatureInputPort>? {
        temperatureSensor as OneWireContainer
        broadcastMessage("${temperatureSensor.name}/${temperatureSensor.addressAsString} discovered as Thermometer!")

        try {
            val inputPortId = portIdBuilder.buildPortId(temperatureSensor.addressAsString, 0.toString(), "T")
            val initialValueRaw = TemperatureContainerHelper.read(temperatureSensor).toBigDecimal() + 273.15.toBigDecimal()
            val initialValue = Temperature(initialValueRaw)
            return listOf(OneWireTemperatureInputPort(inputPortId, temperatureSensor.address, initialValue, now.timeInMillis))
        } catch (ex: Exception) {
            broadcastMessage("There's been a problem reading container: ${temperatureSensor.name}/${temperatureSensor.addressAsString}")
        }

        return null
    }
}