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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This is a convenience class write persistent information to save to restore
 * {@link LaunchableActivity} objects.
 */
public class LaunchableActivityPrefs extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 3;

    private static final String KEY_CLASSNAME = "ClassName";

    private static final String KEY_FAVORITE = "Favorite";

    private static final String KEY_ID = "Id";

    private static final String KEY_LASTLAUNCHTIMESTAMP = "LastLaunchTimestamp";

    private static final String KEY_USAGE_QUANTITY = "UsageQuantity";

    private static final String TABLE_NAME = "ActivityLaunchNumbers";

    public LaunchableActivityPrefs(final Context context) {
        super(context, TABLE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This method deletes a column based on the classname.
     *
     * @param db        The database.
     * @param className The classname of the column to delete.
     */
    private static void deletePreference(final SQLiteDatabase db, final String className) {
        db.delete(TABLE_NAME, KEY_CLASSNAME + "=?", new String[]{className});
    }

    /**
     * This method deletes the {@link LaunchableActivity} from persistent storage.
     *
     * @param launchableActivity The LaunchableActivity to remove from persistent storage.
     */
    public void deletePreference(final LaunchableActivity launchableActivity) {
        final SQLiteDatabase db = getWritableDatabase();

        deletePreference(db, launchableActivity.getComponent().getClassName());

        db.close();
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        final String tableCreate = String.format("CREATE TABLE %s (%S INTEGER PRIMARY KEY, " +
                        "%s TEXT UNIQUE, %s INTEGER, %s INTEGER, %s INTEGER);",
                TABLE_NAME, KEY_ID, KEY_CLASSNAME, KEY_LASTLAUNCHTIMESTAMP,
                KEY_FAVORITE, KEY_USAGE_QUANTITY);

        db.execSQL(tableCreate);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        if (oldVersion < DATABASE_VERSION && newVersion == DATABASE_VERSION) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    /**
     * This method updates a {@link LaunchableActivity} with persistent information.
     *
     * @param launchableActivity The {@link LaunchableActivity} to update.
     */
    public void setPreferences(final LaunchableActivity launchableActivity) {
        final SQLiteDatabase db = getReadableDatabase();

        final String[] whereArgs = {launchableActivity.getComponent().getClassName()};
        final String[] columns = {KEY_LASTLAUNCHTIMESTAMP, KEY_USAGE_QUANTITY, KEY_FAVORITE};
        final Cursor cursor = db.query(TABLE_NAME, columns, KEY_CLASSNAME + "=?", whereArgs, null,
                null, null);

        if (cursor.moveToFirst()) {
            int column = cursor.getColumnIndex(KEY_LASTLAUNCHTIMESTAMP);

            if (column != -1) {
                launchableActivity.setLaunchTime(cursor.getLong(column));
            }

            column = cursor.getColumnIndex(KEY_FAVORITE);

            if (column != -1) {
                launchableActivity.setPriority(cursor.getInt(column));
            }

            column = cursor.getColumnIndex(KEY_USAGE_QUANTITY);

            if (column != -1) {
                launchableActivity.setUsageQuantity(cursor.getInt(column));
            }
        }

        cursor.close();
    }

    /**
     * Write the preferences from the {@link LaunchableActivity} to persistent storage.
     *
     * @param launchableActivity The {@link LaunchableActivity} to write to persistent storage.
     */
    public void writePreference(final LaunchableActivity launchableActivity) {
        final SQLiteDatabase db = getWritableDatabase();
        final ContentValues values = new ContentValues();
        final int priority = launchableActivity.getPriority();
        final int usageQuantity = launchableActivity.getUsageQuantity();
        final String className = launchableActivity.getComponent().getClassName();

        if (priority > 0) {
            values.put(KEY_FAVORITE, priority);
        }

        if (usageQuantity > 0) {
            values.put(KEY_LASTLAUNCHTIMESTAMP, launchableActivity.getLaunchTime());
            values.put(KEY_USAGE_QUANTITY, usageQuantity);
        }

        if (values.size() == 0) {
            deletePreference(db, className);
        } else {
            values.put(KEY_CLASSNAME, className);
            db.replace(TABLE_NAME, null, values);
        }

        db.close();
    }
}
