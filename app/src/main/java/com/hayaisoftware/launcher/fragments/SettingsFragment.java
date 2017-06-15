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

package com.hayaisoftware.launcher.fragments;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hayaisoftware.launcher.R;
import com.hayaisoftware.launcher.ShortcutNotificationManager;

public class SettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_PREF_ALLOW_ROTATION = "pref_allow_rotation";

    public static final String KEY_PREF_AUTO_KEYBOARD = "pref_autokeyboard";

    public static final String KEY_PREF_NOTIFICATION = "pref_notification";

    public static final String KEY_PREF_NOTIFICATION_PRIORITY = "pref_notification_priority";

    public static final String KEY_PREF_NOTIFICATION_PRIORITY_LOW = "min";

    public static final String KEY_PREF_NOTIFICATION_PRIORITY_HIGH = "max";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            //remove priority preference (not supported)
            final PreferenceCategory notificationCategory =
                    (PreferenceCategory) findPreference("pref_category_notification");
            notificationCategory.removePreference(notificationCategory);

            final Preference priorityPreference = findPreference(KEY_PREF_NOTIFICATION_PRIORITY);
            notificationCategory.removePreference(priorityPreference);
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
                                          final String key) {
        if (key.equals(KEY_PREF_NOTIFICATION) || key.equals(KEY_PREF_NOTIFICATION_PRIORITY)) {
            final boolean notificationEnabled =
                    sharedPreferences.getBoolean(KEY_PREF_NOTIFICATION, false);
            final Context context = getActivity();

            // Fragments suck.
            if (context != null) {
                ShortcutNotificationManager.cancelNotification(context);
            }

            if (notificationEnabled) {
                final String strPriority =
                        sharedPreferences.getString(KEY_PREF_NOTIFICATION_PRIORITY,
                                KEY_PREF_NOTIFICATION_PRIORITY_LOW);

                // Fragments suck.
                if (context != null) {
                    ShortcutNotificationManager.showNotification(context, strPriority);
                }
            }
        }
    }
}
