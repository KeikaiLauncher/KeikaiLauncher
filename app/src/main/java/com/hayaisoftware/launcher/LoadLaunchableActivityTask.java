/*
 * Copyright (c) 2015-2017 Hayai Software
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

package com.hayaisoftware.launcher;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.hayaisoftware.launcher.threading.SimpleTaskConsumerManager;


public final class LoadLaunchableActivityTask implements SimpleTaskConsumerManager.Task {

    private final LaunchableAdapter<LaunchableActivity> mAdapter;

    private final ActivityInfo mInfo;

    private final PackageManager mPackageManager;

    private LoadLaunchableActivityTask(final ActivityInfo info, final PackageManager packageManager,
            final LaunchableAdapter<LaunchableActivity> adapter) {
        mInfo = info;
        mPackageManager = packageManager;
        mAdapter = adapter;
    }

    @Override
    public boolean doTask() {
        final LaunchableActivity activity =
                LaunchableActivity.getLaunchable(mInfo, mPackageManager);

        mAdapter.add(activity);

        return true;
    }

    public static final class Factory {

        private final LaunchableAdapter<LaunchableActivity> mAdapter;

        private final PackageManager mPackageManager;

        public Factory(final PackageManager packageManager,
                final LaunchableAdapter<LaunchableActivity> adapter) {
            mPackageManager = packageManager;
            mAdapter = adapter;
        }

        public SimpleTaskConsumerManager.Task create(final ResolveInfo info) {
            return new LoadLaunchableActivityTask(info.activityInfo, mPackageManager, mAdapter);
        }
    }
}
