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

package com.anpmech.launcher.activities;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.ListView;

import com.anpmech.launcher.fragments.SettingsFragment;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ListView listView = new ListView(this);
        listView.setId(android.R.id.list);
        setContentView(listView);

        final Fragment fragment = new SettingsFragment();
        final FragmentTransaction ft = getFragmentManager().beginTransaction();

        ft.replace(android.R.id.content, fragment).commit();
    }

    @Override
    protected void onPause() {
        finish();
        super.onPause();
    }
}
