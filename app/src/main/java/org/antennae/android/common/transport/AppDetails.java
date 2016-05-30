package org.antennae.android.common.transport;

import com.google.gson.Gson;

/**
 * Created by nsankaran on 6/20/15.
 */
public class AppDetails {

    //private String gcmRegistrationId;
    private DeviceInfo deviceInfo;
    private AppInfo appInfo;

    // do we need an appState?


    public AppInfo getAppInfo() {
        return appInfo;
    }

    public void setAppInfo(AppInfo appInfo) {
        this.appInfo = appInfo;
    }


    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String toJson(){
        Gson gson = new Gson();
        return gson.toJson( this);
    }

    public static AppDetails fromJson( String json ){
        Gson gson = new Gson();
        return gson.fromJson(json, AppDetails.class);
    }

}
