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

package com.anpmech.launcher.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.StringRes;

import com.anpmech.launcher.R;

/**
 * This class is used to retrieve shared preferences used by this package.
 */
public class SharedLauncherPrefs {

    /**
     * The current Context.
     */
    private final Context mContext;

    /**
     * A SharedPreferences object.
     */
    private final SharedPreferences mPreferences;

    /**
     * The sole constructor.
     *
     * @param context The current context.
     */
    public SharedLauncherPrefs(final Context context) {
        mContext = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * This is the string resource of the default order of the launchables.
     *
     * @return The string resource of the default order of the launchables.
     */
    @StringRes
    private static int getDefaultLauncherOrder() {
        return R.string.pref_app_preferred_order_entries_alphabetical;
    }

    /**
     * This returns whether icons should be enabled.
     *
     * @return {@code true} if icons should be enabled, {@code false} otherwise.
     */
    public boolean areIconsEnabled() {
        return !isPrefEnabled(R.string.pref_key_disable_icons, false);
    }

    /**
     * This method returns position of the navigation bar when the phone is rotated 270 degrees.
     * Stock android the value, and the default value, is the right side of the screen.
     *
     * @return "right" if the navigation bar is on the right hand side of the screen when the phone
     * is rotated to 270 degrees. "left" if the navigation bar is on the left side of the screen
     * when the phone is rotated 270 degrees. "invisible" if the navigation bar has been turned
     * off.
     */
    public String get270NavBarPosition() {
        return getValue(R.string.pref_key_landscape_270, R.string.pref_value_landscape_right);
    }

    /**
     * This method returns position of the navigation bar when the phone is rotated 90 degrees.
     * Stock android the value, and the default value, is the right side of the screen.
     *
     * @return "right" if the navigation bar is on the right hand side of the screen when the phone
     * is rotated to 90 degrees. "left" if the navigation bar is on the left side of the screen
     * when the phone is rotated 90 degrees. "invisible" if the navigation bar has been turned off.
     */
    public String get90NavBarPosition() {
        return getValue(R.string.pref_key_landscape_90, R.string.pref_value_landscape_right);
    }

    /**
     * This method returns the order launchables should be ordered in.
     *
     * @return The value of {@link R.string#pref_app_preferred_order_entries_alphabetical},
     * {@link R.string#pref_app_preferred_order_entries_recent} or
     * {@link R.string#pref_app_preferred_order_entries_usages}.
     * @see #getDefaultLauncherOrder()
     */
    private String getLauncherOrder() {
        return getValue(R.string.pref_key_preferred_order, getDefaultLauncherOrder());
    }

    /**
     * The used {@link SharedPreferences} object.
     *
     * @return The used SharedPreferences object.
     */
    public SharedPreferences getPreferences() {
        return mPreferences;
    }

    /**
     * A simple caller to {@link Context#getString(int)}.
     *
     * @param resId The resource to retrieve the string from.
     * @return The string value for the given resource.
     */
    private String getString(@StringRes final int resId) {
        return mContext.getString(resId);
    }

    /**
     * This method returns the value of the
     *
     * @param keyRes     The key resource to get the value for.
     * @param defaultRes The key resource to use as a value if {@code keyRes} is unset.
     * @return The value of either {@code keyRes} or {@code valueRes}, as appropriate.
     */
    private String getValue(@StringRes final int keyRes, @StringRes final int defaultRes) {
        final String prefKey = getString(keyRes);
        final String defaultKey = getString(defaultRes);

        return mPreferences.getString(prefKey, defaultKey);
    }

    /**
     * This returns whether the keyboard should be automatically loaded at startup.
     *
     * @return {@code true} if the keyboard should be automatically loaded at startup,
     * {@code false} otherwise.
     */
    public boolean isKeyboardAutomatic() {
        return isPrefEnabled(R.string.pref_key_auto_keyboard, false);
    }

    /**
     * This method returns if the launchables should be ordered with
     * {@link com.anpmech.launcher.comparators.AlphabeticalOrder}.
     *
     * @return {@code true} if launchables should be in alphabetical order, false otherwise.
     */
    public boolean isOrderedByAlphabetical() {
        return !(isOrderedByRecent() || isOrderedByUsage());
    }

    /**
     * This method returns if the launchables should be ordered with
     * {@link com.anpmech.launcher.comparators.RecentOrder}.
     *
     * @return {@code true} if launchables should be in ordered by recent usage, {@code false}
     * otherwise.
     */
    public boolean isOrderedByRecent() {
        return getLauncherOrder().equals(getString(R.string.pref_value_preferred_order_recent));
    }

    /**
     * This method returns if the launchables should be ordered with
     * {@link com.anpmech.launcher.comparators.UsageOrder}.
     *
     * @return {@code true} if launchables should be ordered by frequency of usage, {@code false}
     * otherwise.
     */
    public boolean isOrderedByUsage() {
        return getLauncherOrder().equals(getString(R.string.pref_value_preferred_order_usage));
    }

    /**
     * This method checks if a {@code boolean} preference is enabled by {@link StringRes} key.
     *
     * @param keyRes         The {@code StringRes} key.
     * @param defaultBoolean The default if the key does not exist.
     * @return {@code true} if the value of the {@code StringRes} boolean value is true,
     * {@code false} otherwise.
     */
    private boolean isPrefEnabled(@StringRes final int keyRes, final boolean defaultBoolean) {
        return mPreferences.getBoolean(getString(keyRes), defaultBoolean);
    }

    /**
     * This method returns if screen rotation should be allowed.
     *
     * @return {@code true} if screen rotation should be permitted, {@code false} otherwise.
     */
    public boolean isRotationAllowed() {
        return isPrefEnabled(R.string.pref_key_allow_rotation, true);
    }
}
