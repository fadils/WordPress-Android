/**
 * Copyright 2015 Automattic Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wordpress.android.ui.posts.actions;

import android.util.Log;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.posts.EditPostSettingsFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.UrlUtils;

import java.util.HashMap;
import java.util.Map;

public class PostActions {
    /**
     * Helper to sync featured image that's already published in Settings.
     *
     * @param post
     * Post that's already uploaded and is now being opened/edited
     *
     * @param editPostSettingsFragment
     * Settings fragment in EditPostActivity
     */
    public static void syncFeaturedImageInSettings(final Post post, final EditPostSettingsFragment editPostSettingsFragment) {

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                String featuredImageURL = JSONUtil.getString(jsonObject, "featured_image");
                if (!featuredImageURL.isEmpty()) {
                    post.setFeaturedImage(featuredImageURL);
                    editPostSettingsFragment.setFeaturedImage(post.getFeaturedImage());
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.POSTS, volleyError);
            }
        };
        AppLog.d(AppLog.T.POSTS, "getting featured image");
        String path = "sites/" + UrlUtils.urlEncode(UrlUtils.getDomainFromUrl(WordPress.getCurrentBlog().getHomeURL()))
                + "/posts/" + Integer.parseInt(post.getRemotePostId());
        WordPress.getRestClientUtilsV1_1().get(path, null, null, listener, errorListener);
    }

    /**
     * Helper to remove featured image from Settings.
     *
     * @param post
     * Current post
     */
    public static void removeFeaturedImageInSettings(final Post post) {

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                String featuredImageURL = JSONUtil.getString(jsonObject, "featured_image");
                if (featuredImageURL != "") {
                    post.setFeaturedImage("");
                    updatePostFeaturedImage(post);
                    AppLog.i(AppLog.T.POSTS, "Featured Image removed");
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.POSTS, volleyError);
            }
        };
        AppLog.d(AppLog.T.POSTS, "getting featured image");
        String path = "sites/" + UrlUtils.urlEncode(UrlUtils.getDomainFromUrl(WordPress.getCurrentBlog().getHomeURL()))
                + "/posts/" + Integer.parseInt(post.getRemotePostId());
        WordPress.getRestClientUtilsV1_1().get(path, null, null, listener, errorListener);
    }

    /**
     * Update remote post attributes.
     * Specifically used to handle featured_image management in Android.
     */
    public static void updatePostFeaturedImage(final Post post) {

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                Log.i("Post JSON", jsonObject.toString());
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.POSTS, volleyError);
            }
        };
        AppLog.d(AppLog.T.POSTS, "update post featured image");
        String path = "sites/" + UrlUtils.urlEncode(UrlUtils.getDomainFromUrl(WordPress.getCurrentBlog().getHomeURL()))
                + "/posts/" + Integer.parseInt(post.getRemotePostId());
        Map<String, String> params = new HashMap<>();
        params.put("featured_image", post.getFeaturedImage());
        WordPress.getRestClientUtilsV1_1().post(path, params, null, listener, errorListener);
    }

    /**
     * Upload featuredImage to server
     */
    public static void uploadPostFeaturedImage(final Post post, String mediaFile,
                                               final EditPostSettingsFragment editPostSettingsFragment) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                String featuredImageURL = JSONUtil.getString(jsonObject, "URL");
                if (!featuredImageURL.isEmpty()) {
                    post.setFeaturedImage(featuredImageURL);
                    editPostSettingsFragment.setFeaturedImage(post.getFeaturedImage());
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.POSTS, volleyError);
            }
        };
        AppLog.d(AppLog.T.POSTS, "update post featured image");
        String path = "sites/" + UrlUtils.urlEncode(UrlUtils.getDomainFromUrl(WordPress.getCurrentBlog().getHomeURL()))
                + "/media/new/";
        /*
        Map<String, String[]> params = new HashMap<>();
        Log.i("media", mediaFile);
        String mediaURLs[] = new String[1];
        mediaURLs[0] = mediaFile;
        params.put("media_urls", mediaURLs);
        */
        Map<String, String> params = new HashMap<>();
        params.put("media", mediaFile);
        // not working
        WordPress.getRestClientUtilsV1_1().post(path, params, null, listener, errorListener);

    }
}
