package org.antennea.android.tasks;

import android.os.AsyncTask;
import android.util.Log;

import org.antennae.gcmtests.gcmtest.Globals;
import org.antennea.android.transport.AppDetails;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by nsankaran on 6/23/15.
 */
public class AntenneaServerRegistrationTask extends AsyncTask {

    String serverUrl;
    AppDetails appDetails;

    public AntenneaServerRegistrationTask( String serverUrl, AppDetails appDetails ){
        this.serverUrl = serverUrl;
        this.appDetails = appDetails;
    }

    @Override
    protected Object doInBackground(Object[] params) {

        try {

            HttpPost post = new HttpPost(serverUrl);
            post.setEntity(new StringEntity(appDetails.toJson()));
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-type", "application/json");

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = httpClient.execute(post);

            if( response != null ){
                Log.d(Globals.TAG, "Http Server Response: "+ response);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
