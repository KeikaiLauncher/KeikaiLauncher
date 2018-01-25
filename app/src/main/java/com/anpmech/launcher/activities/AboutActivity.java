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

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.anpmech.launcher.BuildConfig;
import com.anpmech.launcher.R;

/**
 * This class is a user interface to provide simple information about the current build.
 */
public class AboutActivity extends Activity {

    /**
     * This method returns a {@link Html} link.
     *
     * @param url  The HTML Url for this link.
     * @param name The HTML Name for this link.
     * @return The entire HTML url as {@link Html}.
     */
    private static CharSequence getUrl(final CharSequence url, final CharSequence name) {
        final CharSequence result;
        final String sb = "<a href=\"" + url + "\">" + name + "</a>";

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            result = Html.fromHtml(sb, Html.FROM_HTML_MODE_LEGACY);
        } else {
            //noinspection deprecation
            result = Html.fromHtml(sb);
        }

        return result;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Setup the project name/url
        final CharSequence githubUrl = "github.com/";
        final CharSequence projectUrl = "https://www." + githubUrl + BuildConfig.GITHUB_USER +
                '/' + BuildConfig.GITHUB_PROJECT;
        final TextView projectUrlView = findViewById(R.id.about_project_website_url);

        projectUrlView.setText(getUrl(projectUrl, githubUrl + BuildConfig.GITHUB_USER));
        projectUrlView.setMovementMethod(LinkMovementMethod.getInstance());

        // Set the version
        final TextView version = findViewById(R.id.about_version);
        version.setText(BuildConfig.VERSION_NAME);

        // Setup the license name/url
        final CharSequence licenseUrl = getString(R.string.about_license_url);
        final CharSequence licenseName = getString(R.string.about_license_type);
        final TextView licenseType = findViewById(R.id.license_type);

        licenseType.setText(getUrl(licenseUrl, licenseName));
        licenseType.setMovementMethod(LinkMovementMethod.getInstance());

        final TextView contributorView = findViewById(R.id.list_of_contributors);
        final CharSequence listOfContributors = getString(R.string.about_list_of_contributors);

        contributorView.setText(getUrl(projectUrl + "/contributors", listOfContributors));
        contributorView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
