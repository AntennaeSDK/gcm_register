package org.antennae.android.common;

import org.antennae.android.common.transport.AppDetails;
import org.antennae.android.common.transport.AppInfo;
import org.antennae.android.common.transport.DeviceInfo;

/**
 * Created by snambi on 5/30/16.
 */
public interface IAntennaeContext {

    // methods related to GCM registration token
    public void saveRegistrationIdAndAppVersion(String registrationId);
    public boolean isNewRegistrationIdNeeded();
    public boolean isNewTokenNeeded();
    public boolean isRegistered();
    public String getRegistrationId();


    // methods related to app details and version
    public AppDetails getAppDetails();
    public AppInfo getAppInfo();
    public boolean isNewAppVersion();


    // methods related to device details
    public DeviceInfo getDeviceInfo();


    // methods related to interacting with the server
    public void sendAppDetailsToServer();
    public void sendAppDetailsToServer( String host, int port, AppDetails appDetails );
    public boolean isAppDetailsSentToServer();

}
