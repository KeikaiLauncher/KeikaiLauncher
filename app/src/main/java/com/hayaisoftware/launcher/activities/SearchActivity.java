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

package com.hayaisoftware.launcher.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.hayaisoftware.launcher.BuildConfig;
import com.hayaisoftware.launcher.LaunchableActivity;
import com.hayaisoftware.launcher.LaunchableActivityPrefs;
import com.hayaisoftware.launcher.LaunchableAdapter;
import com.hayaisoftware.launcher.LoadLaunchableActivityTask;
import com.hayaisoftware.launcher.R;
import com.hayaisoftware.launcher.ShortcutNotificationManager;
import com.hayaisoftware.launcher.fragments.SettingsFragment;
import com.hayaisoftware.launcher.monitor.PackageChangeCallback;
import com.hayaisoftware.launcher.monitor.PackageChangedReceiver;
import com.hayaisoftware.launcher.threading.SimpleTaskConsumerManager;

import java.util.Collection;

public class SearchActivity extends Activity
        implements SharedPreferences.OnSharedPreferenceChangeListener, PackageChangeCallback {

    private static final String KEY_PREF_DISABLE_ICONS = "pref_disable_icons";

    private static final String KEY_PREF_PREFERRED_ORDER = "pref_app_preferred_order";

    private static final String KEY_PREF_PREFERRED_ORDER_RECENT = "recent";

    private static final String KEY_PREF_PREFERRED_ORDER_USAGE = "usage";

    private static final String SEARCH_EDIT_TEXT_KEY = "SearchEditText";

    private static final String TAG = "SearchActivity";

    /**
     * Synchronize to this lock when the Adapter is visible and might be called by multiple
     * threads.
     */
    private final Object mLock = new Object();

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

    public static Collection<ResolveInfo> getLaunchableResolveInfos(final PackageManager pm,
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
    private static int getNavigationBarHeight(final Resources resources) {
        final int navBarHeight;

        if (hasNavBar(resources)) {
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
        } else {
            navBarHeight = 0;
        }

        return navBarHeight;
    }

    /**
     * Get the navigation bar width.
     *
     * @param resources The resources for the device.
     * @return The width of the navigation bar.
     */
    private static int getNavigationBarWidth(final Resources resources) {
        final int navBarWidth;

        if (hasNavBar(resources)) {
            final Configuration configuration = resources.getConfiguration();

            //Only phone between 0-599 has navigationbar can move
            final boolean isSmartphone = configuration.smallestScreenWidthDp < 600;

            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && isSmartphone) {
                navBarWidth = getDimensionSize(resources, "navigation_bar_width");
            } else {
                navBarWidth = 0;
            }
        } else {
            navBarWidth = 0;
        }

        return navBarWidth;
    }

    /**
     * Retrieves the visibility status of the navigation bar.
     *
     * @param resources The resources for the device.
     * @return {@code True} if the navigation bar is enabled, {@code false} otherwise.
     */
    private static boolean hasNavBar(final Resources resources) {
        final boolean hasNavBar;
        final int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");

        if (id > 0) {
            hasNavBar = resources.getBoolean(id);
        } else {
            hasNavBar = false;
        }

        return hasNavBar;
    }

    private void addToAdapter(@NonNull final Iterable<ResolveInfo> infoList) {
        final PackageManager pm = getPackageManager();
        final String thisCanonicalName = getClass().getCanonicalName();

        for (final ResolveInfo info : infoList) {
            // Don't include activities from this package.
            if (!thisCanonicalName.startsWith(info.activityInfo.packageName)) {
                final LaunchableActivity launchableActivity =
                        LaunchableActivity.getLaunchable(info.activityInfo, pm);

                mAdapter.add(launchableActivity);
            }
        }
    }

    private void disableReceiver() {
        final ComponentName name = new ComponentName(this, PackageChangedReceiver.class);

        getPackageManager().setComponentEnabledSetting(name,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void enableReceiver() {
        final PackageManager pm = getPackageManager();
        final ComponentName componentName = new ComponentName(this, PackageChangedReceiver.class);

        pm.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
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

    public void launchActivity(final LaunchableActivity launchableActivity) {
        final LaunchableActivityPrefs prefs = new LaunchableActivityPrefs(this);

        hideKeyboard();
        try {
            startActivity(launchableActivity.getLaunchIntent());
            mSearchEditText.setText(null);
            launchableActivity.setLaunchTime();
            launchableActivity.addUsage();
            prefs.writePreference(launchableActivity);

            if (mAdapter.isOrderedByRecent()) {
                mAdapter.sort(LaunchableAdapter.RECENT);
            } else if (mAdapter.isOrderedByUsage()) {
                mAdapter.sort(LaunchableAdapter.USAGE);
            }
        } catch (final ActivityNotFoundException e) {
            if (BuildConfig.DEBUG) {
                throw e;
            } else {
                final String notFound = getString(R.string.activity_not_found);

                Log.e(TAG, notFound, e);
                Toast.makeText(this, getString(R.string.activity_not_found),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private LaunchableAdapter<LaunchableActivity> loadLaunchableAdapter() {
        final LaunchableAdapter<LaunchableActivity> adapter;
        final Object object = getLastNonConfigurationInstance();

        if (object == null) {
            adapter = loadLaunchableApps();
        } else {
            adapter = new LaunchableAdapter<>(object, this, R.layout.app_grid_item);
            adapter.setNotifyOnChange(true);
        }

        return adapter;
    }

    private LaunchableAdapter<LaunchableActivity> loadLaunchableApps() {
        final PackageManager pm = getPackageManager();
        final Collection<ResolveInfo> infoList = getLaunchableResolveInfos(pm, null);
        final int infoListSize = infoList.size();
        final LaunchableAdapter<LaunchableActivity> adapter
                = new LaunchableAdapter<>(this, R.layout.app_grid_item, infoListSize);
        final int cores = Runtime.getRuntime().availableProcessors();

        if (cores <= 1) {
            addToAdapter(infoList);
        } else {
            final String thisCanonicalName = getClass().getCanonicalName();
            final SimpleTaskConsumerManager simpleTaskConsumerManager =
                    new SimpleTaskConsumerManager(cores, infoListSize);
            final LoadLaunchableActivityTask.Factory launchableTask =
                    new LoadLaunchableActivityTask.Factory(pm, adapter);

            for (final ResolveInfo info : infoList) {
                // Don't include activities from this package.
                if (!thisCanonicalName.startsWith(info.activityInfo.packageName)) {
                    simpleTaskConsumerManager.addTask(launchableTask.create(info));
                }
            }

            simpleTaskConsumerManager.destroyAllConsumers(true, true);
        }

        adapter.sortApps();
        adapter.notifyDataSetChanged();

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
        showPopup(findViewById(R.id.overflow_button_topleft));
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final View itemView = info.targetView;
        final LaunchableActivity launchableActivity =
                (LaunchableActivity) itemView.findViewById(R.id.appIcon).getTag();
        boolean consumed = true;

        switch (item.getItemId()) {
            case R.id.appmenu_launch:
                launchActivity(launchableActivity);
                break;
            case R.id.appmenu_info:
                final Intent intent = new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:"
                        + launchableActivity.getComponent().getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            case R.id.appmenu_onplaystore:
                final Intent intentPlayStore = new Intent(Intent.ACTION_VIEW);
                intentPlayStore.setData(Uri.parse("market://details?id=" +
                        launchableActivity.getComponent().getPackageName()));
                startActivity(intentPlayStore);
                break;
            case R.id.appmenu_pin_to_top:
                final LaunchableActivityPrefs prefs = new LaunchableActivityPrefs(this);
                launchableActivity.setPriority(launchableActivity.getPriority() == 0 ? 1 : 0);
                prefs.writePreference(launchableActivity);
                mAdapter.sortApps();
                break;
            default:
                consumed = super.onContextItemSelected(item);
                break;
        }

        return consumed;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search);

        //fields:
        mSearchEditText = findViewById(R.id.user_search_input);
        mAdapter = loadLaunchableAdapter();

        final boolean noMultiWindow = Build.VERSION.SDK_INT < Build.VERSION_CODES.N ||
                !isInMultiWindowMode();
        final boolean transparentPossible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        setupPadding(transparentPossible && noMultiWindow);

        PackageChangedReceiver.setCallback(this);
        enableReceiver();

        setupPreferences();
        setupViews();
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app, menu);

        if (menuInfo instanceof AdapterContextMenuInfo) {
            final AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) menuInfo;
            final LaunchableActivity activity = (LaunchableActivity) adapterMenuInfo.targetView
                    .findViewById(R.id.appIcon).getTag();
            final MenuItem item = menu.findItem(R.id.appmenu_pin_to_top);

            menu.setHeaderTitle(activity.toString());

            if (activity.getPriority() == 0) {
                item.setTitle(R.string.appmenu_pin_to_top);
            } else {
                item.setTitle(R.string.appmenu_remove_pin);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (!isChangingConfigurations()) {
            Log.d("HayaiLauncher", "Hayai is ded");
        }
        disableReceiver();
        mAdapter.onDestroy();
        super.onDestroy();
    }

    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        final boolean consumed;

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showPopup(findViewById(R.id.overflow_button_topleft));
            consumed = true;
        } else {
            consumed = super.onKeyUp(keyCode, event);
        }

        return consumed;
    }

    @Override
    public void onMultiWindowModeChanged(final boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);

        setupPadding(!isInMultiWindowMode);
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

        // If the y coordinate is not at 0, let's reset it.
        final GridView view = findViewById(R.id.appsContainer);
        final int[] loc = {0, 0};
        view.getLocationInWindow(loc);
        if (loc[1] != 0) {
            view.smoothScrollToPosition(0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        boolean consumed = true;

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                final Intent intentSettings = new Intent(this, SettingsActivity.class);
                startActivity(intentSettings);
                break;
            case R.id.action_refresh_app_list:
                recreate();
                break;
            case R.id.action_system_settings:
                final Intent intentSystemSettings = new Intent(Settings.ACTION_SETTINGS);
                intentSystemSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentSystemSettings);
                break;
            case R.id.action_manage_apps:
                final Intent intentManageApps = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                intentManageApps.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentManageApps);
                break;
            case R.id.action_set_wallpaper:
                final Intent intentWallpaperPicker = new Intent(Intent.ACTION_SET_WALLPAPER);
                startActivity(intentWallpaperPicker);
                break;
            case R.id.action_about:
                final Intent intentAbout = new Intent(this, AboutActivity.class);
                startActivity(intentAbout);
                break;
            default:
                consumed = super.onOptionsItemSelected(item);
                break;
        }

        return consumed;
    }

    /**
     * Called when a package appears for any reason.
     *
     * @param activityName The name of the {@link Activity} of the package which appeared.
     */
    @Override
    public void onPackageAppeared(final String activityName) {
        final PackageManager pm = getPackageManager();
        final Iterable<ResolveInfo> resolveInfos = getLaunchableResolveInfos(pm, activityName);

        synchronized (mLock) {
            if (mAdapter.getClassNamePosition(activityName) == -1) {
                addToAdapter(resolveInfos);
                mAdapter.sortApps();
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
    public void onPackageDisappeared(final String activityName) {
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
    public void onPackageModified(final String activityName) {
        synchronized (mLock) {
            onPackageDisappeared(activityName);
            onPackageAppeared(activityName);
        }
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
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final Editable searchText = mSearchEditText.getText();

        if (preferences.getBoolean(SettingsFragment.KEY_PREF_AUTO_KEYBOARD, false) ||
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

        if (preferences.getBoolean(SettingsFragment.KEY_PREF_ALLOW_ROTATION, false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
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
        if (key.equals(KEY_PREF_PREFERRED_ORDER)) {
            setPreferredOrder(sharedPreferences);
            mAdapter.sortApps();
        } else if (key.equals(KEY_PREF_DISABLE_ICONS)) {
            recreate();
        }
    }

    @Override
    public void onTrimMemory(final int level) {
        super.onTrimMemory(level);
        if (level == TRIM_MEMORY_COMPLETE) {
            mAdapter.clearCaches();
        }

    }

    private void setPreferredOrder(final SharedPreferences preferences) {
        final String order = preferences.getString(KEY_PREF_PREFERRED_ORDER,
                KEY_PREF_PREFERRED_ORDER_RECENT);

        if (KEY_PREF_PREFERRED_ORDER_RECENT.equals(order)) {
            mAdapter.enableOrderByRecent();
        } else {
            mAdapter.disableOrderByRecent();
        }

        if (KEY_PREF_PREFERRED_ORDER_USAGE.equals(order)) {
            mAdapter.enableOrderByUsage();
        } else {
            mAdapter.disableOrderByUsage();
        }
    }

    /**
     * This method dynamically sets the padding for the outer boundaries of the masterLayout and
     * appContainer.
     *
     * @param isNavBarTranslucent Set this to {@code true} if android.R.windowTranslucentNavigation
     *                            is expected to be {@code true}, {@code false} otherwise.
     */
    private void setupPadding(final boolean isNavBarTranslucent) {
        final Resources resources = getResources();
        final View masterLayout = findViewById(R.id.masterLayout);
        final View appContainer = findViewById(R.id.appsContainer);
        final int appTop = resources.getDimensionPixelSize(R.dimen.activity_vertical_margin);

        if (isNavBarTranslucent) {
            masterLayout.setFitsSystemWindows(false);
            final int navBarWidth = getNavigationBarWidth(resources);
            final int searchUpperPadding = getDimensionSize(resources, "status_bar_height");
            final int navBarHeight = getNavigationBarHeight(resources);

            // If the navigation bar is on the side, don't put apps under it.
            masterLayout.setPadding(0, searchUpperPadding, navBarWidth, 0);

            // If the navigation bar is at the bottom, stop the icons above it.
            appContainer.setPadding(0, appTop, 0, navBarHeight);
        } else {
            masterLayout.setFitsSystemWindows(true);
            appContainer.setPadding(0, appTop, 0, 0);
        }
    }

    private void setupPreferences() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        if (preferences.getBoolean(SettingsFragment.KEY_PREF_NOTIFICATION, false)) {
            final String strPriority =
                    preferences.getString(SettingsFragment.KEY_PREF_NOTIFICATION_PRIORITY,
                            SettingsFragment.KEY_PREF_NOTIFICATION_PRIORITY_LOW);

            ShortcutNotificationManager.showNotification(this, strPriority);
        }

        if (preferences.getBoolean(KEY_PREF_DISABLE_ICONS, false)) {
            mAdapter.setIconsDisabled();
        } else {
            mAdapter.setIconsEnabled();
        }

        setPreferredOrder(preferences);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    private EditText setupSearchEditText() {
        final SearchEditTextListeners listeners = new SearchEditTextListeners();
        final EditText searchEditText = findViewById(R.id.user_search_input);

        searchEditText.addTextChangedListener(listeners);
        searchEditText.setOnEditorActionListener(listeners);
        searchEditText.setOnKeyListener(listeners);

        return searchEditText;
    }

    private void setupViews() {
        final GridView appContainer = findViewById(R.id.appsContainer);
        mSearchEditText = setupSearchEditText();

        registerForContextMenu(appContainer);

        appContainer.setOnScrollListener(new AbsListView.OnScrollListener() {
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
        });
        appContainer.setAdapter(mAdapter);

        appContainer.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(final AdapterView<?> parent, final View view,
                    final int position, final long id) {
                launchActivity(mAdapter.getItem(position));
            }
        });
    }

    public void showPopup(final View v) {
        final PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(new PopupEventListener());
        final MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.search_activity_menu, popup.getMenu());
        popup.show();
    }

    private void updateFilter(final CharSequence cs) {
        final int seqLength = cs.length();

        if (seqLength != 1 || cs.charAt(0) != '\0') {
            mAdapter.getFilter().filter(cs);
        }
    }

    class PopupEventListener implements PopupMenu.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(final MenuItem item) {
            return onOptionsItemSelected(item);
        }
    }

    private final class SearchEditTextListeners
            implements TextView.OnEditorActionListener, TextWatcher, View.OnKeyListener {

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

            if (actionId == EditorInfo.IME_ACTION_GO) {
                Log.d("KEYBOARD", "ACTION_GO");
                actionConsumed = openFirstActivity();
            } else {
                actionConsumed = false;
            }

            return actionConsumed;
        }

        @Override
        public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
            final boolean actionConsumed;

            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                actionConsumed = openFirstActivity();
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

        private boolean openFirstActivity() {
            final boolean actionConsumed;

            if (mAdapter.isEmpty()) {
                actionConsumed = false;
            } else {
                launchActivity(mAdapter.getItem(0));
                actionConsumed = true;
            }

            return actionConsumed;
        }
    }
}
