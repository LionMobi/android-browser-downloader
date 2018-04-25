package vd.core.util;

import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.browser.downloader.videodownloader.data.ConfigData;
import com.browser.downloader.videodownloader.data.Video;

import java.io.File;

import vd.core.common.Constant;
import vd.core.common.PreferencesManager;

public class AppUtil {

    public static boolean isDownloadVideo = false;

    public static void downloadVideo(Context context, Video video) {
        isDownloadVideo = true;
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(video.getUrl()));

        File localFile = FileUtil.getFolderDir();
        if (!localFile.exists() && !localFile.mkdirs()) return;

        request.setDestinationInExternalPublicDir(FileUtil.FOLDER_NAME, video.getFileName());
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        dm.enqueue(request);
    }

    public static String buildUrl(Context context, String data) {
        ConfigData configData = PreferencesManager.getInstance(context).getConfigData();
        String server = configData != null && !TextUtils.isEmpty(configData.getParserServer()) ? configData.getParserServer() : Constant.PARSER_SERVER;
        return String.format(server, data);
    }

    public static void copyClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied link", text);
        clipboard.setPrimaryClip(clip);
    }

}
