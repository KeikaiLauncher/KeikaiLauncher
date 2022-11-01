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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Insets;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DeprecatedSinceApi;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.anpmech.launcher.BuildConfig;
import com.anpmech.launcher.LaunchableActivity;
import com.anpmech.launcher.LaunchableActivityPrefs;
import com.anpmech.launcher.LaunchableAdapter;
import com.anpmech.launcher.R;
import com.anpmech.launcher.SharedLauncherPrefs;
import com.anpmech.launcher.monitor.PackageChangeCallback;
import com.anpmech.launcher.monitor.PackageChangedReceiver;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.ListIterator;

/**
 * This class is the main {@link Activity} for this launcher.
 */
public class SearchActivity extends Activity
        implements SharedPreferences.OnSharedPreferenceChangeListener, PackageChangeCallback {

    private static final String SEARCH_EDIT_TEXT_KEY = "SearchEditText";

    private static final String TAG = "SearchActivity";
    /**
     * Synchronize to this lock when the Adapter is visible and might be called by multiple
     * threads.
     */
    private final Object mLock = new Object();

    /**
     * This {@link BroadcastReceiver} implements an updater for package changes.
     */
    private final BroadcastReceiver mPackageChangeReceiver = new PackageChangedReceiver();

    /**
     * This implements a listener for orientation change, see {@link DisplayChangeListener} for
     * more information.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private DisplayManager.DisplayListener mDisplayListener = null;

    /**
     * This ContentObserver is used by the ContentResolver to register a callback to set rotation in case it changes
     * in the system settings.
     */
    private final ContentObserver mAccSettingObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            setRotation(new SharedLauncherPrefs(SearchActivity.this));
        }
    };

    /**
     * An adapter, based off {@link android.widget.ArrayAdapter}, to handle
     * {@link LaunchableActivity} items.
     */
    private LaunchableAdapter<LaunchableActivity> mAdapter;

    private EditText mSearchEditText;

    /**
     * This method returns the size of the dimen
     *
     * @param resources The resources for the containing the named identifier.
     * @param name      The name of the resource to get the id for.
     * @return The dimension size, {@code 0} if the name for the identifier doesn't exist.
     */
    private static int getDimensionSize(final Resources resources, final String name) {
        final int resourceId = resources.getIdentifier(name, "dimen", "android");
        final int dimensionSize;

        if (resourceId > 0) {
            dimensionSize = resources.getDimensionPixelSize(resourceId);
        } else {
            dimensionSize = 0;
        }

        return dimensionSize;
    }

    private static LaunchableActivity getLaunchableActivity(final View view) {
        return (LaunchableActivity) view.findViewById(R.id.appIcon).getTag();
    }

    private static LaunchableActivity getLaunchableActivity(final ContextMenuInfo menuInfo) {
        return getLaunchableActivity(((AdapterContextMenuInfo) menuInfo).targetView);
    }

    private static LaunchableActivity getLaunchableActivity(final MenuItem item) {
        return getLaunchableActivity(item.getMenuInfo());
    }

    private static Collection<ResolveInfo> getLaunchableResolveInfos(final PackageManager pm,
                                                                     @Nullable final String activityName) {
        final Intent intent = new Intent();

        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(activityName);

        return pm.queryIntentActivities(intent, 0);
    }

    /**
     * Retrieves the navigation bar height.
     *
     * @param resources The resources for the device.
     * @return The height of the navigation bar.
     */
    @DeprecatedSinceApi(api = Build.VERSION_CODES.R, message =
            "Later APIs use get getNavigationBarHeight30()")
    private static int getNavigationBarHeight15(final Resources resources) {
        final int navBarHeight;
        final Configuration configuration = resources.getConfiguration();

        //Only phone between 0-599 has navigationbar can move
        final boolean isSmartphone = configuration.smallestScreenWidthDp < 600;
        final boolean isPortrait =
                configuration.orientation == Configuration.ORIENTATION_PORTRAIT;

        if (isSmartphone && !isPortrait) {
            navBarHeight = 0;
        } else if (isPortrait) {
            navBarHeight = getDimensionSize(resources, "navigation_bar_height");
        } else {
            navBarHeight = getDimensionSize(resources, "navigation_bar_height_landscape");
        }

        return navBarHeight;
    }

    public static int getAppUsableScreenSizeWidth(final Display defaultDisplay) {
        Point size = new Point();
        defaultDisplay.getSize(size);

        return size.x;
    }

    public static int getRealScreenWidth(final Display defaultDisplay) {
        final Point size = new Point();

        if (Build.VERSION.SDK_INT >= 17) {
            defaultDisplay.getRealSize(size);
        } else if (Build.VERSION.SDK_INT >= 14) {
            try {
                size.x = (Integer) Display.class.getMethod("getRawWidth").invoke(defaultDisplay);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            } catch (NoSuchMethodException e) {
            }
        }

        return size.x;
    }

    /**
     * This is a workaround. Unfortunately, Android does not have a way in it's API to
     * accurately detect the position of the navigation bar when it's translucent.
     * This becomes highly problematic in landscape where we really don't want Launchables
     * underneath the navigation bar when in landscape mode. This forces the master layout outside
     * of the status bar and the navigation bar. Ideally, we would be forced outside of the
     * navigation bar alone and be able to travel underneath the status bar. The inconsistency of
     * this layout which affects landscape orientation and non-gesture mode is outweighed by the
     * really crappy hacks that used to try to work around this.
     *
     * @param context The current context.
     * @return True if the navigation bar is not in gesture mode, the device is in landscape, and
     * the application usable space is less than the real space.
     */
    public static boolean isNavBarProblematic(final Context context) {
        final Resources resources = context.getResources();
        final boolean isLandscape = resources.getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;

        return !isInGestureMode(resources) && isLandscape && isRealSizeDifferentThanUsable(context);
    }

    public static boolean isInGestureMode(final Resources resources) {
        final int resourceId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android");

        return resourceId != 0 && resources.getInteger(resourceId) == 2;
    }

    private static boolean isRealSizeDifferentThanUsable(final Context context) {
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        return getAppUsableScreenSizeWidth(display) < getRealScreenWidth(display);
    }

    /**
     * This method adds LauncherActivityInfo objects to an adapter in API 24+.
     *
     * @param adapter  The adapter to add to.
     * @param infoList The objects to add to the adapter.
     */
    @TargetApi(Build.VERSION_CODES.N)
    private void addToAdapter24(@NonNull final LaunchableAdapter<LaunchableActivity> adapter,
                                @NonNull final Iterable<LauncherActivityInfo> infoList) {
        final String thisCanonicalName = getClass().getCanonicalName();
        final UserManager manager = (UserManager) getSystemService(Context.USER_SERVICE);
        final boolean shouldLoadIcons = new SharedLauncherPrefs(this).areIconsEnabled();

        for (final LauncherActivityInfo info : infoList) {
            if (!thisCanonicalName.startsWith(info.getName())) {
                adapter.add(new LaunchableActivity(info, manager, shouldLoadIcons));
            }
        }
    }

    /**
     * This method adds ResolveInfo objects to an adapter in SDK 15-24, optionally using a
     * readCache.
     *
     * @param adapter      The adapter to add ResolveInfo object to.
     * @param infoList     The ResolveInfo object to add to the adapter.
     * @param useReadCache Whether to use a read cache.
     */
    @DeprecatedSinceApi(api = Build.VERSION_CODES.N, message =
            "Later APIs use addToAdapter(LaunchableActivity, Iterable<LauncherActivityInfo>)")
    private void addToAdapter15(@NonNull final LaunchableAdapter<LaunchableActivity> adapter,
                                @NonNull final Iterable<ResolveInfo> infoList,
                                final boolean useReadCache) {
        final SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        final String thisCanonicalName = getClass().getCanonicalName();
        final PackageManager manager;

        if (useReadCache) {
            manager = getPackageManager();
        } else {
            manager = null;
        }

        for (final ResolveInfo info : infoList) {
            if (!thisCanonicalName.startsWith(info.activityInfo.packageName)) {
                adapter.add(new LaunchableActivity(info, prefs, manager));
            }
        }
    }

    private void hideKeyboard() {
        final View focus = getCurrentFocus();

        if (focus != null) {
            final InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
        findViewById(R.id.appsContainer).requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    private boolean isCurrentLauncher() {
        final PackageManager pm = getPackageManager();
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo resolveInfo =
                pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null &&
                getPackageName().equals(resolveInfo.activityInfo.packageName);

    }

    private void launchActivity(final LaunchableActivity launchableActivity) {
        final LaunchableActivityPrefs launchableprefs = new LaunchableActivityPrefs(this);

        hideKeyboard();
        // Second conditional is always true, but this shuts up warnings.
        if (launchableActivity.isUserKnown() &&
                Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            final UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
            final LauncherApps launcher =
                    (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
            final long userSerial = launchableActivity.getUserSerial();
            final UserHandle userHandle = userManager.getUserForSerialNumber(userSerial);

            launcher.startMainActivity(launchableActivity.getComponent(), userHandle,
                    null, Bundle.EMPTY);
        } else {
            try {
                startActivity(launchableActivity.getLaunchIntent());
                mSearchEditText.setText(null);
                launchableActivity.setLaunchTime();
                launchableActivity.addUsage();
                launchableprefs.writePreference(launchableActivity);

                mAdapter.sortApps(this);
            } catch (final ActivityNotFoundException e) {
                if (BuildConfig.DEBUG) {
                    throw e;
                } else {
                    final String notFound = getString(R.string.activity_not_found);

                    Log.e(TAG, notFound, e);
                    Toast.makeText(this, notFound, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void launchActivity(final MenuItem item) {
        launchActivity(getLaunchableActivity(item));
    }

    private void launchActivity(final View view) {
        launchActivity(getLaunchableActivity(view));
    }

    public void launchApplicationDetails(final MenuItem item) {
        final LaunchableActivity activity = getLaunchableActivity(item);
        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + activity.getComponent().getPackageName()));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public void launchPlayStore(final MenuItem item) {
        final LaunchableActivity activity = getLaunchableActivity(item);
        final Intent intentPlayStore = new Intent(Intent.ACTION_VIEW);
        intentPlayStore.setData(Uri.parse("market://details?id=" +
                activity.getComponent().getPackageName()));
        startActivity(intentPlayStore);
    }

    /**
     * Returns a web search {@link LaunchableActivity}.
     *
     * @return A web search {@link LaunchableActivity}.
     */
    private LaunchableActivity getWebLaunchable() {
        final Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);

        return new LaunchableActivity(intent, getString(R.string.web_search), R.drawable.ic_baseline_search_24);
    }

    private LaunchableAdapter<LaunchableActivity> loadLaunchableAdapter() {
        final LaunchableAdapter<LaunchableActivity> adapter;
        final Object object = getLastNonConfigurationInstance();

        if (object == null) {
            final PackageManager pm = getPackageManager();
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                final UserManager manager = (UserManager) getSystemService(Context.USER_SERVICE);
                final LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
                final ListIterator<UserHandle> iter = manager.getUserProfiles().listIterator();
                int count = 1; // Add the web launchable to the count

                while (iter.hasNext()) {
                    count += launcherApps.getActivityList(null, iter.next()).size();
                }

                adapter = new LaunchableAdapter<>(getWebLaunchable(), this, R.layout.app_grid_item, count);

                while (iter.hasPrevious()) {
                    addToAdapter24(adapter, launcherApps.getActivityList(null, iter.previous()));
                }
            } else {
                final Collection<ResolveInfo> infoList = getLaunchableResolveInfos(pm, null);
                final int infoListSize = infoList.size();

                adapter = new LaunchableAdapter<>(getWebLaunchable(), this, R.layout.app_grid_item, infoListSize + 1);

                addToAdapter15(adapter, infoList, true);
            }
            final SharedLauncherPrefs prefs = new SharedLauncherPrefs(this);

            if (!prefs.isActionBarEnabled()) {
                final Intent intent = new Intent(this, SettingsActivity.class);
                adapter.add(new LaunchableActivity(intent, "Keikai Settings", R.drawable.ic_launcher));
            }
            adapter.sortApps(this);
            adapter.notifyDataSetChanged();
        } else {
            adapter = new LaunchableAdapter<>(getWebLaunchable(), object, this, R.layout.app_grid_item);
            adapter.setNotifyOnChange(true);
        }

        return adapter;
    }

    @Override
    public void onBackPressed() {
        if (isCurrentLauncher()) {
            hideKeyboard();
        } else {
            moveTaskToBack(false);
        }
    }

    public void onClickClearButton(final View view) {
        mSearchEditText.setText("");
    }

    public void onClickSettingsButton(final View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
                                    final ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app, menu);

        final LaunchableActivity activity = getLaunchableActivity(menuInfo);
        final MenuItem item = menu.findItem(R.id.appmenu_pin_to_top);

        menu.setHeaderTitle(activity.toString());

        if (activity.getPriority() == 0) {
            item.setTitle(R.string.appmenu_pin_to_top);
        } else {
            item.setTitle(R.string.appmenu_remove_pin);
        }
    }

    /**
     * This method is called when the user is already in this activity and presses the {@code home}
     * button. Use this opportunity to return this activity back to a default state.
     *
     * @param intent The incoming {@link Intent} sent by this activity
     */
    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);

        // If search has been typed, and home is hit, clear it.
        if (mSearchEditText.length() > 0) {
            mSearchEditText.setText(null);
        }

        closeContextMenu();
        closeOptionsMenu();

        // If the y coordinate is not at 0, let's reset it.
        final GridView view = findViewById(R.id.appsContainer);
        final int[] loc = {0, 0};
        view.getLocationInWindow(loc);
        if (loc[1] != 0) {
            view.smoothScrollToPosition(0);
        }
    }

    /**
     * Called when a package appears for any reason.
     *
     * @param activityName The name of the {@link Activity} of the package which appeared.
     */
    @Override
    public void onPackageAppeared(final String activityName, int[] uids) {
        final PackageManager pm = getPackageManager();

        synchronized (mLock) {
            if (mAdapter.getClassNamePosition(activityName) == -1) {

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    final LauncherApps launcherApps =
                            (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);

                    for (int uid : uids) {
                        addToAdapter24(mAdapter, launcherApps.getActivityList(activityName,
                                UserHandle.getUserHandleForUid(uid)));
                    }
                } else {
                    final Iterable<ResolveInfo> resolveInfos =
                            getLaunchableResolveInfos(pm, activityName);

                    addToAdapter15(mAdapter, resolveInfos, false);
                }
                mAdapter.sortApps(this);
                updateFilter(mSearchEditText.getText());
            }
        }
    }

    /**
     * Called when a package disappears for any reason.
     *
     * @param activityName The name of the {@link Activity} of the package which disappeared.
     */
    @Override
    public void onPackageDisappeared(final String activityName, int[] uids) {
        synchronized (mLock) {
            mAdapter.removeAllByName(activityName);
            updateFilter(mSearchEditText.getText());
        }
    }

    /**
     * Called when an existing package is updated or its disabled state changes.
     *
     * @param activityName The name of the {@link Activity} of the package which was modified.
     */
    @Override
    public void onPackageModified(final String activityName, int uid) {
        synchronized (mLock) {
            onPackageDisappeared(activityName, new int[]{uid});
            onPackageAppeared(activityName, new int[]{uid});
        }
    }

    @Override
    protected void onPause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            final DisplayManager manager =
                    (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
            manager.unregisterDisplayListener(mDisplayListener);
        }

        getContentResolver().unregisterContentObserver(mAccSettingObserver);
        super.onPause();
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        final CharSequence searchEditText =
                savedInstanceState.getCharSequence(SEARCH_EDIT_TEXT_KEY);

        if (searchEditText != null) {
            mSearchEditText.setText(searchEditText);
            mSearchEditText.setSelection(searchEditText.length());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        final SharedLauncherPrefs prefs = new SharedLauncherPrefs(this);

        mAdapter.updateUsageMap(this);
        final Editable searchText = mSearchEditText.getText();

        if ((prefs.isActionBarEnabled() && prefs.isKeyboardAutomatic()) ||
                searchText.length() > 0) {
            final InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

            // This is a special case to show SearchEditText should have focus.
            if (searchText.length() == 1 && searchText.charAt(0) == '\0') {
                mSearchEditText.setText(null);
            }

            mSearchEditText.requestFocus();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            imm.showSoftInput(mSearchEditText, 0);
        } else {
            hideKeyboard();
        }

        final Uri accUri = Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION);
        getContentResolver().registerContentObserver(accUri, false, mAccSettingObserver);
    }

    /**
     * This method checks whether rotation should be allowed and sets the launcher to
     * <p>
     * The current rules:
     * <p><ul>
     * <li> Rotate if allowed by both system and local settings.
     * <li> If rotation is not allowed by system settings disable rotation.
     * <li> If rotation is not allowed by local settings set orientation as portrait.
     * </ul><p>
     *
     * @param prefs The SharedLauncherPrefs object for this.
     */
    private void setRotation(final SharedLauncherPrefs prefs) {
        boolean systemRotationAllowed =
                Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1;

        if (systemRotationAllowed) {
            if (prefs.isRotationAllowed()) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    registerDisplayListener();
                }
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    /**
     * Retain the state of the adapter on configuration change.
     *
     * @return The attached {@link LaunchableAdapter}.
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        return mAdapter.export();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        final String searchEdit = mSearchEditText.getText().toString();

        if (!searchEdit.isEmpty()) {
            outState.putCharSequence(SEARCH_EDIT_TEXT_KEY, searchEdit);
        } else if (mSearchEditText.hasFocus()) {
            // This is a special case to show that the box had focus.
            outState.putCharSequence(SEARCH_EDIT_TEXT_KEY, '\0' + "");
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
                                          final String key) {
        //does this need to run in uiThread?
        if (getString(R.string.pref_key_preferred_order).equals(key)) {
            mAdapter.sortApps(this);
        } else if (getString(R.string.pref_key_disable_icons).equals(key)) {
            recreate();
        } else if (getString(R.string.pref_key_allow_rotation).equals(key)) {
            setRotation(new SharedLauncherPrefs(this));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        final SharedLauncherPrefs prefs = new SharedLauncherPrefs(this);

        // In a perfect world, this all could happen in onCreate(), but there are problems
        // with BroadcastReceiver registration and unregistration with that scenario.
        mSearchEditText = findViewById(R.id.user_search_input);
        mAdapter = loadLaunchableAdapter();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mDisplayListener = new DisplayChangeListener();
        }

        registerReceiver(mPackageChangeReceiver, PackageChangedReceiver.getFilter());
        PackageChangedReceiver.setCallback(this);

        setupPadding();
        setupPreferences();
        setupViews();
        setRotation(prefs);

        setupPadding();
    }

    @Override
    protected void onStop() {
        mAdapter.onStop();
        unregisterReceiver(mPackageChangeReceiver);

        super.onStop();
    }

    /**
     * This method clears the {@link LaunchableActivity} icons from memory on trim.
     *
     * @param level The level of memory trim requested.
     */
    @Override
    public void onTrimMemory(final int level) {
        super.onTrimMemory(level);
        if (level == TRIM_MEMORY_COMPLETE) {
            mAdapter.clearCaches();
        }

    }

    /**
     * This method prioritizes a {@link LaunchableActivity} to the top of the order when sorting.
     *
     * @param item The {@link MenuItem} containing the {@link LaunchableActivity} to be ordered.
     */
    public void pinToTop(final MenuItem item) {
        final LaunchableActivity activity = getLaunchableActivity(item);
        final LaunchableActivityPrefs prefs = new LaunchableActivityPrefs(this);

        if (activity.getPriority() == 0) {
            activity.setPriority(1);
        } else {
            activity.setPriority(0);
        }

        prefs.writePreference(activity);
        mAdapter.sortApps(this);
    }

    /**
     * This method registers a display listener for JB MR1 and higher to workaround a Android
     * deficiency with regard to 180 degree landscape rotation. See {@link DisplayChangeListener}
     * documentation for more information.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void registerDisplayListener() {
        final DisplayManager displayManager =
                (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        final Handler handler = new Handler(Looper.getMainLooper());

        displayManager.registerDisplayListener(mDisplayListener, handler);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private int getNavigationBarHeight30() {
        final int navBars = WindowInsets.Type.navigationBars();
        final Insets insets = getWindowManager().getCurrentWindowMetrics()
                .getWindowInsets().getInsets(navBars);

        return insets.bottom;
    }

    private static void setupMasterLayoutPadding(final View masterLayout, final int padding) {
        masterLayout.setFitsSystemWindows(isNavBarProblematic(masterLayout.getContext()));
        final FrameLayout.LayoutParams masterParams =
                (FrameLayout.LayoutParams) masterLayout.getLayoutParams();

        masterParams.setMargins(padding, 0, padding, 0);
    }

    private static int setupActionBarLayout(final View customActionBar, final int padding) {
        final Context context = customActionBar.getContext();
        final FrameLayout.LayoutParams searchParams =
                (FrameLayout.LayoutParams) customActionBar.getLayoutParams();
        final int searchTop;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !isNavBarProblematic(context)) {
            searchTop = getDimensionSize(context.getResources(), "status_bar_height") +
                    padding;
        } else {
            searchTop = padding;
        }

        searchParams.setMargins(0, searchTop, 0, 0);

        return searchTop + context.getResources().getDimensionPixelSize(R.dimen.app_icon_size) + padding;
    }

    private void setupPadding() {
        final View appContainer = findViewById(R.id.appsContainer);
        final int appContainerTop, appContainerBottom;
        final int dp16 = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        final boolean isNavBarProblematic = isNavBarProblematic(this);
        setupMasterLayoutPadding(findViewById(R.id.masterLayout), dp16);

        if (new SharedLauncherPrefs(this).isActionBarEnabled()) {
            appContainerTop = setupActionBarLayout(findViewById(R.id.customActionBar), dp16);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !isNavBarProblematic) {
                appContainerTop = getDimensionSize(getResources(), "status_bar_height") +
                        dp16;
            } else {
                appContainerTop = dp16;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            appContainerBottom = getNavigationBarHeight30() + dp16;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            appContainerBottom = getNavigationBarHeight15(getResources()) + dp16;
        } else {
            appContainerBottom = dp16;
        }

        appContainer.setPadding(0, appContainerTop, 0, appContainerBottom);
    }

    private void setupPreferences() {
        final SharedLauncherPrefs prefs = new SharedLauncherPrefs(this);
        final SharedPreferences preferences = prefs.getPreferences();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    private EditText setupSearchEditText() {
        final SearchEditTextListeners listeners = new SearchEditTextListeners();
        final EditText searchEditText = findViewById(R.id.user_search_input);

        searchEditText.addTextChangedListener(listeners);
        searchEditText.setOnEditorActionListener(listeners);

        return searchEditText;
    }

    private void setupActionBar() {
        final View view = findViewById(R.id.customActionBar);
        final SharedLauncherPrefs prefs = new SharedLauncherPrefs(this);

        if (prefs.isActionBarEnabled()) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private void setupViews() {
        final GridView appContainer = findViewById(R.id.appsContainer);
        final AppContainerListener listener = new AppContainerListener();
        mSearchEditText = setupSearchEditText();

        registerForContextMenu(appContainer);

        appContainer.setOnScrollListener(listener);
        appContainer.setAdapter(mAdapter);
        appContainer.setOnItemClickListener(listener);
        setupActionBar();
    }

    private void updateFilter(final CharSequence cs) {
        final int seqLength = cs.length();

        if (seqLength != 1 || cs.charAt(0) != '\0') {
            mAdapter.getFilter().filter(cs);
        }
    }

    private final class AppContainerListener implements AbsListView.OnScrollListener,
            OnItemClickListener {

        @Override
        public void onItemClick(final AdapterView<?> parent, final View view,
                                final int position, final long id) {
            launchActivity(view);
        }

        @Override
        public void onScroll(final AbsListView view, final int firstVisibleItem,
                             final int visibleItemCount, final int totalItemCount) {
        }

        @Override
        public void onScrollStateChanged(final AbsListView view, final int scrollState) {
            if (scrollState != SCROLL_STATE_IDLE) {
                hideKeyboard();
            }
        }
    }

    /**
     * This class is a workaround for cases where {@link Activity} does not call any lifecycle
     * methods after 180 degree landscape orientation change.
     * <p>
     * In this case, OrientationEventListener would not be suitable due to magnitude restrictions
     * in the SensorEventListener implementation.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private final class DisplayChangeListener implements DisplayManager.DisplayListener {

        @Override
        public void onDisplayAdded(final int displayId) {
        }

        @Override
        public void onDisplayChanged(final int displayId) {
            setupPadding();
        }

        @Override
        public void onDisplayRemoved(final int displayId) {
        }
    }

    private final class SearchEditTextListeners
            implements TextView.OnEditorActionListener, TextWatcher {

        @Override
        public void afterTextChanged(final Editable s) {
            //do nothing
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count,
                                      final int after) {
            //do nothing
        }

        @Override
        public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
            final boolean actionConsumed;
            final boolean enterPressed = event != null &&
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                    event.getKeyCode() == KeyEvent.KEYCODE_ENTER;

            if (actionId == EditorInfo.IME_ACTION_GO || (enterPressed && !mAdapter.isEmpty())) {
                launchActivity(mAdapter.getItem(0));
                actionConsumed = true;
            } else {
                actionConsumed = false;
            }

            return actionConsumed;
        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before,
                                  final int count) {
            updateFilter(s);
            final View clearButton = findViewById(R.id.clear_button);

            if (s.length() > 0) {
                clearButton.setVisibility(View.VISIBLE);
            } else {
                clearButton.setVisibility(View.GONE);
            }
        }
    }
}
