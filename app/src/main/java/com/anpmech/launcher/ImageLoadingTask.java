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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.widget.ImageView;

import com.anpmech.launcher.threading.SimpleTaskConsumerManager;


public final class ImageLoadingTask implements Runnable, SimpleTaskConsumerManager.Task {

    private final Context mContext;

    private final int mIconSizePixels;

    private final ImageView mImageView;

    private final LaunchableActivity mLaunchableActivity;

    private Drawable mActivityIcon;

    private ImageLoadingTask(final ImageView imageView, final LaunchableActivity launchableActivity,
            final int iconSizePixels) {
        mContext = imageView.getContext();
        mImageView = imageView;
        mLaunchableActivity = launchableActivity;
        mIconSizePixels = iconSizePixels;
    }

    @Override
    public boolean doTask() {
        mActivityIcon = mLaunchableActivity.getActivityIcon(mContext, mIconSizePixels);
        final Handler handler = new Handler(mContext.getMainLooper());

        handler.post(this);

        return true;
    }

    @Override
    public void run() {
        if (mImageView.getTag() == mLaunchableActivity) {
            mImageView.setImageDrawable(mActivityIcon);
        }
    }

    public static class Factory {

        private final int mIconSizePixels;

        public Factory(final int iconSizePixels) {
            mIconSizePixels = iconSizePixels;
        }

        public SimpleTaskConsumerManager.Task create(final ImageView imageView,
                final LaunchableActivity activity) {
            return new ImageLoadingTask(imageView, activity, mIconSizePixels);
        }
    }

}
