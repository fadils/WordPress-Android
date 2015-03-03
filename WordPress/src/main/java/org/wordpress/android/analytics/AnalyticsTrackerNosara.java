package org.wordpress.android.analytics;

import android.content.Context;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class AnalyticsTrackerNosara implements AnalyticsTracker.Tracker {
    public static final String TAG = "AnalyticsTrackerNosara";

    private RequestQueue mRequestQueue;
    private NosaraRestClient mRestClient;

    public AnalyticsTrackerNosara(Context ctx) {
        mRequestQueue = Volley.newRequestQueue(ctx);
        mRestClient = new NosaraRestClient(mRequestQueue);
    }

    private class RestListener implements RestRequest.Listener, RestRequest.ErrorListener {
        @Override
        public void onResponse(final JSONObject response) {
            Log.d(TAG, response.toString());
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            if (volleyError == null) {
                Log.e(TAG, "Tried to log a VolleyError, but the error obj was null!");
                return;
            }
            if (volleyError.networkResponse != null) {
                NetworkResponse networkResponse = volleyError.networkResponse;
                Log.e(TAG, "Network status code: " + networkResponse.statusCode);
                if (networkResponse.data != null) {
                    Log.e(TAG, "Network data: " + new String(networkResponse.data));
                }
            }
            Log.e(TAG, "Volley Error details: " + volleyError.getMessage(), volleyError);
        }
    }


    @Override
    public void track(AnalyticsTracker.Stat stat) {
        track(stat, null);
    }

    @Override
    public void track(AnalyticsTracker.Stat stat, Map<String, ?> properties) {

        JSONObject params = new JSONObject();

        String eventName;

        switch (stat) {
            case APPLICATION_STARTED:
                eventName = "Application Started";
                //instructions.setSuperPropertyToIncrement("Application Started");
                break;
            case APPLICATION_OPENED:
                eventName = "Application Opened";
                //instructions.setSuperPropertyToIncrement("Application Opened");
                //incrementSessionCount();
                break;
            case APPLICATION_CLOSED:
                eventName = "Application Closed";
                break;
            case READER_ACCESSED:
                eventName = "Reader - Accessed";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_reader");
                //instructions.setCurrentDateForPeopleProperty("last_time_accessed_reader");
                break;
            case READER_OPENED_ARTICLE:
                eventName = "Reader - Opened Article";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_opened_article");
                //instructions.setCurrentDateForPeopleProperty("last_time_opened_reader_article");
                break;
            case READER_LIKED_ARTICLE:
                eventName = "Reader - Liked Article";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_liked_article");
                //instructions.setCurrentDateForPeopleProperty("last_time_liked_reader_article");
                break;

            case READER_INFINITE_SCROLL:
                eventName = "Reader - Infinite Scroll";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement(
                //      "number_of_times_reader_performed_infinite_scroll");
                //instructions.setCurrentDateForPeopleProperty("last_time_performed_reader_infinite_scroll");
                break;

            case NOTIFICATIONS_ACCESSED:
                eventName = "Notifications - Accessed";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_notifications");
                //instructions.setCurrentDateForPeopleProperty("last_time_accessed_notifications");
                break;
            case NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS:
                eventName = "Notifications - Opened Notification Details";
                // instructions.
                //        setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_opened_notification_details");
                //instructions.setCurrentDateForPeopleProperty("last_time_opened_notification_details");
                break;

            case STATS_ACCESSED:
                eventName = "Stats - Accessed";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_stats");
                //instructions.setCurrentDateForPeopleProperty("last_time_accessed_stats");
                break;
            case STATS_VIEW_ALL_ACCESSED:
                eventName = "Stats - View All Accessed";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_view_all_screen_stats");
                //instructions.setCurrentDateForPeopleProperty("last_time_accessed_view_all_screen_stats");
                break;
            case STATS_SINGLE_POST_ACCESSED:
                eventName = "Stats - Single Post Accessed";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_single_post_screen_stats");
                //instructions.setCurrentDateForPeopleProperty("last_time_accessed_single_post_screen_stats");
                break;
            case STATS_OPENED_WEB_VERSION:
                eventName = "Stats - Opened Web Version";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_web_version_of_stats");
                //instructions.setCurrentDateForPeopleProperty("last_time_accessed_web_version_of_stats");
                break;
            case STATS_TAPPED_BAR_CHART:
                eventName = "Stats - Tapped Bar Chart";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_tapped_stats_bar_chart");
                //instructions.setCurrentDateForPeopleProperty("last_time_tapped_stats_bar_chart");
                break;
            case STATS_SCROLLED_TO_BOTTOM:
                eventName = "Stats - Scrolled to Bottom";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_scrolled_to_bottom_of_stats");
                //instructions.setCurrentDateForPeopleProperty("last_time_scrolled_to_bottom_of_stats");
                break;
            default:
                eventName = null;
                break;
        }

        if (eventName == null) {
            return;
        }


        try {
            JSONObject singleEventParams = new JSONObject();
            singleEventParams.put("_en", eventName);
            singleEventParams.put("_ui", "16154691");

            JSONArray events = new JSONArray();
            events.put(singleEventParams);

            params.put("events", events);
            mRestClient.track(params, new RestListener(), new RestListener());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        //trackNosaraDataForInstructions(instructions, properties);
    }

    @Override
    public void beginSession() {

    }

    @Override
    public void endSession() {

    }

    @Override
    public void refreshMetadata() {

    }

    @Override
    public void clearAllData() {

    }

    @Override
    public void registerPushNotificationToken(String regId) {

    }
}
