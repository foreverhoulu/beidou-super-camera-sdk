package com.beidou.superior.camera.core;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.IntDef;
import java.util.concurrent.atomic.AtomicBoolean;

public interface ICameraControl {

    /**
     * FLASH MODE OFF {@link #setFlashMode(int)}
     */
    int FLASH_MODE_OFF = 0;
    /**
     * FLASH MODE ON {@link #setFlashMode(int)}
     */
    int FLASH_MODE_TORCH = 1;
    /**
     * FLASH MODE AUTO {@link #setFlashMode(int)}
     */
    int FLASH_MODE_AUTO = 2;

    @IntDef({FLASH_MODE_TORCH, FLASH_MODE_OFF, FLASH_MODE_AUTO})
    @interface FlashMode {

    }

    /**
     * Take Picture Callback。
     */
    interface OnTakePictureCallback {
        void onPictureTaken(byte[] data);
    }

    /**
     * Detect Callback。
     */
    void setDetectCallback(OnDetectPictureCallback callback);

    /**
     * Detect Picture Callback
     */
    interface OnDetectPictureCallback {
        int onDetect(byte[] data, int rotation);
    }

    /**
     * start。
     */
    void start();

    /**
     * stop
     */
    void stop();

    /**
     * pause
     */
    void pause();

    /**
     * resume
     */
    void resume();

    /**
     * camera Preview View。
     * @return Preview View
     */
    View getDisplayView();

    /**
     * camera Preview frame
     * @return Preview frame;
     */
    Rect getPreviewFrame();

    /**
     * take Picture
     * @param callback take Picture callback
     */
    void takePicture(OnTakePictureCallback callback);

    /**
     * set Permission Callback
     * @param callback Permission Callback
     */
    void setIPermissionCallback(IPermissionCallback callback);

    /**
     *
     * @param callback
     */
    void setCameraNotSupportCall(CameraNotSupportCall callback);

    /**
     * set display Orientation
     * @param displayOrientation  display Orientation
     */
    void setDisplayOrientation(@SuperiorCameraView.Orientation int displayOrientation);

    /**
     * refresh Permission
     */
    void refreshPermission();

    /**
     * get AbortingScan flag
     */
    AtomicBoolean getAbortingScan();

    /**
     *  set FLASH MODE
     * @param flashMode {@link #FLASH_MODE_TORCH,#FLASH_MODE_OFF,#FLASH_MODE_AUTO}
     */
    void setFlashMode(@FlashMode int flashMode);

    /**
     * FLASH MODE State
     * @return State
     */
    @FlashMode
    int getFlashMode();
}
