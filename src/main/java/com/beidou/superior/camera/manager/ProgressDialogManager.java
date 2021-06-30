package com.beidou.superior.camera.manager;

import android.app.ProgressDialog;
import android.content.Context;

import com.beidou.superior.camera.R;


public class ProgressDialogManager {
    private Context context;
    private ProgressDialog progressDialog;

    public ProgressDialogManager(Context context) {
        this.context = context;
    }

    public void buildProgressDialog() {
        try {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(context);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            }
            progressDialog.setMessage(context.getString(R.string.bd_superior_camera_dialog_in_progress));
            progressDialog.setCancelable(false);
            if (!progressDialog.isShowing())
                progressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelProgressDialog() {
        if (progressDialog != null) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }
}
