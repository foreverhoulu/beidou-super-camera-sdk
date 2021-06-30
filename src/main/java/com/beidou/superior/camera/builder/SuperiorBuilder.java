package com.beidou.superior.camera.builder;

import android.app.Activity;
import android.hardware.Camera;

import com.beidou.superior.camera.activity.SuperiorCameraActivity;
import com.beidou.superior.camera.utils.SuperiorCacheInfo;

import java.lang.ref.WeakReference;

public class SuperiorBuilder {

    private static SuperiorBuilder instance;
    private WeakReference<Activity> mActivity;

    private SuperiorBuilder(Activity activity) {
        this.mActivity = new WeakReference<Activity>(activity);
    }

    /**
     * create Camera
     * @param activity Context
     * @return SuperiorBuilder
     */
    public static SuperiorBuilder createCamera(Activity activity) {
        SuperiorBuilder.clear();
        SuperiorCacheInfo.clean();
        instance = new SuperiorBuilder(activity);
        return instance;
    }

    /**
     * set camera to front facing
     * @return
     */
    public SuperiorBuilder setFrontFacing(){
        SuperiorSetting.CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_FRONT;
        return this;
    }

    /**
     * set camera to back facing
     * @return
     */
    public SuperiorBuilder setBackFacing(){
        SuperiorSetting.CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK;
        return this;
    }

    /**
     * Start Activity
     *
     * @param requestCode startActivityForResult requestCode
     */
    public void start(int requestCode) {
        SuperiorCameraActivity.start(this.mActivity.get(), requestCode);
    }

    /**
     * clear
     */
    private static void clear() {
        instance = null;
        SuperiorSetting.clear();
    }

}
