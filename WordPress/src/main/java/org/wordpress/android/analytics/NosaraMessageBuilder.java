package org.wordpress.android.analytics;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

class NosaraMessageBuilder {

    public static synchronized JSONObject getJSONObject(NosaraEvent event) {
        try {
            JSONObject eventJSON = new JSONObject();
            eventJSON.put("_en", event.getEventName());

            eventJSON.put("_via_ua", event.getUserAgent());
            eventJSON.put("_ts", event.getTimeStamp());

            if (event.getUserType() == NosaraClient.NosaraUserType.ANON) {
                eventJSON.put("_ut", "anon");
                eventJSON.put("_ui", event.getUser());
            } else {
                eventJSON.put("_ul", event.getUser());
            }

            if (event.getUserProperties() != null && event.getUserProperties().length() > 0) {
                unfolderProperties(event.getUserProperties(), "user_info_", eventJSON);
            }
            if (event.getDeviceInfo() != null && event.getDeviceInfo().length() > 0) {
                unfolderProperties(event.getDeviceInfo(), "device_info_", eventJSON);
            }
            return eventJSON;
        } catch (JSONException err) {
            Log.e(NosaraClient.LOGTAG, "Cannot write the JSON representation of this object", err);
            return null;
        }
    }

    // Nosara only strings property values. Don't convert JSON objs by calling toString()
    // otherwise they will be likely un-queryable
    public static void unfolderProperties(JSONObject objectToFlatten, String prefix, JSONObject eventJSON) {
        if (objectToFlatten == null || eventJSON == null) {
            return;
        }

        if (prefix == null) {
            Log.w(NosaraClient.LOGTAG, " Unfolding props with an empty key. This could be an error!");
            prefix = "";
        }

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
                Log.e(NosaraClient.LOGTAG, "Cannot write the flatten JSON representation of this object", e);
            }
        }
    }
}