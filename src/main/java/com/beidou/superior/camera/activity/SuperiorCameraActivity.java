package com.beidou.superior.camera.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.beidou.superior.camera.R;
import com.beidou.superior.camera.SuperiorCamera;
import com.beidou.superior.camera.core.CameraNotSupportCall;
import com.beidou.superior.camera.core.IPermissionCallback;
import com.beidou.superior.camera.core.SuperiorCameraLayout;
import com.beidou.superior.camera.core.SuperiorCameraView;
import com.beidou.superior.camera.core.SuperiorMaskView;
import com.beidou.superior.camera.manager.MediaPlayerManager;
import com.beidou.superior.camera.manager.ProgressDialogManager;
import com.beidou.superior.camera.utils.CameraThreadPool;
import com.beidou.superior.camera.utils.SuperiorCacheInfo;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * camera SDk Activity
 */
public class SuperiorCameraActivity extends Activity implements View.OnClickListener {
    //TAG
    private static final String TAG = SuperiorCameraActivity.class.getSimpleName();
    //cont
    private static final int PERMISSIONS_REQUEST_CAMERA = 800;
    //lock
    final ReentrantReadWriteLock nativeModelLock = new ReentrantReadWriteLock();
    //view
    private ImageView lightButton;
    private SuperiorCameraView cameraView;
    private ImageView takePhotoBtn;
    private ImageView album_button;
    private SuperiorCameraLayout takePictureContainer;
    private SuperiorCameraView.OnTakePictureCallback takePictureCallback;
    //Manager
    private MediaPlayerManager mediaPlayerManager;
    private ProgressDialogManager progressDialogManager;

    //Double Click Check
    public static long startTime = 0;
    public static boolean doubleClick() {
        long now = System.currentTimeMillis();
        if (now - startTime < 600) {
            return true;
        }
        startTime = now;
        return false;
    }

    /**
     * start SDK
     * @param activity
     * @param requestCode
     */
    public static void start(Activity activity, int requestCode) {
        if (doubleClick()) return;
        Intent intent = new Intent(activity, SuperiorCameraActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * onClick take_photo_button
     * @param v View
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.take_photo_button) {
            cameraView.CAMERA_TYPE = cameraView.SHOUT_CAMERA;
            mediaPlayerManager.playerMedia();
            progressDialogManager.buildProgressDialog();
            cameraView.takePicture(takePictureCallback);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bd_superior_camera_main_activity);
        initView();
        initParams();
        callBackManager();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
        cameraView.RESULT_STATUS = -1;
        cameraView.status = -1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraThreadPool.cancelAutoFocusTimer();
        mediaPlayerManager.stopMedia();

    }

    @Override
    public void onBackPressed() {
        Log.e(TAG, "onBackPressed FAILURE_MANUAL_EXIT");
        setActivityResult(SuperiorCamera.ResultCode.FAILURE_MANUAL_EXIT, this.getString(R.string.bd_superior_camera_failed_tip_manually_quit), null);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setOrientation(newConfig);
    }

    /***
     * Apply Request Permissions Result
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cameraView.getCameraControl().refreshPermission();
            } else {
                Toast.makeText(getApplicationContext(), R.string.bd_superior_camera_permission_required, Toast.LENGTH_LONG).show();
                this.deviceNotSupportCall();
            }
        }
    }

    /**
     * Init View
     * @return  Activity
     */
    private SuperiorCameraActivity initView() {
        takePictureContainer =findViewById(R.id.take_picture_container);
        album_button = findViewById(R.id.album_button);
        cameraView = findViewById(R.id.camera_view);
        lightButton = findViewById(R.id.light_button);
        takePhotoBtn = findViewById(R.id.take_photo_button);
        lightButton.setOnClickListener(this);
        takePhotoBtn.setOnClickListener(this);
        album_button.setOnClickListener(this);
        cameraView.getCameraControl().setIPermissionCallback(new IPermissionCallback() {
            @Override
            public boolean onRequestPermission(String[] permissions, int requestCode) {
                ActivityCompat.requestPermissions(SuperiorCameraActivity.this, permissions, requestCode);
                return false;
            }
        });
        cameraView.getCameraControl().setCameraNotSupportCall(new CameraNotSupportCall() {
            @Override
            public void call() {
                Toast.makeText(getApplicationContext(), R.string.bd_superior_camera_permission_required, Toast.LENGTH_LONG).show();
                SuperiorCameraActivity.this.deviceNotSupportCall();
            }
        });
        return this;
    }

    /**
     * Init Params
     * @return  Activity
     */
    private SuperiorCameraActivity initParams() {
        setOrientation(getResources().getConfiguration());
        int maskType = SuperiorMaskView.MASK_TYPE_FILLET_SQUARE;
        cameraView.setEnableScan(true);
        cameraView.setMaskType(maskType, this);
        mediaPlayerManager = new MediaPlayerManager(this, R.raw.bd_superior_camera_shout);
        progressDialogManager = new ProgressDialogManager(this);
        return this;
    }

    /**
     * Device Not Support
     */
    private void deviceNotSupportCall(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                setActivityResult(SuperiorCamera.ResultCode.FAILURE_CAMERA_NOT_SUPPORT, getString(R.string.bd_superior_camera_permission_required), null);
            }
        }).start();
    }


    /**
     * init callBack Manager
     * @return  Activity
     */
    private SuperiorCameraActivity callBackManager() {
        //Take picture callback
        takePictureCallback = new SuperiorCameraView.OnTakePictureCallback() {
            @Override
            public void onPictureTaken(final Bitmap bitmap) {
                try {
                    nativeModelLock.readLock().lock();
                    setActivityResult(SuperiorCamera.ResultCode.SUCCESS, getString(R.string.bd_superior_camera_take_photo_success), bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    nativeModelLock.readLock().unlock();
                }
            }
        };
        return this;
    }

    /**
     * set orientation info
     * @param newConfig
     */
    private void setOrientation(Configuration newConfig) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int orientation;
        int cameraViewOrientation = SuperiorCameraView.ORIENTATION_PORTRAIT;
        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                cameraViewOrientation = SuperiorCameraView.ORIENTATION_PORTRAIT;
                orientation = SuperiorCameraLayout.ORIENTATION_PORTRAIT;
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                orientation = SuperiorCameraLayout.ORIENTATION_HORIZONTAL;
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                    cameraViewOrientation = SuperiorCameraView.ORIENTATION_HORIZONTAL;
                } else {
                    cameraViewOrientation = SuperiorCameraView.ORIENTATION_INVERT;
                }
                break;
            default:
                orientation = SuperiorCameraLayout.ORIENTATION_PORTRAIT;
                cameraView.setOrientation(SuperiorCameraView.ORIENTATION_PORTRAIT);
                break;
        }
        takePictureContainer.setOrientation(orientation);
        cameraView.setOrientation(cameraViewOrientation);
    }

    /**
     * Set Activity Result
     * @param code
     * @param message
     * @param image
     */
    private void setActivityResult(SuperiorCamera.ResultCode code, String message, Bitmap image){
        //deal return
        if(code == SuperiorCamera.ResultCode.SUCCESS && image != null && !image.isRecycled()){
            //image is ok return SUCCESS
            SuperiorCacheInfo.code = code;
            SuperiorCacheInfo.message = message;
            Bitmap compress = Bitmap.createBitmap(image);
            SuperiorCacheInfo.image = compress;
            SuperiorCameraActivity.this.onFinally(true);
        }else if(code == SuperiorCamera.ResultCode.SUCCESS){
            // image is bad return FAILURE
            SuperiorCacheInfo.code = SuperiorCamera.ResultCode.FAILURE;
            SuperiorCacheInfo.message = getString(R.string.bd_superior_camera_take_photo_failure);
            SuperiorCacheInfo.image = null;
        } else{
            //image is bad return code and message
            SuperiorCacheInfo.code = code;
            SuperiorCacheInfo.message = message;
            if(image != null && !image.isRecycled()){
                SuperiorCacheInfo.image = Bitmap.createBitmap(image);
            }else{
                SuperiorCacheInfo.image = null;
            }
            this.onFinally(false);
        }
    }

    private void onFinally(boolean isSuccess){
        //set result
        Intent intentRes = new Intent();
        intentRes.putExtra(SuperiorCamera.KEY_CAMERA_RESULT, new SuperiorCamera.Result());
        setResult(RESULT_OK, intentRes);
        //back activity
        progressDialogManager.cancelProgressDialog();
        SuperiorCameraActivity.this.finish();
    }

}
