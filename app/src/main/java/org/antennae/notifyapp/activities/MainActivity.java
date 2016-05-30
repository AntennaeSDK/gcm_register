package org.antennae.notifyapp.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.antennae.android.common.Constants;
import org.antennae.android.common.gcm.RegistrationIntentService;
import org.antennae.notifyapp.adapters.AlertAdapter;
import org.antennae.gcmtests.gcmtest.R;
import org.antennae.notifyapp.constants.Globals;
import org.antennae.notifyapp.events.EventManager;
import org.antennae.notifyapp.model.Alert;
import org.antennae.notifyapp.model.AlertSeverityEnum;
import org.antennae.notifyapp.listeners.AlertReceivedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class MainActivity extends ActionBarActivity implements AlertReceivedListener {

    public static final String EXTRA_MESSAGE = "message";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public static final String ALERT_PARAM = "alert";
    public static final String ALERT_NOTIFICATION_INTENT_FILTER = "ALERT_NOTIFICATION_INTENT_FILTER";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private boolean isReceiverRegistered;

    ListView lvMessages;
    private List<Alert> alerts = new ArrayList<Alert>();
    private AlertAdapter alertsAdapter;

    Context context;
    AtomicInteger msgId = new AtomicInteger();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //register the listeners with the even manager
        EventManager eventManager = EventManager.getInstance();
        eventManager.registerAlertReceivedListener(this);

//        context = getApplicationContext();
//        SharedPreferences pref= context.getSharedPreferences( Constants.PREF_ANTENNAE , MODE_PRIVATE);


        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                //mRegistrationProgressBar.setVisibility(ProgressBar.GONE);

                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);

                Log.i("BroadcastReceiver", "onReceive()");

                boolean sentToken = sharedPreferences.getBoolean(Constants.SENT_APP_DETAILS_TO_SERVER, false);

                if (sentToken) {
                    //mInformationTextView.setText(getString(R.string.gcm_send_message));
                } else {
                    //mInformationTextView.setText(getString(R.string.token_error_message));
                }
            }
        };

        // Registering BroadcastReceiver
        registerReceiver();

        if( checkPlayServices() ){
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService( intent );
        }

        /*
        GcmWrapper gcmwrapper = new GcmWrapper(context);
        String registrationId = gcmwrapper.getRegistrationId();

        if( registrationId == null ){
            gcmwrapper.registerWithGcmAsync();
        }
        */

        setContentView(R.layout.activity_main);

        lvMessages = (ListView) findViewById(R.id.lvMessages);

        populateAlerts();

        lvMessages.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent i = new Intent(MainActivity.this, MessageDetailActivity.class);

                i.putExtra(ALERT_PARAM, alerts.get(position));

                startActivity(i);
            }
        });
    }

    private void populateAlerts() {

        //alerts = new ArrayList<Alert>();

        Alert alert1 = new Alert("High I/O on ADP Apps", "ADP backend has abnormally high IO", AlertSeverityEnum.HIGH.toString(), "Join the bridge on 1-873-555-3846 #556");
        Alert alert2 = new Alert("MySQL index issue", "MySql index is slower on quote system", AlertSeverityEnum.SEVERE.toString(), "Join the bridge on 1-873-555-3846 #598");

        alerts.add(alert1);
        alerts.add( alert2 );

        alertsAdapter = new AlertAdapter(this, alerts);

        // Attach the adapter to a ListView
        lvMessages.setAdapter(alertsAdapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {

        boolean result = false;

        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int resultCode = googleAPI.isGooglePlayServicesAvailable(this);

        if( resultCode != ConnectionResult.SUCCESS ){
            if( googleAPI.isUserResolvableError(resultCode) ){
                googleAPI.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }else{
                Log.i(Globals.TAG, "This device is not supported.");
                finish();
            }
        }else{
            result = true;
        }

        return result;
    }

    @Override
    public void onReceive(Alert alert) {
        if( alert != null ){

            // add to the beginning of the list.
            // so that it is easier to see
            alerts.add(0, alert);

            // update view on the main thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    alertsAdapter.notifyDataSetChanged();

                }
            });
        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        isReceiverRegistered = false;
        super.onPause();
    }

    private void registerReceiver(){
        if(!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                    new IntentFilter(Constants.REGISTRATION_COMPLETE));
            isReceiverRegistered = true;
        }
    }
}

