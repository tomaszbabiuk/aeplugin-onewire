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

import eu.automateeverything.data.localization.Resource

object R {
    val plugin_description = Resource(
        "Support for 1-Wire devices (DS2408, DS18B20, DS2480B, DS2402)",
        "Wsparcie dla urządzeń 1-Wire (DS2408, DS18B20, DS2480B, DS2402)"
    )

    val plugin_name = Resource("1-Wire", "1-Wire")

    val ds2408_roles_settings_title = Resource(
        "DS2408 as Relay Boards",
        "Karty przekaźników z DS2408"
    )

    val ds2408_roles_settings_description = Resource(
        "1-wire devices equipped with DS2408 are discovered as Input Boards (by default). If you want to change the role of specific board from 'Input' to 'Relay' - you need to type it's address here. You can type more addresses and separate them with comma or semicolon",
        "Urządzenia wyposażone w czip DS2408 są domyślnie rozpoznawane jako karty wejść. Jeżeli chcesz zminić rolę określonej karty z 'Karty wejść' na 'Kartę przekaźników' - wpisz tutaj jej adres. Możesz wpisać więcej adresów i oddzielić je przecinkiem lub średnikiem"
    )

    val field_relays_ids = Resource(
        "1-wire chips that should be discovered as 'Relay boards'",
        "Układy 1-wire, które powinny być rozpoznawane jako karty przekaźników"
    )
}