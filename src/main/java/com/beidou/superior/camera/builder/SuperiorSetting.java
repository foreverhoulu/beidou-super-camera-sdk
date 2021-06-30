package com.beidou.superior.camera.builder;

import android.hardware.Camera;

public class SuperiorSetting {
    //Camera Type
    public static int CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_FRONT;

    //clear
    public static void clear() {
        CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_FRONT;
    }
}
