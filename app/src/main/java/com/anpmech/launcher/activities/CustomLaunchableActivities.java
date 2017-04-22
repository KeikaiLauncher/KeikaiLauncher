/*
 * Copyright 2015-2017 Hayai Software
 * Copyright 2018-2020 The KeikaiLauncher Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.anpmech.launcher.LaunchableActivity;
import com.anpmech.launcher.R;

import java.util.ArrayList;
import java.util.Collection;

public class CustomLaunchableActivities {

    private static final ComponentName MAPS = new ComponentName("com.google.android.apps.maps",
            "com.google.android.maps.MapsActivity");

    private final int mIconResource;

    public CustomLaunchableActivities(final Context context) {
        int iconResource = 0;
        final PackageManager packageManager = context.getPackageManager();
        try {
            final ActivityInfo activityInfo =
                    packageManager.getActivityInfo(MAPS, PackageManager.GET_META_DATA);
            iconResource = activityInfo.getIconResource();
        } catch (final PackageManager.NameNotFoundException e) {
            Log.e("Custom", "Couldn't get MAPS name", e);
        }

        mIconResource = iconResource;
    }

    private static LaunchableActivity getDuckDuckGoLauncher(final String query) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme("https");
        builder.authority("www.duckduckgo.com");
        builder.appendQueryParameter("q", query);

        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, builder.build());

        return new DuckDuckGoLauncher(browserIntent, "Duck Duck Go",
                R.drawable.ic_duckduckgo_icon);
    }

    public Collection<LaunchableActivity> getCustomLaunchables(final String query) {
        final Collection<LaunchableActivity> activities = new ArrayList<>(3);

        activities.add(getDuckDuckGoLauncher(query));
        if (mIconResource != 0) {
            activities.add(getGoogleMapsNavigation(query));
            activities.add(getGoogleMapsNearby(query));
        }

        return activities;
    }

    private LaunchableActivity getGoogleMaps(final Uri uri, final String label) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        intent.setComponent(MAPS);

        return new LaunchableActivity(intent, label, mIconResource);
    }

    private LaunchableActivity getGoogleMapsNavigation(final String query) {
        return getGoogleMaps(Uri.parse("google.navigation:q=" + Uri.encode(query) + "&avoid=tf"),
                "Navigation");
    }

    private LaunchableActivity getGoogleMapsNearby(final String query) {
        return getGoogleMaps(Uri.parse("geo:0,0?q=" + Uri.encode(query)), "Nearby");
    }

    private static final class DuckDuckGoLauncher extends LaunchableActivity {

        private final Object mLock = new Object();

        private Drawable mDrawable;

        private DuckDuckGoLauncher(@NonNull final Intent intent,
                @NonNull final String activityLabel, @DrawableRes final int iconResource) {
            super(intent, activityLabel, iconResource);
        }

        @Nullable
        @Override
        public Drawable getActivityIcon(final Context context, final int iconSizePixels) {
            synchronized (mLock) {
                if (mDrawable == null) {
                    final Resources resources = context.getResources();
                    mDrawable = resources.getDrawable(R.drawable.ic_duckduckgo_icon);
                }
            }
            return mDrawable;
        }

        @Override
        public ComponentName getComponent() {
            // This won't matter.
            return new ComponentName("com.hayaisoftware.launcher",
                    "com.hayaisoftware.launcher.SearchActivity");
        }
    }
}
