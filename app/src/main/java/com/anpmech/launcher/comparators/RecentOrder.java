/*
 * Copyright 2015-2017 Hayai Software
 * Copyright 2018 The KeikaiLauncher Project
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

package com.anpmech.launcher.comparators;

import com.anpmech.launcher.LaunchableActivity;

import java.util.Comparator;


public class RecentOrder implements Comparator<LaunchableActivity> {

    @Override
    public int compare(final LaunchableActivity lhs, final LaunchableActivity rhs) {
        final long lhsLaunchTime = lhs.getLaunchTime();
        final long rhsLaunchTime = rhs.getLaunchTime();
        final int compare;

        if (lhsLaunchTime > rhsLaunchTime) {
            compare = -1;
        } else if (lhsLaunchTime < rhsLaunchTime) {
            compare = 1;
        } else {
            compare = 0;
        }

        return compare;
    }
}
