package org.wordpress.android.ui.posts;

import android.view.LayoutInflater;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.wordpress.android.R;
import org.wordpress.android.ui.posts.actions.PostActions;
import org.wordpress.android.widgets.WPViewPager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PostActionsTest {
    EditPostActivity mActivity;
    EditPostSettingsFragment mSettingsFragment;
    View mRootView;
    WPViewPager mContainer;
    LayoutInflater mInflater;

    @Before
    public void setUp() throws Exception {
        mActivity = Robolectric.setupActivity(EditPostActivity.class);
        mSettingsFragment = mActivity.getEditPostSettingsFragment();
        mActivity.onAttachFragment(mSettingsFragment);
        mContainer = (WPViewPager) mActivity.findViewById(R.id.pager);
        mInflater = mActivity.getLayoutInflater();
        mRootView = mInflater.inflate(R.layout.fragment_edit_post_settings, mContainer, false);
    }

    @Test
    public void fragments_isNotNull() throws Exception {
        assertNotNull(mSettingsFragment);
    }

    @Test
    public void rootView_isNotNull() throws Exception {
        assertNotNull(mRootView);
    }

    @Test
    @Config(shadows={ShadowPost.class})
    public void removeFeaturedImageInSettings_shouldChangePost() throws Exception {
        ShadowPost shadowPost = new ShadowPost();
        assertNotNull(shadowPost);

        String validURL = "valid-image-URL";
        shadowPost.getPost().setFeaturedImage(validURL);
        PostActions.removeFeaturedImageInSettings(shadowPost.getPost());

        assertTrue(shadowPost.getPost().getFeaturedImage().isEmpty());
    }
}