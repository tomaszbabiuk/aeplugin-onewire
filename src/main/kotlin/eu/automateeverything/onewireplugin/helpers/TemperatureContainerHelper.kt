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

import com.dalsemi.onewire.container.OneWireContainer28
import com.dalsemi.onewire.container.Sleeper
import com.dalsemi.onewire.container.TemperatureContainer
import com.dalsemi.onewire.container.ThreadSleeper
import java.math.BigDecimal
import java.math.RoundingMode

object TemperatureContainerHelper {

    private fun readTemperature(temperatureContainer: TemperatureContainer, sleeper: Sleeper): Double {
        var state: ByteArray = temperatureContainer.readDevice()
        if (temperatureContainer is OneWireContainer28) {
            temperatureContainer.doTemperatureConvert(state, sleeper)
        }
        else {
            temperatureContainer.doTemperatureConvert(state)
        }
        state = temperatureContainer.readDevice()
        val lastTemp: BigDecimal = BigDecimal(temperatureContainer.getTemperature(state)).setScale(3, RoundingMode.CEILING)
        return lastTemp.toDouble()
    }

    fun read(temperatureContainer: TemperatureContainer) : Double {
        return readTemperature(temperatureContainer, ThreadSleeper())
    }
}