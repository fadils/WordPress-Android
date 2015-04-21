package org.wordpress.android.ui.main;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;

import com.simperium.client.Bucket;

import org.wordpress.android.GCMIntentService;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Note;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.util.AccountHelper;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.SlidingTabLayout;
import org.wordpress.android.widgets.WPMainViewPager;

import de.greenrobot.event.EventBus;

/**
 * Main activity which hosts sites, reader, me and notifications tabs
 */
public class WPMainActivity extends Activity
    implements ViewPager.OnPageChangeListener,
        SlidingTabLayout.SingleTabClickListener,
        Bucket.Listener<Note> {
    private WPMainViewPager mViewPager;
    private SlidingTabLayout mTabs;
    private WPMainTabAdapter mTabAdapter;

    public static final String ARG_OPENED_FROM_PUSH = "opened_from_push";

    /*
     * tab fragments implement this if their contents can be scrolled, called when user
     * requests to scroll to the top
     */
    public interface OnScrollToTopListener {
        void onScrollToTop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setStatusBarColor();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewPager = (WPMainViewPager) findViewById(R.id.viewpager_main);
        mTabAdapter = new WPMainTabAdapter(getFragmentManager());
        mViewPager.setAdapter(mTabAdapter);

        mTabs = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mTabs.setSelectedIndicatorColors(getResources().getColor(R.color.tab_indicator));
        mTabs.setDistributeEvenly(true);
        Integer icons[] = {R.drawable.main_tab_sites,
                           R.drawable.main_tab_reader,
                           R.drawable.main_tab_me,
                           R.drawable.main_tab_notifications};
        mTabs.setCustomTabView(R.layout.tab_icon, R.id.tab_icon, R.id.tab_badge, icons);
        mTabs.setViewPager(mViewPager);
        mTabs.setOnSingleTabClickListener(this);

        // page change listener must be set on the tab layout rather than the ViewPager
        mTabs.setOnPageChangeListener(this);

        if (savedInstanceState == null) {
            if (AccountHelper.isSignedIn()) {
                // open note detail if activity called from a push, otherwise return to the tab
                // that was showing last time
                boolean openedFromPush = (getIntent() != null && getIntent().getBooleanExtra(ARG_OPENED_FROM_PUSH,
                        false));
                if (openedFromPush) {
                    getIntent().putExtra(ARG_OPENED_FROM_PUSH, false);
                    launchWithNoteId();
                } else {
                    int position = AppPrefs.getMainTabIndex();
                    if (mTabAdapter.isValidPosition(position) && position != mViewPager.getCurrentItem()) {
                        mViewPager.setCurrentItem(position);
                    }
                }
            } else {
                ActivityLauncher.showSignInForResult(this);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar_tint));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        AppLog.i(AppLog.T.NOTIFS, "Main activity new intent");
        if (intent.hasExtra(NotificationsListFragment.NOTE_ID_EXTRA)) {
            launchWithNoteId();
        }
    }

    /*
     * called when app is launched from a push notification, switches to the notification tab
     * and opens the desired note detail
     */
    private void launchWithNoteId() {
        if (isFinishing() || getIntent() == null) return;

        mViewPager.setCurrentItem(WPMainTabAdapter.TAB_NOTIFS);

        String noteId = getIntent().getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA);
        boolean shouldShowKeyboard = getIntent().getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, false);

        if (!TextUtils.isEmpty(noteId)) {
            NotificationsListFragment.openNote(this, noteId, shouldShowKeyboard);
            GCMIntentService.clearNotificationsMap();
        }
    }

    @Override
    public void onPageSelected(int position) {
        // remember the index of this page
        AppPrefs.setMainTabIndex(position);

        switch (position) {
            case WPMainTabAdapter.TAB_NOTIFS:
                if (getNotificationListFragment() != null) {
                    getNotificationListFragment().updateLastSeenTime();
                    mTabs.setBadge(WPMainTabAdapter.TAB_NOTIFS, false);
                }
                break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        int position = mViewPager.getCurrentItem();
        if (position == WPMainTabAdapter.TAB_READER) {
            ReaderPostListFragment fragment = getReaderListFragment();
            if (fragment != null) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING && fragment.isFragmentToolbarShowing()) {
                    fragment.showFragmentToolbar(false);
                } else if (state == ViewPager.SCROLL_STATE_SETTLING && !fragment.isFragmentToolbarShowing()) {
                    fragment.showFragmentToolbar(true);
                }
            }
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // nop
    }

    /*
     * user tapped a tab above the viewPager - detect when the active tab is clicked and scroll
     * the fragment to the top if available
     */
    @Override
    public void onTabClick(View view, int position) {
        if (position == mViewPager.getCurrentItem()) {
            Fragment fragment = mTabAdapter.getFragment(position);
            if (fragment instanceof OnScrollToTopListener) {
                ((OnScrollToTopListener) fragment).onScrollToTop();
            }
        }
    }

    @Override
    protected void onPause() {
        if (SimperiumUtils.getNotesBucket() != null) {
            SimperiumUtils.getNotesBucket().removeListener(this);
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start listening to Simperium Note bucket
        if (SimperiumUtils.getNotesBucket() != null) {
            SimperiumUtils.getNotesBucket().addListener(this);
        }

        checkNoteBadge();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCodes.READER_SUBS:
            case RequestCodes.READER_REBLOG:
                ReaderPostListFragment readerFragment = getReaderListFragment();
                // TODO:
                if (readerFragment != null) {
                    //readerFragment.handleActivityResult(requestCode, resultCode, data);
                }
                break;
            case RequestCodes.ADD_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    WordPress.registerForCloudMessaging(this);
                } else {
                    finish();
                }
                break;
            case RequestCodes.REAUTHENTICATE:
                if (resultCode == RESULT_CANCELED) {
                    ActivityLauncher.showSignInForResult(this);
                } else {
                    WordPress.registerForCloudMessaging(this);
                }
                break;
            case RequestCodes.SETTINGS:
                // user returned from settings
                if (AccountHelper.isSignedIn()) {
                    WordPress.registerForCloudMessaging(this);
                } else {
                    ActivityLauncher.showSignInForResult(this);
                }
                break;
            case RequestCodes.SITE_PICKER:
                if (resultCode == RESULT_OK && data != null) {
                    int localId = data.getIntExtra(SitePickerActivity.KEY_LOCAL_ID, 0);
                    //String blogId = data.getStringExtra(SitePickerActivity.KEY_BLOG_ID);

                    // when a new blog is picked, set it to the current blog
                    Blog blog = WordPress.setCurrentBlog(localId);
                    WordPress.wpDB.updateLastBlogId(localId);

                    MySiteFragment mySiteFragment = getMySiteFragment();
                    if (mySiteFragment != null) {
                        mySiteFragment.setBlog(blog);
                    }
                }
                break;
        }
    }

    /*
     * returns the reader list fragment from the reader tab
     */
    private ReaderPostListFragment getReaderListFragment() {
        Fragment fragment = mTabAdapter.getFragment(WPMainTabAdapter.TAB_READER);
        if (fragment != null && fragment instanceof ReaderPostListFragment) {
            return (ReaderPostListFragment) fragment;
        }
        return null;
    }

    /*
     * returns the notification list fragment from the notification tab
     */
    private NotificationsListFragment getNotificationListFragment() {
        Fragment fragment = mTabAdapter.getFragment(WPMainTabAdapter.TAB_NOTIFS);
        if (fragment != null && fragment instanceof NotificationsListFragment) {
            return (NotificationsListFragment) fragment;
        }
        return null;
    }

    /*
     * returns the my site fragment from the sites tab
     */
    private MySiteFragment getMySiteFragment() {
        Fragment fragment = mTabAdapter.getFragment(WPMainTabAdapter.TAB_SITES);
        if (fragment != null && fragment instanceof MySiteFragment) {
            return (MySiteFragment) fragment;
        }
        return null;
    }

    /*
     * badges the notifications tab depending on whether there are unread notes
     */
    private boolean mIsCheckingNoteBadge;
    private void checkNoteBadge() {
        if (mIsCheckingNoteBadge) {
            AppLog.v(AppLog.T.NOTIFS, "already checking note badge");
            return;
        } else if (isViewingNotificationsTab()) {
            // Don't show the badge if the notifications tab is active
            if (mTabs.isBadged(WPMainTabAdapter.TAB_NOTIFS)) {
                mTabs.setBadge(WPMainTabAdapter.TAB_NOTIFS, false);
            }

            return;
        }

        mIsCheckingNoteBadge = true;
        new Thread() {
            @Override
            public void run() {
                final boolean hasUnreadNotes = SimperiumUtils.hasUnreadNotes();
                boolean isBadged = mTabs.isBadged(WPMainTabAdapter.TAB_NOTIFS);
                if (hasUnreadNotes != isBadged) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTabs.setBadge(WPMainTabAdapter.TAB_NOTIFS, hasUnreadNotes);
                            mIsCheckingNoteBadge = false;
                        }
                    });
                } else {
                    mIsCheckingNoteBadge = false;
                }
            }
        }.start();
    }

    private boolean isViewingNotificationsTab() {
        return mViewPager.getCurrentItem() == WPMainTabAdapter.TAB_NOTIFS;
    }

    // Events

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.UserSignedOut event) {
        ActivityLauncher.showSignInForResult(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.InvalidCredentialsDetected event) {
        AuthenticationDialogUtils.showAuthErrorView(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.RestApiUnauthorized event) {
        AuthenticationDialogUtils.showAuthErrorView(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.TwoFactorAuthenticationDetected event) {
        AuthenticationDialogUtils.showAuthErrorView(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.InvalidSslCertificateDetected event) {
        SelfSignedSSLCertsManager.askForSslTrust(this, null);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.LoginLimitDetected event) {
        ToastUtils.showToast(this, R.string.limit_reached, ToastUtils.Duration.LONG);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.BlogListChanged event) {
        // TODO: reload blog list if showing
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NotificationEvents.NotificationsChanged event) {
        checkNoteBadge();
    }

    /*
     * Simperium Note bucket listeners
     */
    @Override
    public void onNetworkChange(Bucket<Note> noteBucket, Bucket.ChangeType changeType, String s) {
        if (changeType == Bucket.ChangeType.INSERT || changeType == Bucket.ChangeType.MODIFY) {
            checkNoteBadge();
        }
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onDeleteObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onSaveObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }
}
