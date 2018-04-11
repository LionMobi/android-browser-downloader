package vd.core.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;

import com.browser.downloader.videodownloader.R;
import com.browser.downloader.videodownloader.activities.BaseActivity;

import vd.core.common.PreferencesManager;


public class DialogUtil {

    private static Dialog simpleProgressDialog = null;

    public static void showSimpleProgressDialog(Context context) {
        if (simpleProgressDialog != null) {
            closeProgressDialog();
        }

        if (context != null) {
            simpleProgressDialog = new Dialog(context);
            simpleProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            simpleProgressDialog.setContentView(R.layout.dialog_progress_simple);
            setDialogOpacity(simpleProgressDialog, Color.WHITE, 0);
            simpleProgressDialog.setCancelable(false);
            simpleProgressDialog.show();
        }
    }

    public static void closeProgressDialog() {

        if (simpleProgressDialog != null) {
            try {
                simpleProgressDialog.cancel();
                simpleProgressDialog = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isProgressShowing() {
        return (simpleProgressDialog != null && simpleProgressDialog.isShowing());
    }

    public static void setDialogOpacity(Dialog dialog, int bgColor, int alpha) {
        ColorDrawable bgDrawable = new ColorDrawable(bgColor);
        bgDrawable.setAlpha(alpha);
        dialog.getWindow().setBackgroundDrawable(bgDrawable);
    }

    public static void showRateDialog(Context context) {
        new AlertDialog.Builder(context).setTitle(context.getString(R.string.app_name))
                .setMessage(context.getString(R.string.rate_app))
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    PreferencesManager.getInstance(context).setRateApp(true);
                    IntentUtil.openGooglePlay(context, context.getPackageName());
                    // google analytics
                    ((BaseActivity) context).trackEvent(context.getString(R.string.app_name), context.getString(R.string.action_rate_us_exit_app), "");
                })
                .setNegativeButton("EXIT", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    ((Activity) context).finish();
                })
                .show();
    }

    public static void showAlertDialog(Context context, String title, String message,
                                       OnClickListener onClickListener) {
        AlertDialog.Builder arAlertDialog = new AlertDialog.Builder(context);
        arAlertDialog.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, onClickListener)
                .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void showAlertDialog(Context context, String message) {
        AlertDialog.Builder arAlertDialog = new AlertDialog.Builder(context);
        arAlertDialog.setTitle(context.getString(R.string.app_name))
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
