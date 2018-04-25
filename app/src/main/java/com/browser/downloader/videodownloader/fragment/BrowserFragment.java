package com.browser.downloader.videodownloader.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.browser.downloader.videodownloader.R;
import com.browser.downloader.videodownloader.activities.BookmarkActivity;
import com.browser.downloader.videodownloader.activities.HistoryActivity;
import com.browser.downloader.videodownloader.adapter.SuggestionAdapter;
import com.browser.downloader.videodownloader.data.ConfigData;
import com.browser.downloader.videodownloader.data.Video;
import com.browser.downloader.videodownloader.data.WebViewData;
import com.browser.downloader.videodownloader.databinding.FragmentBrowserBinding;
import com.browser.downloader.videodownloader.databinding.LayoutVideoDataBinding;
import com.browser.downloader.videodownloader.service.DownloadService;
import com.browser.downloader.videodownloader.service.SearchService;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;

import org.greenrobot.eventbus.EventBus;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.subjects.PublishSubject;
import vd.core.common.Constant;
import vd.core.util.AdUtil;
import vd.core.util.AppUtil;
import vd.core.util.DialogUtil;
import vd.core.util.IntentUtil;
import vd.core.util.ScriptUtil;
import vd.core.util.TimeUtil;
import vd.core.util.ViewUtil;

public class BrowserFragment extends BaseFragment {

    FragmentBrowserBinding mBinding;

    private InterstitialAd mInterstitialAd;

    private PublishSubject<String> mPublishSubject;

    private SuggestionAdapter mSuggestionAdapter;

    private ArrayList<WebViewData> mHistoryData;

    private ArrayList<WebViewData> mBookmarData;

    private Menu mMenu;

    private boolean isAdShowed = false;

    private boolean isHasFocus = false;

    private final static int ACTIVITY_HISTORY = 0;

    private final static int ACTIVITY_BOOKMARK = 1;

    public final static String RESULT_URL = "RESULT_URL";

    private LinkStatus mLinkStatus;

    private enum LinkStatus {
        SUPPORTED, GENERAL, UNSUPPORTED
    }

    public static BrowserFragment getInstance() {
        return new BrowserFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_browser, container, false);
        ButterKnife.bind(this, mBinding.getRoot());
        setSupportActionBar(mBinding.toolbar);
        setHasOptionsMenu(true);

        initUI();

//        // Show ad banner
//        AdUtil.showBanner(this, mBinding.layoutBanner);

        // Load ad interstitial
        loadInterstitialAd();

        return mBinding.getRoot();
    }

    private void loadInterstitialAd() {
        // Check show ad
        ConfigData configData = mPreferenceManager.getConfigData();
        boolean isShowAd = configData == null ? true : configData.isShowAdBrowser();
        if (isShowAd) {
            DialogUtil.showSimpleProgressDialog(getContext());
            mInterstitialAd = new InterstitialAd(getContext());
            AdUtil.showInterstitialAd(mInterstitialAd, new AdListener() {
                @Override
                public void onAdLoaded() {
                    DialogUtil.closeProgressDialog();
                    super.onAdLoaded();
                }

                @Override
                public void onAdFailedToLoad(int i) {
                    DialogUtil.closeProgressDialog();
                    super.onAdFailedToLoad(i);
                }
            });
        }
    }

    private void showInterstitlaAd() {
        if (!isAdShowed && mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            isAdShowed = true;
            mInterstitialAd.show();
            // google analytics
            trackEvent(getString(R.string.app_name), getString(R.string.action_show_ad_browser), "");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_browser, menu);
        mMenu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_add_bookmark) {
            if (mBinding.webview != null) {
                if (isBookmarkLink(mBinding.webview)) {
                    removeBookmark(mBinding.webview);
                } else {
                    saveWebViewBookmark(mBinding.webview);
                }
                updateBookmarkMenu(mBinding.webview);
            }

        } else if (item.getItemId() == R.id.menu_bookmark) {
            startActivityForResult(new Intent(getContext(), BookmarkActivity.class), ACTIVITY_BOOKMARK);
            getActivity().overridePendingTransition(R.anim.enter_from_right, 0);

        } else if (item.getItemId() == R.id.menu_history) {
            startActivityForResult(new Intent(getContext(), HistoryActivity.class), ACTIVITY_HISTORY);
            getActivity().overridePendingTransition(R.anim.enter_from_right, 0);

        } else if (item.getItemId() == R.id.menu_share) {
            if (mBinding.webview != null && !TextUtils.isEmpty(mBinding.webview.getUrl())) {
                IntentUtil.shareLink(getContext(), mBinding.webview.getUrl());
                // google analytics
                trackEvent(getString(R.string.app_name), getString(R.string.action_share_link), "");
            }

        } else if (item.getItemId() == R.id.menu_copy_link) {
            if (mBinding.webview != null && !TextUtils.isEmpty(mBinding.webview.getUrl())) {
                AppUtil.copyClipboard(getContext(), mBinding.webview.getUrl());
                Toast.makeText(getContext(), "Copied link", Toast.LENGTH_SHORT).show();
                // google analytics
                trackEvent(getString(R.string.app_name), getString(R.string.action_copy_link), "");
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ACTIVITY_HISTORY:
                if (resultCode == Activity.RESULT_OK) {
                    String url = data.getStringExtra(RESULT_URL);
                    mBinding.webview.loadUrl(url);
                }
                break;
            case ACTIVITY_BOOKMARK:
                if (resultCode == Activity.RESULT_OK) {
                    String url = data.getStringExtra(RESULT_URL);
                    mBinding.webview.loadUrl(url);
                }
                break;
        }
    }

    private void initUI() {
        // Grant permission
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        ConfigData configData = mPreferenceManager.getConfigData();
        mBinding.layoutSocial.layoutMostVisited.setVisibility(configData != null && configData.isShowAllPages() ? View.VISIBLE : View.GONE);

        mBinding.webview.getSettings().setJavaScriptEnabled(true);
        mBinding.webview.addJavascriptInterface(this, "browser");
        mBinding.webview.setWebViewClient(webViewClient);
        mBinding.webview.setWebChromeClient(webChromeClient);

        mActivity.setIOnBackPressed(() -> {
            if (mBinding.webview.canGoBack()) {
                mBinding.webview.goBack();
                return true;
            } else {
                return false;
            }
        });

        mBinding.etSearch.setOnKeyListener((v, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        // clear focus & hide keyboard
                        focusSearchView(false);
                        // load data
                        loadWebView();
                        // google analytics
                        String content = mBinding.etSearch.getText().toString().trim();
                        trackEvent(getString(R.string.app_name), getString(R.string.action_search), content);
                        return true;
                    }
                    return false;
                }
        );

        mBinding.etSearch.setOnClickListener(view -> focusSearchView(true));

        mBinding.etSearch.setOnFocusChangeListener((view, isHasFocus) -> {
            this.isHasFocus = isHasFocus;
            if (isHasFocus) {
                mBinding.ivCloseRefresh.setImageResource(R.drawable.ic_close_gray_24dp);
            } else {
                mBinding.ivCloseRefresh.setImageResource(R.drawable.ic_refresh_gray_24dp);
//                String data = mBinding.webview.getUrl();
//                if (data != null && data.length() > 0) {
//                    mBinding.etSearch.setText(data);
//                } else {
//                    mBinding.etSearch.setText("");
//                }
            }
        });

        mBinding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                mPublishSubject.onNext(editable.toString());
            }
        });

        mPublishSubject = PublishSubject.create();
        mPublishSubject.debounce(300, TimeUnit.MILLISECONDS).subscribe(searchValue -> {
            if (searchValue.length() > 0 && !searchValue.startsWith("http://") && !searchValue.startsWith("https://")) {
                getActivity().runOnUiThread(() -> {
                    new SearchService(suggestions -> {
                        showSuggestion(suggestions);
                    }).execute(String.format(Constant.SUGGESTION_URL, searchValue));
                });
            }
        });
    }

    private void focusSearchView(boolean isFocus) {
        mBinding.etSearch.setFocusable(isFocus);
        mBinding.etSearch.setFocusableInTouchMode(isFocus);
        if (isFocus) {
            ViewUtil.showSoftKeyboard(getContext(), mBinding.etSearch);
        } else {
            ViewUtil.hideSoftKeyboard(getContext(), mBinding.etSearch);
        }
    }

    private void showSuggestion(List<String> suggestions) {
        mSuggestionAdapter = new SuggestionAdapter(getContext(), R.layout.item_suggestion, suggestions);
        mBinding.etSearch.setAdapter(mSuggestionAdapter);
        mBinding.etSearch.showDropDown();
        mBinding.etSearch.setOnItemClickListener((parent, view, position, id) -> {
            // clear focus & hide keyboard
            focusSearchView(false);
            // Search keyword
            loadWebView();
            // google analytics
            String content = mBinding.etSearch.getText().toString().trim();
            trackEvent(getString(R.string.app_name), getString(R.string.action_search_suggestion), content);
        });
    }

    WebChromeClient webChromeClient = new WebChromeClient() {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            mBinding.progressBar.setProgress(newProgress);
            super.onProgressChanged(view, newProgress);
        }
    };

    WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (url.toLowerCase().startsWith("https://youtu.be/") || url.toLowerCase().contains("youtube.com")) {
                mBinding.webview.setVisibility(View.GONE);
                mBinding.tvNotSupport.setVisibility(View.VISIBLE);
                mBinding.layoutSocial.layoutRoot.setVisibility(View.GONE);
                return;
            }

            mBinding.webview.setVisibility(View.VISIBLE);
            mBinding.tvNotSupport.setVisibility(View.GONE);
            mBinding.etSearch.setText(url);
            mBinding.fab.setVisibility(View.VISIBLE);
            mBinding.progressBar.setVisibility(View.VISIBLE);
            mBinding.layoutSocial.layoutRoot.setVisibility(View.GONE);

            checkLinkStatus(url);
            updateBookmarkMenu(view);
            super.onPageStarted(view, url, favicon);
        }


        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            mBinding.etSearch.setText(url);
            view.loadUrl(url);
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            try {
                if (url.contains("facebook.com")) {
                    view.loadUrl(ScriptUtil.FACEBOOK_SCRIPT);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            mBinding.etSearch.setText(url);
            mBinding.progressBar.setVisibility(View.GONE);
            checkLinkStatus(url);
            saveWebViewHistory(view);
            updateBookmarkMenu(view);
            super.onPageFinished(view, url);
        }
    };

    private void saveWebViewHistory(WebView webView) {
        WebViewData webViewData = new WebViewData();
        String url = webView.getUrl();
        if (!TextUtils.isEmpty(url)) {
            webViewData.setUrl(url);
            String title = webView.getTitle();
            if (TextUtils.isEmpty(title)) {
                try {
                    title = url.split("/")[2];
                } catch (Exception e) {
                    e.printStackTrace();
                    title = url;
                }
            }
            webViewData.setTitle(title);
            getWebViewHistory().add(0, webViewData);
            mPreferenceManager.setHistory(getWebViewHistory());
        }
    }

    private ArrayList<WebViewData> getWebViewHistory() {
        if (mHistoryData == null) {
            mHistoryData = mPreferenceManager.getHistory();
        }
        return mHistoryData;
    }

    private void saveWebViewBookmark(WebView webView) {
        WebViewData webViewData = new WebViewData();
        String url = webView.getUrl();
        if (!TextUtils.isEmpty(url)) {
            webViewData.setUrl(url);
            String title = webView.getTitle();
            if (TextUtils.isEmpty(title)) {
                try {
                    title = url.split("/")[2];
                } catch (Exception e) {
                    e.printStackTrace();
                    title = url;
                }
            }
            webViewData.setTitle(title);
            getWebViewBookmark().add(0, webViewData);
            mPreferenceManager.setBookmark(getWebViewBookmark());
        }
    }

    private ArrayList<WebViewData> getWebViewBookmark() {
        if (mBookmarData == null) {
            mBookmarData = mPreferenceManager.getBookmark();
        }
        return mBookmarData;
    }

    private boolean isBookmarkLink(WebView webView) {
        for (WebViewData webViewData : getWebViewBookmark()) {
            if (webViewData.getUrl().equals(webView.getUrl())) {
                return true;
            }
        }

        return false;
    }

    private void removeBookmark(WebView webView) {
        for (WebViewData webViewData : getWebViewBookmark()) {
            if (webViewData.getUrl().equals(webView.getUrl())) {
                getWebViewBookmark().remove(webViewData);
                mPreferenceManager.setBookmark(getWebViewBookmark());
                return;
            }
        }
    }

    private void updateBookmarkMenu(WebView webView) {
        mMenu.getItem(0).getSubMenu().getItem(0).setIcon(isBookmarkLink(webView)
                ? R.drawable.ic_star_yellow_24dp : R.drawable.ic_star_border_gray_24dp);
        mMenu.getItem(0).getSubMenu().getItem(0).setTitle(isBookmarkLink(webView)
                ? "Remove bookmark" : "Add bookmark");
    }

    private void showVideoDataDialog(Video video) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        LayoutVideoDataBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.layout_video_data, null, false);

        binding.ivThumbnail.setImageURI(Uri.parse(video.getThumbnail()));
        binding.tvName.setText(video.getFileName());
        if (video.getDuration() != 0) {
            binding.tvTime.setVisibility(View.VISIBLE);
            binding.tvTime.setText(TimeUtil.convertMilliSecondsToTimer(video.getDuration() * 1000));
        } else {
            binding.tvTime.setVisibility(View.GONE);
        }

        binding.tvCancel.setOnClickListener(view -> bottomSheetDialog.dismiss());

        binding.tvOk.setOnClickListener(view -> {
            bottomSheetDialog.dismiss();
//            AppUtil.downloadVideo(getContext(), video);
            EventBus.getDefault().post(video);
            showInterstitlaAd();
        });

        bottomSheetDialog.setContentView(binding.getRoot());
        bottomSheetDialog.show();
    }

    private void checkLinkStatus(String url) {
        ConfigData configData = mPreferenceManager.getConfigData();
        if (configData != null) {
            // General sites
            if (configData.getPagesGeneral() != null) {
                for (String link : configData.getPagesGeneral()) {
                    if (url.startsWith(link) || url.equals(link)) {
                        mLinkStatus = LinkStatus.GENERAL;
                        disableDownloadBtn();
                        return;
                    }
                }
            }
            // General sites with specific link
            if (configData.getPagesGeneral1() != null) {
                for (String link : configData.getPagesGeneral1()) {
                    if (url.equals(link)) {
                        mLinkStatus = LinkStatus.GENERAL;
                        disableDownloadBtn();
                        return;
                    }
                }
            }
            // Unsupported sites
            if (configData.getPagesUnsupported() != null) {
                for (String link : configData.getPagesUnsupported()) {
                    if (url.startsWith(link)) {
                        mLinkStatus = LinkStatus.UNSUPPORTED;
                        disableDownloadBtn();
                        return;
                    }
                }
            }
        }

        // Other sites
        mLinkStatus = LinkStatus.SUPPORTED;
        enableDownloadBtn();
    }

    private void disableDownloadBtn() {
        mBinding.fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.color_gray_1)));
    }

    private void enableDownloadBtn() {
        mBinding.fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.colorAccent)));
    }

    private void enableDownloadBtnAndShake() {
        enableDownloadBtn();
        shakeButton(mBinding.fab);
    }

    private void shakeButton(View view) {
        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.shake_btn_anim);
        anim.setDuration(50L);
        view.startAnimation(anim);
    }

    @JavascriptInterface
    public void getVideoData(String link) {
        getActivity().runOnUiThread(() -> {
            try {
                String url = URLDecoder.decode(link, "UTF-8");
                if (!TextUtils.isEmpty(url) && url.startsWith("http")) {
                    Video video = new Video(System.currentTimeMillis() + ".mp4", url);
                    DialogUtil.showAlertDialog(getContext(), video.getFileName(), getString(R.string.message_download_video),
                            (dialogInterface, i) -> {
                                showInterstitlaAd();
                                AppUtil.downloadVideo(getContext(), video);
                            });
                    // google analytics
                    trackEvent(getString(R.string.app_name), getString(R.string.event_get_link_facebook), url);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void loadWebView() {
        String content = mBinding.etSearch.getText().toString().trim();
        if (content.length() > 0) {
            if (content.startsWith("http://") || content.startsWith("https://")) {
                mBinding.webview.loadUrl(content);
            } else if (Patterns.WEB_URL.matcher(content).matches()) {
                mBinding.webview.loadUrl("http://" + content);
                mBinding.etSearch.setText("http://" + content);
            } else {
                mBinding.webview.loadUrl(String.format(Constant.SEARCH_URL, content));
                mBinding.etSearch.setText(String.format(Constant.SEARCH_URL, content));
            }
        }
    }

    @OnClick(R.id.btn_facebook)
    public void clickFacebook() {
        mBinding.etSearch.setText(mBinding.layoutSocial.tvFacebook.getText().toString());
        loadWebView();
        // google analytics
        trackEvent(getString(R.string.app_name), getString(R.string.action_open_facebook), "");
    }

    @OnClick(R.id.btn_twitter)
    public void clickTwitter() {
        mBinding.etSearch.setText(mBinding.layoutSocial.tvTwitter.getText().toString());
        loadWebView();
        // google analytics
        trackEvent(getString(R.string.app_name), getString(R.string.action_open_twitter), "");
    }

    @OnClick(R.id.btn_instagram)
    public void clickInstagram() {
        mBinding.etSearch.setText(mBinding.layoutSocial.tvInstagram.getText().toString());
        loadWebView();
        // google analytics
        trackEvent(getString(R.string.app_name), getString(R.string.action_open_instagram), "");
    }

    @OnClick(R.id.btn_dailymotion)
    public void clickDailymotion() {
        mBinding.etSearch.setText(mBinding.layoutSocial.tvDailymotion.getText().toString());
        loadWebView();
        // google analytics
        trackEvent(getString(R.string.app_name), getString(R.string.action_open_dailymotion), "");
    }

    @OnClick(R.id.btn_vimeo)
    public void clickVimeo() {
        mBinding.etSearch.setText(mBinding.layoutSocial.tvVimeo.getText().toString());
        loadWebView();
        // google analytics
        trackEvent(getString(R.string.app_name), getString(R.string.action_open_vimeo), "");
    }

    @OnClick(R.id.iv_close_refresh)
    public void clickCloseOrRefresh() {
        if (mBinding.etSearch.getText().toString().trim().length() > 0) {
            if (isHasFocus) {
                mBinding.etSearch.setText("");
                mBinding.tvNotSupport.setVisibility(View.GONE);
                mBinding.webview.setVisibility(View.GONE);
                mBinding.fab.setVisibility(View.GONE);
                mBinding.layoutSocial.layoutRoot.setVisibility(View.VISIBLE);
                showInterstitlaAd();
            } else {
                mBinding.webview.reload();
            }
        }
    }

    @OnClick(R.id.fab)
    public void downloadVideo() {

        if (mLinkStatus != null) {
            if (mLinkStatus == LinkStatus.GENERAL) {
                DialogUtil.showAlertDialog(getContext(), getString(R.string.error_video_page));
                return;
            }
            if (mLinkStatus == LinkStatus.UNSUPPORTED) {
                DialogUtil.showAlertDialog(getContext(), getString(R.string.error_unsupported_site));
                return;
            }
        }

        String data = mBinding.webview.getUrl();
        if (data == null || data.length() == 0 || !Patterns.WEB_URL.matcher(data).matches()) {
            DialogUtil.showAlertDialog(getContext(), getString(R.string.error_valid_link));
            return;
        }

        if (data.contains("facebook.com")) {
            DialogUtil.showAlertDialog(getContext(), getString(R.string.error_facebook));
            return;
        }

        if (data.toLowerCase().startsWith("https://youtu.be/") || data.toLowerCase().contains("youtube.com")) {
            Toast.makeText(getContext(), "Unsupported site!", Toast.LENGTH_LONG).show();
            // google analytics
            trackEvent(getString(R.string.app_name), getString(R.string.event_unsupported_site), data);
            return;
        }

        new DownloadService(getContext(), video -> showVideoDataDialog(video)).execute(data);
    }
}
