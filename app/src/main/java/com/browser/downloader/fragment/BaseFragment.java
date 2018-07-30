package com.browser.downloader.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.browser.downloader.activities.MainActivity;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import com.browser.core.common.Constant;
import com.browser.core.common.PreferencesManager;

public abstract class BaseFragment extends Fragment {

    protected PreferencesManager mPreferenceManager;

    protected Tracker mTracker;

    protected MainActivity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = (MainActivity) getActivity();

        mPreferenceManager = PreferencesManager.getInstance(mActivity);

        GoogleAnalytics analytics = GoogleAnalytics.getInstance(mActivity);
        mTracker = analytics.newTracker(Constant.UA_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void trackView(String screenName) {
        mTracker.setScreenName(screenName);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void trackEvent(String category, String action, String label) {
        mTracker.send(new HitBuilders.EventBuilder().setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());
    }

    protected void setSupportActionBar(Toolbar toolbar) {
        ((AppCompatActivity) this.getActivity()).setSupportActionBar(toolbar);
    }
}
