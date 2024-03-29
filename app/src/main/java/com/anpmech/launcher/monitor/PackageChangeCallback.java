/*
 * Copyright 2015-2017 Hayai Software
 * Copyright 2018-2022 The KeikaiLauncher Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anpmech.launcher.monitor;

import android.app.Activity;

public interface PackageChangeCallback {

    /**
     * Called when a package appears for any reason.
     *
     * @param activityName The name of the {@link Activity} of the package which appeared.
     * @param uids         Unix-type UIDs which use this activity, not to be confused with user serials.
     */
    void onPackageAppeared(String activityName, int[] uids);

    /**
     * Called when a package disappears for any reason.
     *
     * @param activityName The name of the {@link Activity} of the package which disappeared.
     * @param uids         Unix-type UIDs which use this activity, not to be confused with user serials.
     */
    void onPackageDisappeared(String activityName, int[] uids);

    /**
     * Called when an existing package is updated or its disabled state changes.
     *
     * @param activityName The name of the {@link Activity} of the package which was modified.
     * @param uid          Unix-type UID which use this activity, not to be confused with a user serial.
     */
    void onPackageModified(String activityName, int uid);
}
