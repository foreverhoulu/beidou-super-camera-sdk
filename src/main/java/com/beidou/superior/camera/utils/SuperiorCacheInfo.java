package com.beidou.superior.camera.utils;

import android.graphics.Bitmap;

import com.beidou.superior.camera.SuperiorCamera;

public class SuperiorCacheInfo {
    //code
    public static SuperiorCamera.ResultCode code;
    //message
    public static String message;
    //image
    public static Bitmap image;

    public static void clean(){
        code = null;
        message = null;
        if(image != null){
            if(!image.isRecycled()){
                image.recycle();
            }
            image = null;
        }
    }
}
