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

package com.anpmech.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.UserManager;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;


public class LaunchableActivity {

    private static final String TAG = "LaunchableActivity";

    private final String mActivityLabel;

    @DrawableRes
    private final int mIconResource;

    private final Intent mLaunchIntent;

    private final Object mLock = new Object();

    /**
     * The user serial, to be used to retrieve a {@link android.os.UserHandle} as necessary.
     * Defined as {@code Long.MIN_VALUE} if there is no user serial assigned to this object.
     */
    private final long mUserSerial;

    private Drawable mActivityIcon;

    private long mLastLaunchTime;

    private int mPriority;

    private long mUsageTime;

    private int mUsagesQuantity;

    /**
     * This is the constructor for LaunchableActivities, used in a {@link LaunchableAdapter}, for
     * API 21+.
     *
     * @param info           Information to derive the LaunchableActivity from.
     * @param manager        The service to retrieve user information about the activity from.
     * @param shouldLoadIcon Whether the icon should be loaded from the {@code info}.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public LaunchableActivity(@NonNull final LauncherActivityInfo info, final UserManager manager,
                              final boolean shouldLoadIcon) {
        mLaunchIntent = getLaunchableIntent(info.getComponentName());
        mActivityLabel = info.getLabel().toString();
        mIconResource = Integer.MIN_VALUE;
        mUserSerial = manager.getSerialNumberForUser(info.getUser());

        if (shouldLoadIcon) {
            mActivityIcon = info.getBadgedIcon(R.dimen.app_icon_size);
        } else {
            mActivityIcon = null;
        }
    }

    /**
     * This is a constructor used for manual {@code LaunchableActivity} creation.
     *
     * @param intent The {@link Intent} to create this from.
     * @param label  The label to construct this object with.
     * @param icon   The icon to use for this object. If null, the Android icon will be loaded.
     */
    public LaunchableActivity(@NonNull final Intent intent, @NonNull final String label,
                              @DrawableRes final int icon) {
        mLaunchIntent = intent;
        mActivityLabel = label;
        mIconResource = icon;
        mUserSerial = Long.MIN_VALUE;
    }

    /**
     * This is the constructor for LaunchableActivities, used in a {@link LaunchableAdapter}, for
     * APIs 15-20. If this constructor is used, {@code LaunchableActivity.getActivityIcon()} will
     * need to be called to load the icon from the icon resource.
     *
     * @param info    Information to derive the LaunchableActivity from.
     * @param prefs   The {@link SharedPreferences} to load the label for this from.
     * @param manager The {@link PackageManager} to load the label for this from. If null, the
     *                local store will not cache the label.
     */
    public LaunchableActivity(@NonNull final ResolveInfo info,
                              @NonNull final SharedPreferences prefs,
                              @Nullable final PackageManager manager) {
        final ActivityInfo activityInfo = info.activityInfo;
        final ComponentName name =
                new ComponentName(activityInfo.packageName, activityInfo.name);
        mLaunchIntent = getLaunchableIntent(name);
        mIconResource = info.getIconResource();

        /**
         * Returns the actual label from the info and stores it locally, or retrieve it locally.
         */
        if (prefs.contains(activityInfo.packageName) && manager != null) {
            mActivityLabel = prefs.getString(activityInfo.packageName, null);
        } else {
            mActivityLabel = info.loadLabel(manager).toString();
            prefs.edit().putString(activityInfo.packageName, mActivityLabel).apply();
        }

        mUserSerial = Long.MIN_VALUE;
    }

    private static Intent getLaunchableIntent(final ComponentName componentName) {
        final Intent launchIntent = Intent.makeMainActivity(componentName);

        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        return launchIntent;
    }

    public boolean isUserKnown() {
        return mUserSerial != Long.MIN_VALUE;
    }

    public void addUsage() {
        mUsagesQuantity++;
    }

    public void deleteActivityIcon() {
        synchronized (mLock) {
            mActivityIcon = null;
        }
    }

    @Nullable
    public Drawable getActivityIcon(final Context context, final int iconSizePixels) {
        if (!isIconLoaded()) {
            synchronized (mLock) {
                mActivityIcon = context.getResources().getDrawable(mIconResource);

                //rescaling the icon if it is bigger than the target size
                //TODO do this when it is not a bitmap drawable?
                if (mActivityIcon instanceof BitmapDrawable) {
                    if (mActivityIcon.getIntrinsicHeight() > iconSizePixels &&
                            mActivityIcon.getIntrinsicWidth() > iconSizePixels) {
                        //noinspection deprecation
                        mActivityIcon = new BitmapDrawable(
                                Bitmap.createScaledBitmap(
                                        ((BitmapDrawable) mActivityIcon).getBitmap()
                                        , iconSizePixels, iconSizePixels, false));
                    }
                }
            }
        }
        return mActivityIcon;
    }

    /**
     * The user serial, to be used to retrieve a {@link android.os.UserHandle} as necessary.
     *
     * @return A user serial, {@code Long.MIN_VALUE} if there is no user serial assigned to this
     * object.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public long getUserSerial() {
        return mUserSerial;
    }

    public ComponentName getComponent() {
        return mLaunchIntent.getComponent();
    }

    public Intent getLaunchIntent() {
        return mLaunchIntent;
    }

    public long getLaunchTime() {
        return mLastLaunchTime;
    }

    public void setLaunchTime(final long timestamp) {
        mLastLaunchTime = timestamp;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(final int priority) {
        mPriority = priority;
    }

    public int getUsageQuantity() {
        return mUsagesQuantity;
    }

    public void setUsageQuantity(final int usagesQuantity) {
        mUsagesQuantity = usagesQuantity;
    }

    /**
     * This method returns the usage time.
     * <p>
     * The usage time will be set if it is supported by Android and the permission is
     * granted by the user.
     *
     * @return The usage time, -1L if not supported for whatever reason.
     */
    public long getUsageTime() {
        return mUsageTime;
    }

    /**
     * This method sets the usage time.
     *
     * @param usageTime The usage time, this shall be -1L if not supported for whatever reason.
     */
    public void setUsageTime(final long usageTime) {
        mUsageTime = usageTime;
    }

    public boolean isIconLoaded() {
        return mActivityIcon != null;
    }

    public void setLaunchTime() {
        mLastLaunchTime = System.currentTimeMillis() / 1000;
    }

    @Override
    public String toString() {
        return mActivityLabel;
    }
}
