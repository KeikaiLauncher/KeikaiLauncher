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
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.hayaisoftware.launcher.BuildConfig;
import com.hayaisoftware.launcher.R;

/**
 * This class is a user interface to provide simple information about the current build.
 */
public class AboutActivity extends Activity {

    /**
     * This method returns a link to a Github user profile.
     *
     * @param userName The Github username for the profile. If null, this method will return the
     *                 {@code name} parameter with no url.
     * @param name     The actual name of the user for the Github profile.
     * @return A link to the Github user profile for the user detailed in the parameters.
     */
    private static CharSequence getGithubUserUrl(final CharSequence userName,
                                                 final CharSequence name) {
        final CharSequence results;

        if (userName == null) {
            results = name;
        } else {
            results = getUrl("https://www.github.com/" + userName, name);
        }

        return results;
    }

    /**
     * This method returns a {@link Html} link.
     *
     * @param url  The HTML Url for this link.
     * @param name The HTML Name for this link.
     * @return The entire HTML url as {@link Html}.
     */
    private static CharSequence getUrl(final CharSequence url, final CharSequence name) {
        final String sb = "<a href=\"" + url + "\">" + name + "</a>";

        return Html.fromHtml(sb);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        final Resources resources = getResources();
        final String[] contributorUserNames =
                resources.getStringArray(R.array.about_contributor_usernames);

        // Setup the project name/url
        final CharSequence projectUrl = getString(R.string.about_project_website_url);
        final CharSequence projectMaintainer = contributorUserNames[0];
        final TextView projectUrlView = ((TextView) findViewById(R.id.about_project_website_url));

        projectUrlView.setText(getUrl(projectUrl, "github.com/" + projectMaintainer));
        projectUrlView.setMovementMethod(LinkMovementMethod.getInstance());

        // Set the version
        final TextView version = ((TextView) findViewById(R.id.about_version));

        version.setText(BuildConfig.VERSION_NAME);

        // Setup the license name/url
        final String licenseUrl = getString(R.string.about_license_url);
        final String licenseName = getString(R.string.about_license_type);
        final TextView licenseType = ((TextView) findViewById(R.id.license_type));

        licenseType.setText(getUrl(licenseUrl, licenseName));
        licenseType.setMovementMethod(LinkMovementMethod.getInstance());

        // Setup the contributor names/urls.
        final String[] contributorNames =
                resources.getStringArray(R.array.about_contributor_names);
        final TextView contributorView = (TextView) findViewById(R.id.about_contributors);

        for (int i = 0; i < contributorNames.length; i++) {
            contributorView.append(getGithubUserUrl(contributorUserNames[i], contributorNames[i]));
            contributorView.append(System.getProperty("line.separator"));
        }

        contributorView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
