package org.antennae.notifyapp.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
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
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.antennae.android.common.AntennaeContext;
import org.antennae.android.common.Constants;
import org.antennae.android.common.gcm.RegistrationIntentService;
import org.antennae.notifyapp.adapters.AlertAdapter;
import org.antennae.gcmtests.gcmtest.R;
import org.antennae.notifyapp.constants.Globals;
import org.antennae.notifyapp.events.EventManager;
import org.antennae.notifyapp.model.Alert;
import org.antennae.notifyapp.model.AlertSeverityEnum;
import org.antennae.notifyapp.listeners.AlertReceivedListener;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;


public class MainActivity extends ActionBarActivity implements AlertReceivedListener {

    public static final String EXTRA_MESSAGE = "message";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public static final String ALERT_PARAM = "alert";
    public static final String ALERT_NOTIFICATION_INTENT_FILTER = "ALERT_NOTIFICATION_INTENT_FILTER";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private boolean isReceiverRegistered;
    private WebSocketClient mWebSocketClient;

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

            // get the server details and store it in the PREFS
            AntennaeContext antennaeContext = new AntennaeContext(this);
            SharedPreferences.Editor editor = antennaeContext.getPreferences().edit();

            editor.putString(Constants.DEFAULT_SERVER_HOST_VALUE, String.valueOf(R.string.ANTENNAE_SERVER_HOST));
            editor.putInt(Constants.DEFAULT_SERVER_PORT_NAME, R.integer.ANTENNAE_SERVER_PORT);
            editor.putString(Constants.DEFAULT_SERVER_PROTOCOL_VALUE, String.valueOf(R.string.ANTENNAE_SERVER_PROTOCOL));
            editor.apply();

            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService( intent );
        }

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


        //connectToWebSocket();
        connectToWS();
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

    private void connectToWebSocket(){

        URI uri;
        try {
            uri = new URI("ws://192.168.1.114:8080/ws");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {

            @Override
            public void onOpen(ServerHandshake handShakeData) {
                Log.i("Websocket", "Opened");
                mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
            }

            @Override
            public void onMessage(String message) {
                final String msg = message;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = (TextView)findViewById(R.id.tvMessage);
                        textView.setText(textView.getText() + "\n" + msg);
                    }
                });
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {

            }

            @Override
            public void onError(Exception ex) {

            }
        };

        mWebSocketClient.connect();
    }

    private void connectToWS(){
        Future<WebSocket> webSocketFuture = AsyncHttpClient.getDefaultInstance().websocket("ws://192.168.1.4:8080/ws", "my-protocol", new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }
                webSocket.send("a string");
                webSocket.send(new byte[10]);
                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    public void onStringAvailable(String s) {
                        System.out.println("I got a string: " + s);
                    }
                });
                webSocket.setDataCallback(new DataCallback() {
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList byteBufferList) {
                        System.out.println("I got some bytes!");
                        // note that this data has been read
                        byteBufferList.recycle();
                    }
                });
            }
        });

        try {
            WebSocket ws = webSocketFuture.get();
            ws.send("Hello World");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}

