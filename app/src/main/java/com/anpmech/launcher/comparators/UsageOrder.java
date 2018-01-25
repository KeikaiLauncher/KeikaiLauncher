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


public class UsageOrder implements Comparator<LaunchableActivity> {

    @Override
    public int compare(final LaunchableActivity lhs, final LaunchableActivity rhs) {
        final int compare;
        long lhsResult = lhs.getUsageTime();
        long rhsResult = rhs.getUsageTime();

        // Prefer the more accurate usage time. If one is -1L, all will be.
        if (lhsResult == -1L) {
            lhsResult = (long) lhs.getUsageQuantity();
            rhsResult = (long) rhs.getUsageQuantity();
        }

        if (lhsResult > rhsResult) {
            compare = -1;
        } else if (lhsResult < rhsResult) {
            compare = 1;
        } else {
            compare = 0;
        }

        return compare;
    }
}
