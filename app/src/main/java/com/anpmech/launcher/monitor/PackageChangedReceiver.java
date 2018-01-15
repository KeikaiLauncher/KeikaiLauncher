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

package com.anpmech.launcher.monitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * This {@link BroadcastReceiver} receives messages from the base system about package changes
 * and stores it in SharedPreferences to be acted upon later.
 *
 * This class is not thread-safe and is expected to only be called by one thread at a time, in
 * sequence per the Android documentation.
 */
public class PackageChangedReceiver extends BroadcastReceiver {

    /**
     * This is an action to be used when there is no callback.
     */
    private static final String ACTION_DELAYED = "ActionDelayed";

    /**
     * This is storage for Intents that arrive when {@link #sCallback} is null.
     */
    private static final List<Intent> DELAYED_INTENTS = new ArrayList<>(2);

    /**
     * An action was received noting that a package appeared on the system.
     */
    private static final int PACKAGE_APPEARED = 0;

    /**
     * An action was received noting that a package on the system changed.
     */
    private static final int PACKAGE_CHANGED = 1;

    /**
     * An action was received noting that a package on the system was removed.
     */
    private static final int PACKAGE_DISAPPEARED = 2;

    /**
     * The class log identifier.
     */
    private static final String TAG = "PackageChangedReceiver";

    /**
     * The storage for the callback implementation, called upon receipt.
     */
    private static PackageChangeCallback sCallback;

    private static void actOnIntent(final Intent intent) {
        final String action = intent.getAction();

        switch (intent.getAction()) {
            case Intent.ACTION_PACKAGE_ADDED:
                sendPackageName(PACKAGE_APPEARED, intent.getData().getSchemeSpecificPart());
                break;
            case Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE:
            case Intent.ACTION_PACKAGES_UNSUSPENDED:
                sendPackageName(PACKAGE_APPEARED,
                        intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST));
                break;
            case Intent.ACTION_PACKAGE_REMOVED:
                sendPackageName(PACKAGE_DISAPPEARED, intent.getData().getSchemeSpecificPart());
                break;
            case Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE:
            case Intent.ACTION_PACKAGES_SUSPENDED:
                sendPackageName(PACKAGE_DISAPPEARED,
                        intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST));
                break;
            case Intent.ACTION_PACKAGE_CHANGED:
            case Intent.ACTION_PACKAGE_REPLACED:
                sendPackageName(PACKAGE_CHANGED, intent.getData().getSchemeSpecificPart());
                break;
            case ACTION_DELAYED:
                // This will happen if a second intent processes the delayed intent first.
                break;
            default:
                Log.w(TAG, "Received action without reaction: " + action);
                break;
        }
    }

    public static IntentFilter getFilter() {
        final IntentFilter filter = new IntentFilter();

        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");

        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            filter.addAction(Intent.ACTION_PACKAGES_SUSPENDED);
            filter.addAction(Intent.ACTION_PACKAGES_UNSUSPENDED);
        }

        return filter;
    }

    /**
     * This method is used to implement the callbacks.
     *
     * @param action   The action which took place.
     * @param packages The package which appeared, changed or disappeared.
     */
    private static void sendPackageName(final int action, final String... packages) {
        if (packages != null) {
            for (String newPackage : packages) {
                if ((int) newPackage.charAt(newPackage.length() - 1) == (int) ' ') {
                    newPackage = newPackage.trim();
                }

                switch (action) {
                    case PACKAGE_APPEARED:
                        Log.d(TAG, "Package appeared: " + newPackage);
                        sCallback.onPackageAppeared(newPackage);
                        break;
                    case PACKAGE_CHANGED:
                        Log.d(TAG, "Package changed: " + newPackage);
                        sCallback.onPackageModified(newPackage);
                        break;
                    case PACKAGE_DISAPPEARED:
                        Log.d(TAG, "Package disappeared: " + newPackage);
                        sCallback.onPackageDisappeared(newPackage);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Set a {@link PackageChangeCallback} to call after a change has been received.
     *
     * @param callback The implementation to callback.
     */
    public static void setCallback(final PackageChangeCallback callback) {
        sCallback = callback;
    }

    /**
     * This method will be called by the system when a package addition, modification or removal
     * has been performed.
     *
     * Intents received when there is no callback will be stored in {@link #DELAYED_INTENTS}.
     * The window that Intents are delayed should be minimal.
     *
     * @param context The current context.
     * @param intent  The Intent noting the changes, or a blank intent signifing .
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String intentAction = intent.getAction();

        if (intentAction == null) {
            Log.w(TAG, "Received a Intent with no action.");
        } else if (sCallback == null) {
            Log.d(TAG, "Callback was null, delaying intent: " + intent);
            if (!ACTION_DELAYED.equals(intentAction)) {
                DELAYED_INTENTS.add(intent);
            }

            final Intent restartServiceIntent
                    = new Intent(ACTION_DELAYED, null, context, PackageChangedReceiver.class);
            final PendingIntent restartService = PendingIntent
                    .getBroadcast(context, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
            final AlarmManager alarmService =
                    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            final long halfSecond = 500L;

            alarmService.set(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + halfSecond, restartService);
        } else {
            if (DELAYED_INTENTS.isEmpty()) {
                actOnIntent(intent);
            } else {
                if (!ACTION_DELAYED.equals(intentAction)) {
                    DELAYED_INTENTS.add(intent);
                }

                final ListIterator<Intent> iterator = DELAYED_INTENTS.listIterator();

                while (iterator.hasNext()) {
                    final Intent processingIntent = iterator.next();
                    actOnIntent(processingIntent);
                    iterator.remove();
                }
            }
        }
    }
}
