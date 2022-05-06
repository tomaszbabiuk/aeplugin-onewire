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

import com.dalsemi.onewire.adapter.USerialAdapter
import eu.automateeverything.data.localization.Resource
import eu.automateeverything.data.plugins.PluginCategory
import eu.automateeverything.domain.events.EventBus
import eu.automateeverything.domain.extensibility.PluginMetadata
import eu.automateeverything.domain.hardware.HardwareAdapter
import eu.automateeverything.domain.hardware.HardwarePlugin
import eu.automateeverything.domain.settings.SettingsResolver
import org.pf4j.PluginWrapper
import java.io.File

class OneWirePlugin(
    wrapper: PluginWrapper,
    private val eventBus: EventBus,
    private val settingsResolver: SettingsResolver)
    : HardwarePlugin(wrapper), PluginMetadata{

    override fun start() {
    }

    override fun stop() {
    }

    override fun createAdapters(): List<HardwareAdapter<*>> {
        val settings = settingsResolver.resolve()
        val ds2408AsRelays = settings
            .firstOrNull()
            ?.fields
            ?.get(DS2408RolesSettingGroup.FIELD_ADDRESSES_OF_RELAYS)
            ?.replace(";",",")
            ?.replace(" ","")
            ?.split(",") ?: listOf()


        return listSerialPorts()
            .map { createOneWireAdapter(it, ds2408AsRelays) }
    }

    private fun createOneWireAdapter(serialPort: String, ds2408AsRelays: List<String>): HardwareAdapter<*> {
        return OneWireAdapter(pluginId, serialPort, eventBus, ds2408AsRelays)
    }

    private fun listSerialPorts(): List<String> {
        val system = System.getProperty("os.name")
        if (system.lowercase().startsWith("windows")) {
            return probeFirst10SerialPorts()
        } else {
            val serialPorts = ArrayList<String>()
            val path = File.separator + "dev"
            val directory = File(path)
            if (directory.exists()) {
                val aliases = directory.listFiles()
                if (aliases != null) {
                    for (f in aliases) {
                        if (f.name.startsWith("ttyU")) {
                            val adapterPort = f.absolutePath
                            serialPorts.add(adapterPort)
                        }
                    }
                }
            }

            return serialPorts
        }
    }

    private fun probeFirst10SerialPorts(): List<String> {
        eventBus.broadcastDiscoveryEvent(pluginId, "Looking for 1-wire adapters by probing the first 10 system communication ports (COM0..COM9)")

        val result = ArrayList<String>()
        repeat((0..9).count()) {
            try {
                val portIdentifier = "COM$it"
                val adapter = USerialAdapter()
                adapter.selectPort(portIdentifier)
                adapter.reset()
                adapter.freePort()
                result.add(portIdentifier)
                eventBus.broadcastDiscoveryEvent(pluginId, "1-wire adapter based on ${adapter.adapterName} detected. The port is: $portIdentifier")
            } catch (ex: Exception) {
            }
        }

        return result
    }

    override val name: Resource = R.plugin_name
    override val description: Resource = R.plugin_description
    override val category: PluginCategory = PluginCategory.Hardware

    override val settingGroups = listOf(DS2408RolesSettingGroup())
}