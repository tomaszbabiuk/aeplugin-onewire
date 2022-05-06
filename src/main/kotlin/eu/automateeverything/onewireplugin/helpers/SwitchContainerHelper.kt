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

package eu.automateeverything.onewireplugin.helpers

import com.dalsemi.onewire.OneWireException
import com.dalsemi.onewire.container.SwitchContainer
import kotlinx.coroutines.delay

object SwitchContainerHelper {

    fun readChannelsCount(container: SwitchContainer): Int {
        return try {
            val registersData = container.readDevice()
            container.getNumberChannels(registersData)
        } catch (ex: java.lang.Exception) {
            0
        }
    }

    @Throws(OneWireException::class)
    suspend fun execute(switchContainer: SwitchContainer, values: Array<Boolean>) {
        var trial = 1
        while (trial < 4) {
            if (executeWithBackoff(switchContainer, values)) {
                return
            }
            trial++
        }
    }

    private suspend fun executeWithBackoff(
        switchContainer: SwitchContainer,
        values: Array<Boolean>
    ): Boolean {
        return try {
            executeInternal(switchContainer, values)
            true
        } catch (ex: Exception) {
            delay(500)
            false
        }
    }

    @Throws(OneWireException::class)
    private fun executeInternal(
        switchContainer: SwitchContainer,
        values: Array<Boolean>
    ) {
        val data = switchContainer.readDevice()
        values.forEachIndexed { index, newValue ->
            switchContainer.setLatchState(index, newValue, false, data)
        }
        switchContainer.writeDevice(data)
    }

    @Throws(OneWireException::class)
    fun read(switchContainer: SwitchContainer, sensing: Boolean): Array<SwitchContainerReading> {
        return try {
            val readings = readInternal(switchContainer, sensing, false)
            throwIfAllReadingsAreSensed(readings)
            readings
        } catch (ex: Exception) {
            val readings = readInternal(switchContainer, sensing, true)
            throwIfAllReadingsAreSensed(readings)
            readings
        }
    }

    @Throws(OneWireException::class)
    private fun throwIfAllReadingsAreSensed(readings: Array<SwitchContainerReading>) {
        for (reading in readings) {
            if (!reading.isSensed) {
                return
            }
        }

        throw OneWireException("All readings are SENSED! It's impossible unless there's an error on one-wire line. Ignoring...")
    }

    @Throws(OneWireException::class)
    private fun readInternal(
        switchContainer: SwitchContainer,
        sensing: Boolean,
        lastReadingHasFailed: Boolean
    ): Array<SwitchContainerReading> {
        val state = switchContainer.readDevice()
        val channelsCount = switchContainer.getNumberChannels(state)
        val result = Array(channelsCount) {
            SwitchContainerReading(
                level = false,
                isSensed = false
            )
        }
        if (sensing) {
            for (channel in 0 until channelsCount) {
                switchContainer.setLatchState(channel, true, false, state)
            }
            switchContainer.writeDevice(state)
            for (channel in 0 until channelsCount) {
                val level = switchContainer.getLevel(channel, state)
                //Last reading has failed?... disable sensing in this round!
                val sensed = !lastReadingHasFailed && switchContainer.getSensedActivity(channel, state)
                switchContainer.setLatchState(channel, !level, false, state)
                result[channel] = SwitchContainerReading(level, sensed)
            }
            switchContainer.clearActivity()
        } else {
            for (channel in 0 until channelsCount) {
                val level = switchContainer.getLevel(channel, state)
                result[channel] = SwitchContainerReading(level, false)
            }
        }
        return result
    }

    data class SwitchContainerReading(val level: Boolean, val isSensed: Boolean)
}