package com.browser.downloader.videodownloader.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.browser.downloader.videodownloader.fragment.BrowserFragment;
import com.browser.downloader.videodownloader.fragment.ProgressFragment;
import com.browser.downloader.videodownloader.fragment.SettingsFragment;
import com.browser.downloader.videodownloader.fragment.VideoFragment;

public class HomeAdapter extends FragmentPagerAdapter {

    public HomeAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return BrowserFragment.getInstance();
        } else if (position == 1) {
            return ProgressFragment.getInstance();
        } else if (position == 2) {
            return VideoFragment.getInstance();
        } else if (position == 3) {
            return SettingsFragment.getInstance();
        } else {
            return SettingsFragment.getInstance();
        }
    }

    @Override
    public int getCount() {
        return 5;
    }

}

