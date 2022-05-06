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

import eu.automateeverything.domain.hardware.InputPort
import eu.automateeverything.data.hardware.PortValue
import java.util.*

abstract class OneWirePort<V: PortValue>(override var lastSeenTimestamp: Long) : InputPort<V> {
    abstract val address: ByteArray
    abstract var value: V
    var lastUpdateMs: Long = Calendar.getInstance().timeInMillis

    override val sleepInterval: Long = 10 + 1000L

    override fun read(): V {
        return value
    }

    fun update(now: Long, value: V) {
        lastUpdateMs = now
        this.value = value
    }
}