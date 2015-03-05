package org.wordpress.android.analytics;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Iterator;

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
                unfolderProperties(mUserProperties, "user_info_", eventJSON);
            }
            if (mDeviceInfo != null && mDeviceInfo.length() > 0) {
                unfolderProperties(mDeviceInfo, "device_info_", eventJSON);
            }
            return eventJSON;
        } catch (JSONException err) {
            Log.e(LOGTAG, "Cannot write the JSON representation of this object", err);
            return null;
        }
    }

    // Nosora only strings property values. Don't convert JSON objs with toString()
    // Otherwise they will be likely un-queryable
    private void unfolderProperties(JSONObject objectToFlatten, String prefix, JSONObject eventJSON) {
        Iterator<String> iter = objectToFlatten.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            try {
                Object value = objectToFlatten.get(key);
                String valueString;
                if (value != null) {
                    valueString = String.valueOf(value);
                } else {
                    valueString = "";
                }
                eventJSON.put(String.valueOf(prefix + key).toLowerCase(), valueString);
            } catch (JSONException e) {
                // Something went wrong!
                Log.e(LOGTAG, "Cannot write the flatten JSON representation of this object", e);
            }
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
