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

package org.antennae.android.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.antennae.android.common.transport.AppDetails;
import org.antennae.android.common.transport.AppInfo;
import org.antennae.android.common.transport.DeviceInfo;
import org.antennae.android.common.transport.PhoneTypeEnum;
import org.antennae.android.common.utils.AppUtils;
import org.antennae.notifyapp.constants.Globals;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by nambi sankaran on 6/16/15.
 */
public class AntennaeContext {

    private SharedPreferences preferences=null;
    private Context context;

    public AntennaeContext(Context context){
        this.context = context;
        this.preferences = context.getSharedPreferences(Constants.PREF_ANTENNAE, Context.MODE_PRIVATE);
    }

    public SharedPreferences getPreferences(){
        return preferences;
    }

    /**
        Save registrationId from GCM and appVersion in Shared-Preferences
     */
    public void saveGCMRegistrationIdAndAppVersion(String registrationId){

        if( registrationId == null || registrationId.trim().equals("")){
            throw new NullPointerException("registrationId cannot be null or empty");
        }

        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(Constants.ANTENNAE_REGISTRATION_ID, registrationId);
        editor.apply();

        // save appVersion
        int appVersion = AppUtils.getAppVersionFromApk(context);
        saveAppVersion(appVersion);
    }

    public boolean isNewTokenNeeded(){
        boolean result = false;
        if( !isRegistered() || isNewRegistrationIdNeeded() ){
            result = true;
        }
        return result;
    }

    public boolean isRegistered(){
        boolean result=false;
        if( getPreferences().contains(Constants.ANTENNAE_REGISTRATION_ID) ){
            if( getPreferences().getString(Constants.ANTENNAE_REGISTRATION_ID, null) != null ){
                result = true;
            }
        }
        return result;
    }

    public String getGcmTokenId(){
        String result=null;
        if( isRegistered() ){
            result = getPreferences().getString(Constants.ANTENNAE_REGISTRATION_ID,null);
        }

        return result;
    }

    public AppDetails getAppDetails(){
        AppDetails appDetails = new AppDetails();

        appDetails.setAppInfo(getAppInfo());
        appDetails.setDeviceInfo( getDeviceInfo());

        return appDetails;
    }

    public DeviceInfo getDeviceInfo(){

        DeviceInfo deviceInfo = new DeviceInfo();

        // get the device details
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if( telephonyManager != null ){

            PhoneTypeEnum phonetype = null;
            int pt = telephonyManager.getPhoneType();

            switch (pt) {
                case (TelephonyManager.PHONE_TYPE_CDMA):
                    phonetype = PhoneTypeEnum.CDMA;
                    break;
                case (TelephonyManager.PHONE_TYPE_GSM) :
                    phonetype = PhoneTypeEnum.GSM;
                    break;
                case (TelephonyManager.PHONE_TYPE_SIP):
                    phonetype = PhoneTypeEnum.SIP;
                    break;
                default:
                    phonetype = PhoneTypeEnum.NONE;
                    break;
            }

            // Getting Device Id
            deviceInfo.setDeviceId( telephonyManager.getDeviceId());

            // Getting software version( not sdk version )
            deviceInfo.setSoftwareVersion(telephonyManager.getDeviceSoftwareVersion());

            // Get the phone’s number
            deviceInfo.setPhoneNumber(telephonyManager.getLine1Number());

            // Getting connected network iso country code
            deviceInfo.setNetworkCountryIso(telephonyManager.getNetworkCountryIso());

            // Getting the connected network operator ID
            deviceInfo.setNetworkOperatorId(telephonyManager.getNetworkOperator());

            // Getting the connected network operator name
            deviceInfo.setNetworkOperatorName( telephonyManager.getNetworkOperatorName() );
        }

        return deviceInfo;
    }

    public AppInfo getAppInfo(){
        return AppUtils.getAppInfoFromApk(context);
    }

    public boolean isNewRegistrationIdNeeded(){
        boolean result = false;
        if( isNewAppVersion() ){
            result = true;
        }

        return result;
    }

    public boolean isNewAppVersion(){
        boolean result = false;

        int appVersion = AppUtils.getAppVersionFromApk(context);
        if( getSavedAppVersion() < appVersion ){
            result = true;
        }

        return result;
    }

    public void saveAppVersion( int appVersion ){
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putInt(Constants.ANTENNAE_APP_VERSION, appVersion);
        editor.apply();
    }

    public int getSavedAppVersion(){
        int savedAppVersion = getPreferences().getInt(Constants.ANTENNAE_APP_VERSION, -1000000);
        return savedAppVersion;
    }


    public void sendAppDetailsToServer() {
        // TODO: get the ip address and port of the server from a config file

        // the call the API to send the token
        AppDetails appDetails = getAppDetails();
        getPreferences().getString(Constants.DEFAULT_SERVER_PROTOCOL_VALUE, Constants.DEFAULT_SERVER_PROTOCOL_VALUE);
        sendAppDetailsToServer(Constants.DEFAULT_SERVER_PROTOCOL_VALUE, Constants.DEFAULT_SERVER_HOST_VALUE, Constants.DEFAULT_SERVER_PORT_VALUE, appDetails);
    }

    public void sendAppDetailsToServer(String protocol, String host, int port, AppDetails appDetails){

        String serverUrl =  protocol + "://" + host + ":" + port + Constants.SERVER_REGISTRATION_URL;

        try {

            HttpPost post = new HttpPost(serverUrl);

            post.setEntity(new StringEntity(appDetails.toJson()));
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-type", "application/json");

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = httpClient.execute(post);

            if( response != null ){
                Log.d(Globals.TAG, "Http Server Response: " + response);
            }

            if(response.getStatusLine().getStatusCode() == 200 ){
                // mark that the application details are sent to server
                SharedPreferences.Editor editor = getPreferences().edit();
                editor.putBoolean(Constants.SENT_APP_DETAILS_TO_SERVER, true);
                editor.apply();
            }else{
                // mark, so, that app retries again to the send the details to the server
                SharedPreferences.Editor editor = getPreferences().edit();
                editor.putBoolean(Constants.SENT_APP_DETAILS_TO_SERVER, false);
                editor.apply();
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isAppDetailsSentToServer(){
        boolean result =false;

        if ( getPreferences().getBoolean(Constants.SENT_APP_DETAILS_TO_SERVER, false)) {
            result = true;
        }

        return result;
    }
}
