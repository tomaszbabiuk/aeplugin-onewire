/*
 * Copyright (c) 2019-2021 Tomasz Babiuk
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

package eu.automateeverything.onewireplugin;

public enum SwitchRole {
    AlarmSensing(0),
    RelayBoard(1),
    BothConfliting(2),
    Unknown(3);

    private int _index;

    SwitchRole(int index) {
        _index = index;
    }

    @Override
    public String toString() {
        return String.valueOf(_index);
    }

    public int toInt() {
        return _index;
    }

    public static SwitchRole fromInt(int i) {
        for(SwitchRole value : values()) {
            if (value.toInt() == i) {
                return value;
            }
        }

        return null;
    }
}