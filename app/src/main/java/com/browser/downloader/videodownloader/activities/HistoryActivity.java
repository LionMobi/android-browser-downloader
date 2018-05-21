package com.browser.downloader.videodownloader.activities;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import com.browser.downloader.videodownloader.R;
import com.browser.downloader.videodownloader.adapter.HistoryAdapter;
import com.browser.downloader.videodownloader.databinding.ActivityHistoryBinding;
import com.browser.downloader.videodownloader.fragment.BrowserFragment;
import com.google.android.gms.ads.AdSize;

import butterknife.ButterKnife;
import vd.core.util.AdUtil;

public class HistoryActivity extends BaseActivity {

    ActivityHistoryBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_history);
        ButterKnife.bind(this);
        initUI();
    }

    private void initUI() {
        mBinding.toolbar.setNavigationOnClickListener(view -> onBackPressed());

        mBinding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
        HistoryAdapter historyAdapter = new HistoryAdapter(mPreferenceManager.getHistory(),
                history -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(BrowserFragment.RESULT_URL, history.getUrl());
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                });
        mBinding.rvHistory.setAdapter(historyAdapter);

        if (mPreferenceManager.getHistory().isEmpty()) {
            mBinding.tvNoHistory.setVisibility(View.VISIBLE);
        }

        // Show ad banner
        AdUtil.loadBanner(this, mBinding.layoutBanner, AdSize.SMART_BANNER, true);

        // google analytics
        trackEvent(getString(R.string.app_name), getString(R.string.screen_history), mPreferenceManager.getHistory().size() + "");
    }

    @Override
    public void onResume() {
        trackView(getString(R.string.screen_history));
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
