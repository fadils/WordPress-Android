package org.wordpress.android.analytics;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

public class NosaraRestClient {
    public static final String TAG = "NosaraRestClient";

    private static String DEFAULT_USER_AGENT = "WordPress Nosara Client for Android";

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


    protected static final String NOSARA_REST_API_ENDPOINT_URL_V1_1 = "https://public-api.wordpress.com/rest/v1.1/";
    private String mUserAgent = NosaraRestClient.DEFAULT_USER_AGENT;
    private RequestQueue mQueue;
    private String mRestApiEndpointURL;

    public NosaraRestClient(RequestQueue queue) {
        mQueue = queue;
        mRestApiEndpointURL = NOSARA_REST_API_ENDPOINT_URL_V1_1;
    }

    public NosaraRestClient(RequestQueue queue, String endpointURL) {
        this(queue);
        mRestApiEndpointURL = endpointURL;
    }

    // Volley send the request
    public JsonObjectRequest track(JSONObject jsonRequest, Listener<JSONObject> listener, ErrorListener errorListener) {
        String path = "tracks/record";
        NosaraRestRequest request = this.post(path, jsonRequest, listener, errorListener);
        mQueue.add(request);
        return request;
    }

    public NosaraRestRequest get(String path, Listener<JSONObject> listener, ErrorListener errorListener) {
        return makeRequest(Method.GET, getAbsoluteURL(path), null, listener, errorListener);
    }

    public NosaraRestRequest post(String path, JSONObject jsonRequest, Listener<JSONObject> listener, ErrorListener errorListener) {
        return this.post(path, jsonRequest, null, listener, errorListener);
    }

    public NosaraRestRequest post(final String path, JSONObject jsonRequest, RetryPolicy retryPolicy, Listener<JSONObject> listener, ErrorListener errorListener) {
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
}
