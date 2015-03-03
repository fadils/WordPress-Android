package org.wordpress.android.analytics;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NosaraRestClient {
    public static final String LOGTAG = "NosaraRestClient";

    public static final String LIB_VERSION = "0.0.1";
    protected static final String DEFAULT_USER_AGENT = "Nosara Client for Android";
    protected static final String NOSARA_REST_API_ENDPOINT_URL_V1_1 = "https://public-api.wordpress.com/rest/v1.1/";

    /**
     * Socket timeout in milliseconds for rest requests
     */
    public static final int REST_TIMEOUT_MS = 30000;

    /**
     * Default number of retries for POST rest requests
     */
    public static final int REST_MAX_RETRIES_POST = 0;

    /**
     * Default number of retries for GET rest requests
     */
    public static final int REST_MAX_RETRIES_GET = 3;

    /**
     * Default backoff multiplier for rest requests
     */
    public static final float REST_BACKOFF_MULT = 2f;

    private String mUserAgent = NosaraRestClient.DEFAULT_USER_AGENT;
    private final RequestQueue mQueue;
    private String mRestApiEndpointURL;
    private final Map<String, String> mDeviceInfo;
    private final NosaraExtendedSystemInformation extendedSystemInformation;


    public NosaraRestClient(Context ctx) {
        mQueue = Volley.newRequestQueue(ctx);
        mRestApiEndpointURL = NOSARA_REST_API_ENDPOINT_URL_V1_1;

        final Map<String, String> deviceInfo = new HashMap<String, String>();
        deviceInfo.put("$android_lib_version", LIB_VERSION);
        deviceInfo.put("$android_os", "Android");
        deviceInfo.put("$android_os_version", Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);
        deviceInfo.put("$android_manufacturer", Build.MANUFACTURER == null ? "UNKNOWN" : Build.MANUFACTURER);
        deviceInfo.put("$android_brand", Build.BRAND == null ? "UNKNOWN" : Build.BRAND);
        deviceInfo.put("$android_model", Build.MODEL == null ? "UNKNOWN" : Build.MODEL);
        try {
            final PackageManager manager = ctx.getPackageManager();
            final PackageInfo info = manager.getPackageInfo(ctx.getPackageName(), 0);
            deviceInfo.put("$android_app_version", info.versionName);
            deviceInfo.put("$android_app_version_code", Integer.toString(info.versionCode));
        } catch (final PackageManager.NameNotFoundException e) {
            Log.e(LOGTAG, "Exception getting app version name", e);
        }
        mDeviceInfo = Collections.unmodifiableMap(deviceInfo);
        extendedSystemInformation = new NosaraExtendedSystemInformation(ctx);
    }

    public NosaraRestClient(Context ctx, String endpointURL) {
        this(ctx);
        mRestApiEndpointURL = endpointURL;
    }

    // Volley send the request
    public JsonObjectRequest track(JSONObject singleEvent) {
       //FIXME: Send the request to server. One request per connection for now. No BATCHING
        JSONObject requestJSONObject = new JSONObject();
       try {
           singleEvent.put("_via_ua", getUserAgent());
           JSONArray events = new JSONArray();
           events.put(singleEvent);
           requestJSONObject.put("events", events);

           JSONObject commonProps = new JSONObject();
           commonProps.put("device_info", new JSONObject(mDeviceInfo));
           commonProps.put("extended_device_info", getExtendedSystemInfo());

           requestJSONObject.put("commonProps", commonProps);
       } catch (JSONException err) {
           Log.e(LOGTAG, "Exception creating the request JSON object", err);
           return null;
       }
        String path = "tracks/record";
        NosaraRestListener nosaraRestListener = new NosaraRestListener(requestJSONObject);
        NosaraRestRequest request = this.post(path, requestJSONObject, nosaraRestListener, nosaraRestListener);
        mQueue.add(request);
        return request;
    }

    private NosaraRestRequest get(String path, Listener<JSONObject> listener, ErrorListener errorListener) {
        return makeRequest(Method.GET, getAbsoluteURL(path), null, listener, errorListener);
    }

    private NosaraRestRequest post(String path, JSONObject jsonRequest, Listener<JSONObject> listener, ErrorListener errorListener) {
        return this.post(path, jsonRequest, null, listener, errorListener);
    }

    private NosaraRestRequest post(final String path, JSONObject jsonRequest, RetryPolicy retryPolicy, Listener<JSONObject> listener, ErrorListener errorListener) {
        final NosaraRestRequest request = makeRequest(Method.POST, getAbsoluteURL(path), jsonRequest, listener, errorListener);
        if (retryPolicy == null) {
            retryPolicy = new DefaultRetryPolicy(REST_TIMEOUT_MS, REST_MAX_RETRIES_POST,
                    REST_BACKOFF_MULT); //Do not retry on failure
        }
        request.setRetryPolicy(retryPolicy);
        return request;
    }

    private NosaraRestRequest makeRequest(int method, String url, JSONObject jsonRequest, Listener<JSONObject> listener,
                                          ErrorListener errorListener) {
        NosaraRestRequest request = new NosaraRestRequest(method, url, jsonRequest, listener, errorListener);
        request.setUserAgent(mUserAgent);
        return request;
    }

    private String getAbsoluteURL(String url) {
        // if it already starts with our endpoint, let it pass through
        if (url.indexOf(mRestApiEndpointURL) == 0) {
            return url;
        }
        // if it has a leading slash, remove it
        if (url.indexOf("/") == 0) {
            url = url.substring(1);
        }
        // prepend the endpoint
        return String.format("%s%s", mRestApiEndpointURL, url);
    }

    //Sets the User-Agent header to be sent with each future request.
    public void setUserAgent(String userAgent) {
        mUserAgent = userAgent;
    }

    public String getUserAgent() {
        return mUserAgent;
    }


    private class NosaraRestListener implements Response.Listener<JSONObject>, Response.ErrorListener {
        private final JSONObject mRequestObject; // Keep a reference to the opbj sent on the wire.

        public NosaraRestListener(final JSONObject request) {
            this.mRequestObject = request;
        }

        @Override
        public void onResponse(final JSONObject response) {
            Log.d(LOGTAG, response.toString());
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            if (volleyError == null) {
                Log.e(LOGTAG, "Tried to log a VolleyError, but the error obj was null!");
                return;
            }
            if (volleyError.networkResponse != null) {
                NetworkResponse networkResponse = volleyError.networkResponse;
                Log.e(LOGTAG, "Network status code: " + networkResponse.statusCode);
                if (networkResponse.data != null) {
                    Log.e(LOGTAG, "Network data: " + new String(networkResponse.data));
                }
            }
            Log.e(LOGTAG, "Volley Error details: " + volleyError.getMessage(), volleyError);
        }
    }


    private JSONObject getExtendedSystemInfo() {
        if (extendedSystemInformation == null) {
            return null;
        }

        JSONObject extSysInfo = new JSONObject();
        try {
            extSysInfo.put("bluetooth_version", extendedSystemInformation.getBluetoothVersion());
            extSysInfo.put("bluetooth_enabled", extendedSystemInformation.isBluetoothEnabled());
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing bluetooth info values in JSON object", e);
        }

        try {
            extSysInfo.put("current_network_operator", extendedSystemInformation.getCurrentNetworkOperator());
            extSysInfo.put("phone_radio_type", extendedSystemInformation.getPhoneRadioType()); // NONE - GMS - CDMA - SIP
            extSysInfo.put("wifi_connected", extendedSystemInformation.isWifiConnected());
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing network info values in JSON object", e);
        }

        try {
            extSysInfo.put("has_NFS", extendedSystemInformation.hasNFC());
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing has_NFS value in JSON object", e);
        }
        try {
            extSysInfo.put("has_telephony", extendedSystemInformation.hasTelephony());
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing has_telephony value in JSON object", e);
        }

        try {
            DisplayMetrics dMetrics = extendedSystemInformation.getDisplayMetrics();
            extSysInfo.put("display_density_dpi", dMetrics.densityDpi);
            extSysInfo.put("display_width_px", dMetrics.widthPixels);
            extSysInfo.put("display_height_px", dMetrics.heightPixels);
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing DisplayMetrics values in JSON object", e);
        }

        return extSysInfo;
    }

}
