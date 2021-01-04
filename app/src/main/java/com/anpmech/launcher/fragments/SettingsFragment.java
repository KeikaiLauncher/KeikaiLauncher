/*
 * Copyright 2015-2017 Hayai Software
 * Copyright 2018-2021 The KeikaiLauncher Project
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

package com.anpmech.launcher.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.anpmech.launcher.R;
import com.anpmech.launcher.ShortcutNotificationManager;
import com.anpmech.launcher.activities.SharedLauncherPrefs;

public class SettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * This field generates a ContentObserver to enable or disable the orientation locked setting depending if
     * rotation is locked or unlocked.
     */
    private final ContentObserver mAccSettingObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            final Preference orientationPreference = findPreference(R.string.pref_key_allow_rotation);

            orientationPreference.setEnabled(isOrientationLocked());

            super.onChange(selfChange);
        }
    };

    /**
     * This method returns whether the system orientation is locked.
     *
     * @return True if orientation is locked, false otherwise.
     */
    private boolean isOrientationLocked() {
        return Settings.System.getInt(getPreferenceScreen().getContext().getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
    }

    private Preference findPreference(@StringRes final int prefKey) {
        return findPreference(getString(prefKey));
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            findPreference(R.string.pref_key_notification_priority).setEnabled(false);
        }

        setUsageStatisticsStatus();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onPause() {
        final PreferenceScreen prefs = getPreferenceScreen();

        prefs.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        prefs.getContext().getContentResolver().unregisterContentObserver(mAccSettingObserver);

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        final Preference orientationPreference = findPreference(R.string.pref_key_allow_rotation);
        final Context context = orientationPreference.getContext();
        final Uri accUri = Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION);

        context.getContentResolver().registerContentObserver(accUri, false, mAccSettingObserver);
        orientationPreference.setEnabled(isOrientationLocked());
        orientationPreference.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
            final String key) {
        final Context context = getPreferenceScreen().getContext();

        // Fragments suck.
        if (context != null) {
            final SharedLauncherPrefs prefs = new SharedLauncherPrefs(context);

            if (key.equals(getString(R.string.pref_key_notification)) ||
                    key.equals(getString(R.string.pref_key_notification_priority))) {
                ShortcutNotificationManager.cancelNotification(context);

                if (prefs.isNotificationEnabled()) {
                    ShortcutNotificationManager.showNotification(context);
                }
            }
        }
    }

    /**
     * This method sets the usage statistics preference status by checking the availability of the
     * UsageStats subsystem.
     */
    private void setUsageStatisticsStatus() {
        final Preference pref = findPreference(R.string.pref_key_modify_usage_statistics);
        final PackageManager pm = pref.getContext().getPackageManager();

        pref.setEnabled(pref.getIntent().resolveActivity(pm) != null);
    }
}
