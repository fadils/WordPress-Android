package org.wordpress.android.analytics;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class NosaraEvent implements Serializable {

    public static final String LOGTAG = "NosaraEvent";

    private final String mEventName;
    private final String mUserID;
    private final String mUserAgent;
    private final long mTimeStamp;

    private int mRetryTime = 0;

    private JSONObject mUserProperties;
    private JSONObject mDeviceInfo;

    public NosaraEvent(String mEventName, String userID, String userAgent, long timeStamp) {
        this.mEventName = mEventName;
        this.mUserID = userID;
        this.mUserAgent = userAgent;
        this.mTimeStamp = timeStamp;
    }

    public JSONObject getJSONObject() {
        try {
            JSONObject eventJSON = new JSONObject();
            eventJSON.put("_en", mEventName);
            eventJSON.put("_ui", mUserID);
            eventJSON.put("_via_ua", mUserAgent);
            eventJSON.put("_ts", mTimeStamp);
            if (mUserProperties != null && mUserProperties.length() > 0 ) {
                eventJSON.put("user_info", mUserProperties.toString());
            }
            if (mDeviceInfo != null && mDeviceInfo.length() > 0) {
                eventJSON.put("device_info", mDeviceInfo.toString());
            }
            return eventJSON;
        } catch (JSONException err) {
            Log.e(LOGTAG, "Cannot writhe the JSON representation of this object", err);
            return null;
        }
    }

    public void addRetryCount() {
        mRetryTime += 1;
    }

    public int getRetryTime() {
        return mRetryTime;
    }

    public boolean isStillValid() {
        // Should we discard events > 5 days?
        // Should we count the retry times?
        return true;
    }

    public void setUserProperties(JSONObject userProperties) {
        this.mUserProperties = userProperties;
    }

    public void setDeviceInfo(JSONObject deviceInfo) {
        this.mDeviceInfo = deviceInfo;
    }

    public JSONObject getUserProperties() {

        return mUserProperties;
    }

    public JSONObject getDeviceInfo() {
        return mDeviceInfo;
    }
}
