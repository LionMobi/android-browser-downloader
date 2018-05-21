package com.browser.downloader.videodownloader.activities;

import android.app.Dialog;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;

import com.airpush.injector.internal.IAirPushAdListener;
import com.airpush.injector.internal.IAirPushPreparedAd;
import com.airpush.injector.internal.ads.AirPushInterstitial;
import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.browser.downloader.videodownloader.AppApplication;
import com.browser.downloader.videodownloader.BuildConfig;
import com.browser.downloader.videodownloader.R;
import com.browser.downloader.videodownloader.adapter.HomeAdapter;
import com.browser.downloader.videodownloader.callback.DialogListener;
import com.browser.downloader.videodownloader.data.AdType;
import com.browser.downloader.videodownloader.data.ConfigData;
import com.browser.downloader.videodownloader.data.Video;
import com.browser.downloader.videodownloader.databinding.ActivityMainBinding;
import com.browser.downloader.videodownloader.dialog.RatingDialog;
import com.browser.downloader.videodownloader.dialog.UpdateDialog;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.InterstitialAd;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.ButterKnife;
import vd.core.util.AdUtil;
import vd.core.util.AppUtil;
import vd.core.util.DialogUtil;
import vd.core.util.FileUtil;
import vd.core.util.IntentUtil;

public class MainActivity extends BaseActivity {

    ActivityMainBinding mBinding;

    private int mPagePosition = 0;

    private InterstitialAd mInterstitialAd;

    private AppLovinAd mAppLovinAd;

    private IAirPushPreparedAd mIAirPushPreparedAd;

    private boolean isGetLinkSuccess = false;

    private boolean isAdShowed = false;

    private int numberActionShowAd = 0;

    private int totalActionShowAd = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        ButterKnife.bind(this);
        initUI();

        // Show ad banner
        AdUtil.loadBanner(this, mBinding.layoutBanner, AdSize.BANNER, true);

        // Load ad interstitial
        loadInterstitialAd();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initUI() {
        HomeAdapter adapter = new HomeAdapter(getSupportFragmentManager());
        mBinding.viewPager.setAdapter(adapter);
        mBinding.viewPager.setOffscreenPageLimit(5);

        mBinding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                showInterstitlaAd();
                mPagePosition = position;
                mBinding.bottomBar.setDefaultTabPosition(position);
                // google analytics
                String category = getString(position == 0 ? R.string.screen_browser
                        : position == 1 ? R.string.screen_progress : position == 2 ? R.string.screen_video
                        : position == 3 ? R.string.screen_online : R.string.screen_settings);
                String action = (position == 1 ? (mPreferenceManager.getProgress().size() + "")
                        : position == 2 ? (FileUtil.getListFiles().size() + "")
                        : position == 3 ? (mPreferenceManager.getSavedVideos().size() + "") : "");
                trackEvent(getString(R.string.app_name), category, action);
                trackView(category);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        mBinding.bottomBar.setOnTabSelectListener(tabId -> {
            if (tabId == R.id.tab_browser) {
                mBinding.viewPager.setCurrentItem(0, true);
            } else if (tabId == R.id.tab_progress) {
                mBinding.viewPager.setCurrentItem(1, true);
            } else if (tabId == R.id.tab_video) {
                mBinding.viewPager.setCurrentItem(2, true);
                mPreferenceManager.setTabVideoBadge(0);
                mBinding.bottomBar.getTabWithId(tabId).removeBadge();
            } else if (tabId == R.id.tab_online) {
                mBinding.viewPager.setCurrentItem(3, true);
                mPreferenceManager.setTabOnlineBadge(0);
                mBinding.bottomBar.getTabWithId(tabId).removeBadge();
            } else {
                mBinding.viewPager.setCurrentItem(4, true);
            }
        });

        // show badge in progress tab
        showProgressBadge();

        // google analytics
        trackEvent(getString(R.string.app_name), getString(R.string.screen_browser), "");
        trackView(getString(R.string.screen_browser));

        // Check retention
        trackEvent(getString(R.string.app_name), getString(R.string.action_check_user), AppUtil.getRetentionTime(this));

        // Update dialog
        showUpdateDialog();
    }

    private void showUpdateDialog() {

        ConfigData configData = mPreferenceManager.getConfigData();
        if (configData != null && configData.isUpdateApp() && !TextUtils.isEmpty(configData.getAppVersion())
                && !configData.getAppVersion().equals(BuildConfig.VERSION_NAME)) {
            UpdateDialog.getDialog(this, configData.getAppVersion(), view -> {
                IntentUtil.openGooglePlay(MainActivity.this, getPackageName());
                // google analytics
                trackEvent(getString(R.string.app_name), getString(R.string.action_update), BuildConfig.VERSION_NAME);
            }).show();
        }
    }

    @Subscribe
    public void onDownloadVideo(Video video) {
        // show badge in progress tab
        showProgressBadge();

        // show badge in video tab
        if (video.isDownloadCompleted()) {
            mPreferenceManager.setTabVideoBadge(mPreferenceManager.getTabVideoBadge() + 1);
            mBinding.bottomBar.getTabWithId(R.id.tab_video).setBadgeCount(mPreferenceManager.getTabVideoBadge());
        }
    }

    private void showProgressBadge() {
        new Handler().postDelayed(() -> {
            int progressBadge = mPreferenceManager.getProgress().size();
            if (progressBadge > 0) {
                mBinding.bottomBar.getTabWithId(R.id.tab_progress).setBadgeCount(progressBadge);
            } else {
                mBinding.bottomBar.getTabWithId(R.id.tab_progress).removeBadge();
            }
        }, 1000);
    }

    public void showOnlineTabBadge() {
        mPreferenceManager.setTabOnlineBadge(mPreferenceManager.getTabOnlineBadge() + 1);
        mBinding.bottomBar.getTabWithId(R.id.tab_online).setBadgeCount(mPreferenceManager.getTabOnlineBadge());
    }

    @Override
    public void onBackPressed() {
        if (mPagePosition != 0) {
            mBinding.viewPager.setCurrentItem(0, true);
            return;
        }

        if ((isGetLinkSuccess || !mPreferenceManager.isFirstTime()) && !mPreferenceManager.isRateApp()) {
            RatingDialog.getDialog(this, new DialogListener() {
                @Override
                public void onPositiveButton(Dialog dialog) {
                    dialog.dismiss();
                    mPreferenceManager.setRateApp(true);
                    IntentUtil.openGooglePlay(MainActivity.this, getPackageName());
                    // google analytics
                    trackEvent(getString(R.string.app_name), getString(R.string.action_rate_us_exit_app), "");
                    finish();
                }

                @Override
                public void onNegativeButton(Dialog dialog) {
                    dialog.dismiss();
                    finish();
                }
            }).show();
        } else {
            DialogUtil.showAlertDialog(this, getString(R.string.app_name), getString(R.string.message_exit),
                    (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        finish();
                    });
        }

        // set first time data
        mPreferenceManager.setFirstTime(false);
    }

    private void loadInterstitialAd() {
        // Get config
        ConfigData configData = mPreferenceManager.getConfigData();

        // Get total actions to show ad
        totalActionShowAd = configData == null ? totalActionShowAd : configData.getTotalActionShowAd();

        // Check show ad
        boolean isShowAd = configData == null ? true : configData.isShowAdApp();

        // Check ad type
        int adType = configData == null ? AdType.ADMOB.getValue() : configData.getShowAdAppType();

        // Show ad
        if (isShowAd) {
            if (adType == AdType.ADMOB.getValue()) {
                // Admob type
                loadInterstitialAdmob();
            } else if (adType == AdType.APPLOVIN.getValue()) {
                // AppLovin type
                loadInterstitialAppLovin();
            } else if (adType == AdType.AIRPUSH.getValue()) {
                // AirPush type
                loadInterstitialAirPush();
            } else {
                // Default is admob type
                loadInterstitialAdmob();
            }
        }
    }

    private void loadInterstitialAdmob() {
        mInterstitialAd = new InterstitialAd(this);
        AdUtil.loadInterstitialAd(mInterstitialAd, new AdListener() {
            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                // Load admob failed -> load AirPush
                AirPushInterstitial airPushInterstitial = new AirPushInterstitial(MainActivity.this);
                airPushInterstitial.setEventsListener(new IAirPushAdListener() {
                    @Override
                    public void onLoad(IAirPushPreparedAd ad) {
                        mIAirPushPreparedAd = ad;
                    }

                    @Override
                    public void onError(Exception e) {
                    }
                });
                airPushInterstitial.load();
            }
        });
    }

    private void loadInterstitialAppLovin() {
        AppApplication.getAppLovinSdk().getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, new AppLovinAdLoadListener() {
            @Override
            public void adReceived(AppLovinAd ad) {
                mAppLovinAd = ad;
            }

            @Override
            public void failedToReceiveAd(int errorCode) {
                // Load AppLovin failed -> load Admob
                runOnUiThread(() -> {
                    mInterstitialAd = new InterstitialAd(MainActivity.this);
                    AdUtil.loadInterstitialAd(mInterstitialAd, null);
                });
            }
        });
    }

    private void loadInterstitialAirPush() {
        AirPushInterstitial airPushInterstitial = new AirPushInterstitial(this);
        airPushInterstitial.setEventsListener(new IAirPushAdListener() {
            @Override
            public void onLoad(IAirPushPreparedAd ad) {
                mIAirPushPreparedAd = ad;
            }

            @Override
            public void onError(Exception e) {
                // Load AirPush failed -> load Admob
                mInterstitialAd = new InterstitialAd(MainActivity.this);
                AdUtil.loadInterstitialAd(mInterstitialAd, null);
            }
        });
        airPushInterstitial.load();
    }

    public void showInterstitlaAd() {
        numberActionShowAd++;
        if (!isAdShowed && (numberActionShowAd % totalActionShowAd == 0)) {
            if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
                isAdShowed = true;
                // google analytics
                trackEvent(getString(R.string.app_name), getString(R.string.action_show_ad_app), "Admob");
            } else if (mAppLovinAd != null) {
                AppLovinInterstitialAdDialog interstitialAd = AppLovinInterstitialAd.create(AppApplication.getAppLovinSdk(), getApplicationContext());
                interstitialAd.showAndRender(mAppLovinAd);
                isAdShowed = true;
                // google analytics
                trackEvent(getString(R.string.app_name), getString(R.string.action_show_ad_app), "AppLovin");
            } else if (mIAirPushPreparedAd != null) {
                mIAirPushPreparedAd.show();
                isAdShowed = true;
                // google analytics
                trackEvent(getString(R.string.app_name), getString(R.string.action_show_ad_app), "AirPush");
            }
        }
    }

    public boolean isGetLinkSuccess() {
        return isGetLinkSuccess;
    }

    public void setGetLinkSuccess(boolean getLinkSuccess) {
        isGetLinkSuccess = getLinkSuccess;
    }
}