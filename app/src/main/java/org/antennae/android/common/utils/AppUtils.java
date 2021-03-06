/**
 * Copyright 2015 Antennea Project. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.antennae.android.common.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.antennae.android.common.transport.AppInfo;

/**
 * Created by nsankaran on 6/16/15.
 */
public class AppUtils {

    /*
        Get the App version from installed APK file
     */
    public static int getAppVersionFromApk( Context context ){

        int appversion;

        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            appversion = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }

        return appversion;
    }

    public static AppInfo getAppInfoFromApk( Context context ){

        AppInfo appInfo = new AppInfo();

        try {

            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            appInfo.setFirstInstallTime(packageInfo.firstInstallTime );
            appInfo.setLastUpdateTime( packageInfo.lastUpdateTime);
            appInfo.setAppId( packageInfo.packageName );
            appInfo.setAppVersion( packageInfo.versionCode );

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return appInfo;
    }
}
