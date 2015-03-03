package org.wordpress.android.analytics;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.DisplayMetrics;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class AnalyticsTrackerNosara implements AnalyticsTracker.Tracker {
    public static final String LOGTAG = "AnalyticsTrackerNosara";


    private NosaraRestClient mRestClient;

    public AnalyticsTrackerNosara(Context ctx) {
        if (null == ctx || !checkBasicConfiguration(ctx)) {
            mRestClient = null;
            return;
        }
        mRestClient = new NosaraRestClient(ctx);
    }


    private static boolean checkBasicConfiguration(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        final String packageName = context.getPackageName();

        if (PackageManager.PERMISSION_GRANTED != packageManager.checkPermission("android.permission.INTERNET", packageName)) {
            Log.w(LOGTAG, "Package does not have permission android.permission.INTERNET - Nosara Client will not work at all!");
            Log.i(LOGTAG, "You can fix this by adding the following to your AndroidManifest.xml file:\n" +
                    "<uses-permission android:name=\"android.permission.INTERNET\" />");
            return false;
        }

        return true;
    }


    @Override
    public void track(AnalyticsTracker.Stat stat) {
        track(stat, null);
    }

    @Override
    public void track(AnalyticsTracker.Stat stat, Map<String, ?> properties) {
        if (mRestClient == null) {
            return;
        }

        String eventName;

        switch (stat) {
            case APPLICATION_STARTED:
                eventName = "application_started";
                break;
            case APPLICATION_OPENED:
                eventName = "application_opened";
                break;
            case APPLICATION_CLOSED:
                eventName = "application_closed";
                break;
            case READER_ACCESSED:
                eventName = "reader_accessed";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_reader");
                //instructions.setCurrentDateForPeopleProperty("last_time_accessed_reader");
                break;
            case READER_OPENED_ARTICLE:
                eventName = "reader_opened_article";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_opened_article");
                //instructions.setCurrentDateForPeopleProperty("last_time_opened_reader_article");
                break;
            case READER_LIKED_ARTICLE:
                eventName = "reader_liked_article";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_liked_article");
                //instructions.setCurrentDateForPeopleProperty("last_time_liked_reader_article");
                break;
            case READER_INFINITE_SCROLL:
                eventName = "reader_infinite_scroll_performed";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement(
                //      "number_of_times_reader_performed_infinite_scroll");
                //instructions.setCurrentDateForPeopleProperty("last_time_performed_reader_infinite_scroll");
                break;
            case NOTIFICATIONS_ACCESSED:
                eventName = "notifications_accessed";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_notifications");
                //instructions.setCurrentDateForPeopleProperty("last_time_accessed_notifications");
                break;
            case STATS_ACCESSED:
                eventName = "stats_accessed";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_stats");
                //instructions.setCurrentDateForPeopleProperty("last_time_accessed_stats");
                break;
            case STATS_VIEW_ALL_ACCESSED:
                eventName = "stats_view_all_accessed";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_view_all_screen_stats");
                //instructions.setCurrentDateForPeopleProperty("last_time_accessed_view_all_screen_stats");
                break;
            case STATS_SINGLE_POST_ACCESSED:
                eventName = "stats_single_post_accessed";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_single_post_screen_stats");
                //instructions.setCurrentDateForPeopleProperty("last_time_accessed_single_post_screen_stats");
                break;
            case STATS_OPENED_WEB_VERSION:
                eventName = "stats_web_version_accessed";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_web_version_of_stats");
                //instructions.setCurrentDateForPeopleProperty("last_time_accessed_web_version_of_stats");
                break;
            case STATS_TAPPED_BAR_CHART:
                eventName = "stats_tapped_bar_chart";
                //instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_tapped_stats_bar_chart");
                //instructions.setCurrentDateForPeopleProperty("last_time_tapped_stats_bar_chart");
                break;
            case STATS_SCROLLED_TO_BOTTOM:
                eventName = "stats_scrolled_to_bottom";
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
            mRestClient.track(singleEventParams);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //trackNosaraDataForInstructions(instructions, properties);
    }

    @Override
    public void beginSession() {
        if (mRestClient == null) {
            return;
        }
    }

    @Override
    public void endSession() {
        if (mRestClient == null) {
            return;
        }
    }

    @Override
    public void refreshMetadata() {
        if (mRestClient == null) {
            return;
        }
    }

    @Override
    public void clearAllData() {
        if (mRestClient == null) {
            return;
        }
    }

    @Override
    public void registerPushNotificationToken(String regId) {
        return;
    }
}
