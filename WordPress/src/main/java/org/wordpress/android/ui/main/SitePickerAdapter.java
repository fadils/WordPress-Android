package org.wordpress.android.ui.main;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

class SitePickerAdapter extends RecyclerView.Adapter<SitePickerAdapter.SiteViewHolder> {

    interface OnSiteClickListener {
        void onSiteClick(SiteRecord site);
    }

    interface OnMultiSelectListener {
        void onMultiSelectEnabled();
        void onSelectedCountChanged(int numSelected);
    }

    private final int mTextColorNormal;
    private final int mTextColorHidden;
    private final Drawable mSelectedItemBackground;
    private static int mBlavatarSz;

    private SiteList mSites = new SiteList();
    private final LayoutInflater mInflater;
    private final HashSet<Integer> mSelectedPositions = new HashSet<>();

    private boolean mIsMultiSelectEnabled;
    private boolean mShowHiddenSites = false;
    private boolean mCanEnableMultiSelect = false;

    private OnSiteClickListener mSiteSelectedListener;
    private OnMultiSelectListener mMultiSelectListener;

    static class SiteViewHolder extends RecyclerView.ViewHolder {
        private final ViewGroup layoutContainer;
        private final TextView txtTitle;
        private final TextView txtDomain;
        private final WPNetworkImageView imgBlavatar;
        private Boolean isSiteHidden;

        public SiteViewHolder(View view) {
            super(view);
            layoutContainer = (ViewGroup) view.findViewById(R.id.layout_container);
            txtTitle = (TextView) view.findViewById(R.id.text_title);
            txtDomain = (TextView) view.findViewById(R.id.text_domain);
            imgBlavatar = (WPNetworkImageView) view.findViewById(R.id.image_blavatar);
            isSiteHidden = null;
        }
    }

    public SitePickerAdapter(Context context) {
        super();
        setHasStableIds(true);
        mInflater = LayoutInflater.from(context);
        mTextColorNormal = context.getResources().getColor(R.color.grey_dark);
        mTextColorHidden = context.getResources().getColor(R.color.grey);
        mBlavatarSz = context.getResources().getDimensionPixelSize(R.dimen.blavatar_sz);
        mSelectedItemBackground = new ColorDrawable(context.getResources().getColor(R.color.translucent_grey_lighten_20));
        loadSites();
    }

    @Override
    public int getItemCount() {
        return mSites.size();
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).localId;
    }

    private SiteRecord getItem(int position) {
        return mSites.get(position);
    }

    void setOnMultiSelectedListener(OnMultiSelectListener listener) {
        mMultiSelectListener = listener;
    }

    void setOnSiteClickListener(OnSiteClickListener listener) {
        mSiteSelectedListener = listener;
    }

    @Override
    public SiteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.site_picker_card, parent, false);
        return new SiteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SiteViewHolder holder, final int position) {
        SiteRecord site = getItem(position);

        holder.txtTitle.setText(site.blogName);
        holder.txtDomain.setText(site.hostName);
        holder.imgBlavatar.setErrorImageResId(site.isDotCom ? R.drawable.blavatar_placeholder_com : R.drawable.blavatar_placeholder_org);
        holder.imgBlavatar.setImageUrl(site.blavatarUrl, WPNetworkImageView.ImageType.BLAVATAR);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSiteSelectedListener != null) {
                    mSiteSelectedListener.onSiteClick(getItem(position));
                }
                if (mIsMultiSelectEnabled) {
                    toggleSelection(position);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // enable multi-select on long press
                if (!mIsMultiSelectEnabled && mCanEnableMultiSelect) {
                    if (mMultiSelectListener != null) {
                        mMultiSelectListener.onMultiSelectEnabled();
                    }
                    setEnableMultiSelect(true);
                    setItemSelected(position, true);
                }
                return true;
            }
        });

        if (mIsMultiSelectEnabled) {
            holder.layoutContainer.setBackgroundDrawable(isItemSelected(position) ? mSelectedItemBackground : null);
        }

        // different styling for visible/hidden sites
        if (holder.isSiteHidden == null || holder.isSiteHidden != site.isHidden) {
            holder.isSiteHidden = site.isHidden;
            holder.txtTitle.setTextColor(site.isHidden ? mTextColorHidden : mTextColorNormal);
            holder.txtTitle.setTypeface(holder.txtTitle.getTypeface(), site.isHidden ? Typeface.NORMAL : Typeface.BOLD);
            holder.imgBlavatar.setAlpha(site.isHidden ? 0.5f : 1f);
        }
    }

    private boolean isValidPosition(int position) {
        return (position >= 0 && position < mSites.size());
    }

    void setEnableMultiSelect(boolean enable) {
        if (enable == mIsMultiSelectEnabled) return;

        mIsMultiSelectEnabled = enable;
        mSelectedPositions.clear();
        notifyDataSetChanged();
    }

    int getSelectionCount() {
        return mSelectedPositions.size();
    }

    private void toggleSelection(int position) {
        setItemSelected(position, !isItemSelected(position));
    }

    private boolean isItemSelected(int position) {
        return mSelectedPositions.contains(position);
    }

    private void setItemSelected(int position, boolean isSelected) {
        if (isItemSelected(position) == isSelected) {
            return;
        }

        if (isSelected) {
            mSelectedPositions.add(position);
        } else {
            mSelectedPositions.remove(position);
        }

        notifyItemChanged(position);

        if (mMultiSelectListener != null) {
            mMultiSelectListener.onSelectedCountChanged(getSelectionCount());
        }
    }

    SiteList getSelectedSites() {
        SiteList sites = new SiteList();
        if (!mIsMultiSelectEnabled) {
            return sites;
        }

        for (Integer position : mSelectedPositions) {
            if (isValidPosition(position))
                sites.add(mSites.get(position));
        }

        return sites;
    }

    void loadSites() {
        if (mIsTaskRunning) {
            AppLog.w(AppLog.T.UTILS, "site picker > already loading sites");
        } else {
            new LoadSitesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /*
     * AsyncTasks which loads sites from database and populates the adapter
     */
    private boolean mIsTaskRunning;
    private class LoadSitesTask extends AsyncTask<Void, Void, SiteList> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsTaskRunning = true;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mIsTaskRunning = false;
        }

        @Override
        protected SiteList doInBackground(Void... params) {
            List<Map<String, Object>> blogs;
            String[] extraFields = {"isHidden", "dotcomFlag"};

            // add wp.com blogs
            if (mShowHiddenSites) {
                blogs = WordPress.wpDB.getBlogsBy("dotcomFlag=1", extraFields);
            } else {
                blogs = WordPress.wpDB.getBlogsBy("isHidden=0 AND dotcomFlag=1", extraFields);
            }

            // add self-hosted
            blogs.addAll(WordPress.wpDB.getBlogsBy("dotcomFlag=0", extraFields));

            SiteList sites = new SiteList(blogs);
            Collections.sort(sites, SiteComparator);

            return sites;
        }

        @Override
        protected void onPostExecute(SiteList sites) {
            if (mSites == null || !mSites.isSameList(sites)) {
                mSites = sites;
                notifyDataSetChanged();
            }
            mIsTaskRunning = false;
        }
    }

    /**
     * SiteRecord is a simplified version of the full account (blog) record
     */
    static class SiteRecord {
        final int localId;
        final String blogId;
        final String blogName;
        final String hostName;
        final String url;
        final String blavatarUrl;
        final boolean isHidden;
        final boolean isDotCom;

        SiteRecord(Map<String, Object> account) {
            localId = MapUtils.getMapInt(account, "id");
            blogId = MapUtils.getMapStr(account, "blogId");
            blogName = BlogUtils.getBlogNameFromAccountMap(account);
            hostName = BlogUtils.getHostNameFromAccountMap(account);
            url = MapUtils.getMapStr(account, "url");
            blavatarUrl = GravatarUtils.blavatarFromUrl(url, mBlavatarSz);
            isHidden = MapUtils.getMapBool(account, "isHidden");
            isDotCom = MapUtils.getMapBool(account, "dotcomFlag");
        }

        String getBlogNameOrHostName() {
            if (TextUtils.isEmpty(blogName)) {
                return hostName;
            }
            return blogName;
        }
    }

    private static class SiteList extends ArrayList<SiteRecord> {
        SiteList() { }
        SiteList(List<Map<String, Object>> accounts) {
            if (accounts != null) {
                for (Map<String, Object> account : accounts) {
                    add(new SiteRecord(account));
                }
            }
        }

        boolean isSameList(SiteList sites) {
            if (sites == null || sites.size() != this.size()) {
                return false;
            }
            for (SiteRecord site: sites) {
                if (!this.containsSite(site)) {
                    return false;
                }
            }
            return true;
        }

        boolean containsSite(SiteRecord site) {
            if (site != null && site.blogId != null) {
                for (SiteRecord thisSite : this) {
                    if (site.blogId.equals(thisSite.blogId)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /*
     * sorts sites based on their name/host and visibility - hidden blogs are sorted
     * below visible ones
     */
    private static final Comparator<SiteRecord> SiteComparator = new Comparator<SiteRecord>() {
        public int compare(SiteRecord site1, SiteRecord site2) {
            if (site1.isHidden != site2.isHidden) {
                return (site1.isHidden ? 1 : -1);
            } else {
                return site1.getBlogNameOrHostName().compareToIgnoreCase(site2.getBlogNameOrHostName());
            }
        }
    };
}
