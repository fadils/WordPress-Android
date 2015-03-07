package org.wordpress.android.analytics;

import org.json.JSONObject;

import java.io.Serializable;

public class NosaraEvent implements Serializable {

    public static final String LOGTAG = "NosaraEvent";

    private final String mEventName;
    private final String mUser;
    private final String mUserAgent;
    private final long mTimeStamp;
    private final NosaraClient.NosaraUserType mUserType;

    private int mRetryTime = 0;

    private JSONObject mUserProperties;
    private JSONObject mDeviceInfo;

    public NosaraEvent(String mEventName, String userID, NosaraClient.NosaraUserType uType, String userAgent, long timeStamp) {
        this.mEventName = mEventName;
        this.mUser = userID;
        this.mUserType = uType;
        this.mUserAgent = userAgent;
        this.mTimeStamp = timeStamp;
    }

    public String getEventName() {
        return mEventName;
    }

    public String getUser() {
        return mUser;
    }

    public NosaraClient.NosaraUserType getUserType() {
        return mUserType;
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }


    public int getRetryTime() {
        return mRetryTime;
    }

    public String getUserAgent() {
        return mUserAgent;
    }

    public void addRetryCount() {
        mRetryTime += 1;
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
