package com.beidou.superior.camera;

import android.app.Activity;
import android.graphics.Bitmap;
import com.beidou.superior.camera.builder.SuperiorBuilder;
import com.beidou.superior.camera.utils.SuperiorCacheInfo;

import java.io.Serializable;

public class SuperiorCamera {
    /**SDK version*/
    public final static String SDK_VERSION = "lite-0.1.1";
    /**Activity return KEY*/
    public static String KEY_CAMERA_RESULT = "Superior.Camera.Result";

    /**
     * create Camera
     * @param activity Context
     * @return AlbumBuilder
     */
    public static SuperiorBuilder createCamera(Activity activity) {
        return SuperiorBuilder.createCamera(activity);
    }

    /**
     * Result
     */
    public static class Result implements Serializable {
        /**
         * code
         * @return
         */
        public ResultCode getCode() {
            return SuperiorCacheInfo.code;
        }

        /**
         * msg
         * @return
         */
        public String getMessage() {
            return SuperiorCacheInfo.message;
        }

        /**
         * get Image
         * @return
         */
        public Bitmap getImage(){
            if(SuperiorCacheInfo.image != null && !SuperiorCacheInfo.image.isRecycled()){
                return SuperiorCacheInfo.image.copy(Bitmap.Config.ARGB_8888, true);
            }else{
                return null;
            }
        }

        /**
         * clean
         */
        public void clean(){
            SuperiorCacheInfo.clean();
        }
    }

    /**Result Code*/
    public enum ResultCode {
        SUCCESS(200),
        FAILURE(400),                   //FAILURE
        FAILURE_MANUAL_EXIT(401),       //MANUAL EXIT
        FAILURE_CAMERA_NOT_SUPPORT(402);//CAMERA NOT SUPPORT

        private int code;
        ResultCode(int code) {
            this.code = code;
        }
        public int getCode() {
            return code;
        }
    }
}
