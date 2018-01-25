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

package com.anpmech.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;


public class LaunchableActivity {

    private static final String TAG = "LaunchableActivity";

    private final String mActivityLabel;

    @DrawableRes
    private final int mIconResource;

    private final Intent mLaunchIntent;

    private final Object mLock = new Object();

    private Drawable mActivityIcon;

    private long mLastLaunchTime;

    private int mPriority;

    private long mUsageTime;

    private int mUsagesQuantity;

    public LaunchableActivity(@NonNull final Intent intent, @NonNull final String activityLabel,
            @DrawableRes final int iconResource) {
        mLaunchIntent = intent;
        mActivityLabel = activityLabel;
        mIconResource = iconResource;
    }

    /**
     * This is a convenience method to create a LaunchableActivity.
     *
     * @param info The {@link ActivityInfo} to extract information from.
     * @param pm   The PackageManager to use to extract information from.
     * @return A LaunchableActivity based off the ActivityInfo given.
     */
    public static LaunchableActivity getLaunchable(@NonNull final ActivityInfo info,
            @NonNull final PackageManager pm) {
        final Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        final String label = info.loadLabel(pm).toString();
        final int iconResource = info.getIconResource();

        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        launchIntent.setComponent(new ComponentName(info.packageName, info.name));

        return new LaunchableActivity(launchIntent, label, iconResource);
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
                try {
                    final PackageManager pm = context.getPackageManager();
                    final Resources resources = pm.getResourcesForActivity(getComponent());

                    //noinspection deprecation
                    mActivityIcon = resources.getDrawable(mIconResource);

                } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                    Log.e(TAG, "Error when trying to inflate a launcher icon.", e);
                }

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

    public ComponentName getComponent() {
        return mLaunchIntent.getComponent();
    }

    public Intent getLaunchIntent() {
        return mLaunchIntent;
    }

    public long getLaunchTime() {
        return mLastLaunchTime;
    }

    public int getPriority() {
        return mPriority;
    }

    public int getUsageQuantity() {
        return mUsagesQuantity;
    }

    /**
     * This method returns the usage time.
     *
     * The usage time will be set if it is supported by Android and the permission is
     * granted by the user.
     *
     * @return The usage time, -1L if not supported for whatever reason.
     */
    public long getUsageTime() {
        return mUsageTime;
    }

    public boolean isIconLoaded() {
        return mActivityIcon != null;
    }

    public void setLaunchTime() {
        mLastLaunchTime = System.currentTimeMillis() / 1000;
    }

    public void setLaunchTime(final long timestamp) {
        mLastLaunchTime = timestamp;
    }

    public void setPriority(final int priority) {
        mPriority = priority;
    }

    public void setUsageQuantity(final int usagesQuantity) {
        mUsagesQuantity = usagesQuantity;
    }

    /**
     * This method sets the usage time.
     *
     * @param usageTime The usage time, this shall be -1L if not supported for whatever reason.
     */
    public void setUsageTime(final long usageTime) {
        mUsageTime = usageTime;
    }

    @Override
    public String toString() {
        return mActivityLabel;
    }
}
