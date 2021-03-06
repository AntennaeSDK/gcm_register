package org.antennae.android.common.gcm;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

import org.antennae.android.common.Constants;

/**
 * Created by snambi on 5/27/16.
 */
public class AntennaeInstanceIDListenerService extends InstanceIDListenerService {

    private static final String TAG = "AntennaeInstanceIDLS";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */

    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        Intent intent = new Intent(this, RegistrationIntentService.class);
        intent.putExtra(Constants.GCM_TOKEN_REFRESH, true);
        startService(intent);
    }
    // [END refresh_token]
}
