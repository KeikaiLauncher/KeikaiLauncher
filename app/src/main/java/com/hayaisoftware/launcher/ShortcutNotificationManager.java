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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.hayaisoftware.launcher.activities.SearchActivity;
import com.hayaisoftware.launcher.fragments.SettingsFragment;

public final class ShortcutNotificationManager {

    private static final int NOTIFICATION_ID = 0;

    private ShortcutNotificationManager() {
    }

    public static void cancelNotification(final Context context) {

        final NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    public static void showNotification(final Context context, final String priority) {
        final Intent resultIntent = new Intent(context, SearchActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        final Notification notification;
        final Notification.Builder builder;
        final NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final int importance;
            final NotificationChannel channel;

            if (SettingsFragment.KEY_PREF_NOTIFICATION_PRIORITY_LOW.equals(priority)) {
                importance = NotificationManager.IMPORTANCE_LOW;
            } else if (SettingsFragment.KEY_PREF_NOTIFICATION_PRIORITY_HIGH.equals(priority)) {
                importance = NotificationManager.IMPORTANCE_HIGH;
            } else {
                throw new AssertionError("Undefined notification priority.");
            }

            channel = new NotificationChannel(BuildConfig.APPLICATION_ID, "Hayai Launcher",
                    importance);
            notificationManager.createNotificationChannel(channel);
            builder = new Notification.Builder(context, BuildConfig.APPLICATION_ID);
        } else {
            //noinspection deprecation
            builder = new Notification.Builder(context);
        }

        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.title_activity_search))
                .setOngoing(true)
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (SettingsFragment.KEY_PREF_NOTIFICATION_PRIORITY_LOW.equals(priority)) {
                //noinspection deprecation
                builder.setPriority(Notification.PRIORITY_LOW);
            } else if (SettingsFragment.KEY_PREF_NOTIFICATION_PRIORITY_HIGH.equals(priority)) {
                //noinspection deprecation
                builder.setPriority(Notification.PRIORITY_HIGH);
            } else {
                throw new AssertionError("Undefined notification priority.");
            }
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            notification = builder.build();
        } else {
            //noinspection deprecation
            notification = builder.getNotification();
        }

        notificationManager.notify(NOTIFICATION_ID, notification);
    }
}
