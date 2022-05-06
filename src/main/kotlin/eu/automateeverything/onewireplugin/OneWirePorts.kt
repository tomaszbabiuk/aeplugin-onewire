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

import eu.automateeverything.domain.hardware.BinaryInput
import eu.automateeverything.domain.hardware.OutputPort
import eu.automateeverything.domain.hardware.Relay
import eu.automateeverything.domain.hardware.Temperature

class OneWireTemperatureInputPort(
    override val id: String,
    override val address: ByteArray,
    override var value: Temperature,
    lastSeenTimestamp: Long
) : OneWirePort<Temperature>(lastSeenTimestamp) {

    override val valueClazz = Temperature::class.java
}

class OneWireBinaryInputPort(
    override val id: String,
    val channel: Int,
    override val address: ByteArray,
    override var value: BinaryInput,
    lastSeenTimestamp: Long
) : OneWirePort<BinaryInput>(lastSeenTimestamp) {

    override val valueClazz = BinaryInput::class.java
}

class OneWireRelayPort(
    override val id: String,
    val channel: Int,
    override val address: ByteArray,
    override var value: Relay,
    lastSeenTimestamp: Long
) : OneWirePort<Relay>(lastSeenTimestamp), OutputPort<Relay> {

    override val valueClazz = Relay::class.java

    override fun write(value: Relay) {
        requestedValue = value
    }

    fun commit() {
        if (requestedValue != null) {
            value = Relay(requestedValue!!.value)
            reset()
        }
    }

    override var requestedValue: Relay? = null

    override fun reset() {
        requestedValue = null
    }
}