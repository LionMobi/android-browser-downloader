package com.browser.downloader.videodownloader.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.browser.downloader.videodownloader.R;
import com.browser.downloader.videodownloader.data.ProgressInfo;
import com.browser.downloader.videodownloader.databinding.ItemProgressBinding;

import java.util.ArrayList;

public class ProgressAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<ProgressInfo> mProgressInfos;

    public ProgressAdapter(ArrayList<ProgressInfo> progressInfos) {
        this.mProgressInfos = progressInfos;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemProgressBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                R.layout.item_progress, parent, false);
        return new ProgressViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ProgressInfo progressInfo = mProgressInfos.get(position);
        ItemProgressBinding binding = ((ProgressViewHolder) holder).binding;

        binding.progressBar.setProgress(progressInfo.isDownloaded() ? 100 : progressInfo.getProgress());
        binding.tvProgress.setText(progressInfo.isDownloaded() ? "Done" : progressInfo.getProgressSize());
    }

    @Override
    public int getItemCount() {
        return mProgressInfos.size();
    }

    class ProgressViewHolder extends RecyclerView.ViewHolder {

        ItemProgressBinding binding;

        public ProgressViewHolder(ItemProgressBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
