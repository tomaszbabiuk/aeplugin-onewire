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

import eu.automateeverything.domain.configurable.*

class DS2408RolesSettingGroup : SettingGroup {

    override val titleRes = R.ds2408_roles_settings_title
    override val descriptionRes = R.ds2408_roles_settings_description

    override val fieldDefinitions: Map<String, FieldDefinition<*>> = mapOf(
        Pair(FIELD_ADDRESSES_OF_RELAYS, StringField(FIELD_ADDRESSES_OF_RELAYS, R.field_relays_ids, 0, "", RequiredStringValidator())),
    )

    companion object {
        const val FIELD_ADDRESSES_OF_RELAYS = "addresses_of_relays"
    }
}