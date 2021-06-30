package com.beidou.superior.camera.core;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import androidx.core.app.ActivityCompat;

import com.beidou.superior.camera.builder.SuperiorSetting;
import com.beidou.superior.camera.utils.CameraThreadPool;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SuperiorCameraControl implements ICameraControl {
    public static final String TAG = "SuperiorCameraControl";
    public static final int PERMISSIONS_REQUEST_CAMERA = 800;

    private int displayOrientation = 0;
    private int cameraId = 0;
    private int flashMode;
    private AtomicBoolean takingPicture = new AtomicBoolean(false);
    private AtomicBoolean abortingScan = new AtomicBoolean(false);
    private Context context;
    private Camera camera;

    private Camera.Parameters parameters;
    private IPermissionCallback IPermissionCallback;
    private CameraNotSupportCall cameraNotSupportCall;
    private Rect previewFrame = new Rect();

    private PreviewView previewView;
    private View displayView;
    private int rotation = 0;

    private Camera.Size optSize;

    private AtomicBoolean hasNotCameraPermission = new AtomicBoolean(true);

    private final int MODEL_NOSCAN = 0;
    private final int MODEL_SCAN = 1;

    private int detectType = MODEL_NOSCAN;

    public int getCameraRotation() {
        return rotation;
    }

    public AtomicBoolean getAbortingScan() {
        return abortingScan;
    }

    @Override
    public void setDetectCallback(OnDetectPictureCallback callback) {
        detectType = MODEL_SCAN;
    }

    @Override
    public void setDisplayOrientation(@SuperiorCameraView.Orientation int displayOrientation) {
        this.displayOrientation = displayOrientation;
        switch (displayOrientation) {
            case SuperiorCameraView.ORIENTATION_PORTRAIT:
                rotation = 90;
                break;
            case SuperiorCameraView.ORIENTATION_HORIZONTAL:
                rotation = 0;
                break;
            case SuperiorCameraView.ORIENTATION_INVERT:
                rotation = 180;
                break;
            default:
                rotation = 0;
        }
        previewView.requestLayout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshPermission() {
        startPreview(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFlashMode(@FlashMode int flashMode) {
        if (this.flashMode == flashMode) {
            return;
        }
        this.flashMode = flashMode;
        updateFlashMode(flashMode);
    }

    @Override
    public int getFlashMode() {
        return flashMode;
    }

    @Override
    public void start() {
        startPreview(false);
    }

    @Override
    public void stop() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            stopPreview();
            Camera tempC = camera;
            camera = null;
            tempC.release();
            camera = null;
            buffer = null;
        }
    }

    private void stopPreview() {
        if (camera != null) {
            camera.stopPreview();
        }
    }

    @Override
    public void pause() {
        if (camera != null) {
            stopPreview();
        }
        setFlashMode(FLASH_MODE_OFF);
    }

    @Override
    public void resume() {
        takingPicture.set(false);
        if (camera == null) {
            openCamera();
        } else {
            previewView.textureView.setSurfaceTextureListener(surfaceTextureListener);
            if (previewView.textureView.isAvailable()) {
                startPreview(false);
            }
        }
    }

    @Override
    public View getDisplayView() {
        return displayView;
    }

    @Override
    public void takePicture(final OnTakePictureCallback onTakePictureCallback) {
        if (takingPicture.get()) {
            return;
        }
        switch (displayOrientation) {
            case SuperiorCameraView.ORIENTATION_PORTRAIT:
                parameters.setRotation(90);
                break;
            case SuperiorCameraView.ORIENTATION_HORIZONTAL:
                parameters.setRotation(0);
                break;
            case SuperiorCameraView.ORIENTATION_INVERT:
                parameters.setRotation(180);
                break;
        }
        try {
            optSize = getOptimalSize(false);
            parameters.setPictureSize(optSize.width, optSize.height);
            camera.setParameters(parameters);
            takingPicture.set(true);
            cancelAutoFocus();
            CameraThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        camera.takePicture(null, null, new Camera.PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data, Camera camera) {
                                startPreview(false);
                                takingPicture.set(false);
                                if (onTakePictureCallback != null) {
                                    onTakePictureCallback.onPictureTaken(data);
                                }
                            }
                        });
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (RuntimeException e) {
            e.printStackTrace();
            startPreview(false);
            takingPicture.set(false);
        }
    }


    @Override
    public void setIPermissionCallback(IPermissionCallback callback) {
        this.IPermissionCallback = callback;
    }

    @Override
    public void setCameraNotSupportCall(CameraNotSupportCall callback){
        this.cameraNotSupportCall = callback;
    }


    public SuperiorCameraControl(Context context) {
        this.context = context;
        previewView = new PreviewView(context);
        openCamera();
    }

    private void openCamera() {
        setupDisplayView();
    }

    private void setupDisplayView() {
        final TextureView textureView = new TextureView(context);
        previewView.textureView = textureView;
        previewView.setTextureView(textureView);
        displayView = previewView;
        textureView.setSurfaceTextureListener(surfaceTextureListener);
    }

    private SurfaceTexture surfaceCache;

    private byte[] buffer = null;

    private void setPreviewCallbackImpl() {
        if (buffer == null) {
            buffer = new byte[displayView.getWidth() * displayView.getHeight() * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8];
        }
        if (camera != null && detectType == MODEL_SCAN) {
            camera.addCallbackBuffer(buffer);
            camera.setPreviewCallback(previewCallback);
        }
    }

    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(final byte[] data, Camera camera) {
            //take Picture
            if(takingPicture.get()){
                return;
            }
            // scan
            if (abortingScan.get()) {
                return;
            }
            // data is ok
            if(null == data || null == parameters){
                return;
            }
            //yuv data check
            if (data.length != parameters.getPreviewSize().width * parameters.getPreviewSize().height * 1.5) {
                return;
            }
            //deal data
            camera.addCallbackBuffer(buffer);
        }
    };


    private void initCamera() {
        if(!hasNotCameraPermission.get()){
            return;
        }
        try {
            if (camera == null) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == SuperiorSetting.CAMERA_FACING) {
                        cameraId = i;
                    }
                }
                try {
                    camera = Camera.open(cameraId);
                } catch (Throwable e) {
                    //open fault
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        hasNotCameraPermission.set(false);
                        if(null != this.cameraNotSupportCall){
                            this.cameraNotSupportCall.call();
                            return;
                        }
                    }
                    startPreview(true);
                    return;
                }
            }
            if (parameters == null) {
                parameters = camera.getParameters();
                parameters.setPreviewFormat(ImageFormat.NV21);
            }
            opPreviewSize(previewView.getWidth(), previewView.getHeight());
            camera.setPreviewTexture(surfaceCache);
            setPreviewCallbackImpl();
            startPreview(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            surfaceCache = surface;
            initCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            opPreviewSize(previewView.getWidth(), previewView.getHeight());
            setPreviewCallbackImpl();
            startPreview(false);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            setPreviewCallbackImpl();
        }
    };

    // start Preview
    private void startPreview(boolean checkPermission) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (checkPermission && IPermissionCallback != null) {
                IPermissionCallback.onRequestPermission(new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            }
            return;
        }
        if (camera == null) {
            initCamera();
        } else {
            camera.startPreview();
            startAutoFocus();
        }
    }

    private void cancelAutoFocus() {
        camera.cancelAutoFocus();
        CameraThreadPool.cancelAutoFocusTimer();
    }

    private void startAutoFocus() {
        CameraThreadPool.createAutoFocusTimerTask(new Runnable() {
            @Override
            public void run() {
                synchronized (SuperiorCameraControl.this) {
                    if (camera != null && !takingPicture.get()) {
                        try {
                            camera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean success, Camera camera) {
                                }
                            });
                        } catch (Throwable e) {
                            // startPreview auto focus fail
                        }
                    }
                }
            }
        });
    }

    private void opPreviewSize(int width, @SuppressWarnings("unused") int height) {
        if (parameters != null && camera != null && width > 0) {
            optSize = getOptimalSize(true);
            parameters.setPreviewSize(optSize.width, optSize.height);
            previewView.setRatio(1.0f * optSize.width / optSize.height);

            camera.setDisplayOrientation(getSurfaceOrientation());
            stopPreview();
            try {
                camera.setParameters(parameters);
            } catch (RuntimeException e) {
                e.printStackTrace();

            }
        }
    }

    private Camera.Size getOptimalSize(boolean isPreview) {
        if(camera == null){
            return null;
        }
        List<Camera.Size> minSize = new ArrayList<>();
        List<Camera.Size> sameSize = new ArrayList<>();
        List<Camera.Size> commonSize = new ArrayList<>();
        //Picture Supported Size
        List<Camera.Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();
        //Preview Supported Size
        List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
        for(Camera.Size pictureSize : pictureSizes){
            if(previewSizes.contains(pictureSize)){
                if(pictureSize.width == pictureSize.height){
                    sameSize.add(pictureSize);
                }else if(pictureSize.width <320 || pictureSize.height< 320){
                    minSize.add(pictureSize);
                }else{
                    commonSize.add(pictureSize);
                }
            }
        }
        //optimize size
        if(commonSize.size() == 0){
            if(sameSize.size() > 0){
                commonSize.addAll(sameSize);
            }else if (minSize.size() > 0){
                commonSize.addAll(minSize);
            }
        }
        //has same size
        if(commonSize.size() == 0){
            if(isPreview){
                commonSize = previewSizes;
            }else{
                commonSize = pictureSizes;
            }
        }
        //sort
        Collections.sort(commonSize, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size o1, Camera.Size o2) {
                return o1.height > o2.height ? 1: -1;
            }
        });
        //optimize size
        int width = previewView.getWidth();
        int height = previewView.getHeight();
        List<Camera.Size> candidates = new ArrayList<>();
        for (Camera.Size size : commonSize) {
            if (size.width >= width && size.height >= height && size.width * height == size.height * width) {
                candidates.add(size);
            } else if (size.height >= width && size.width >= height && size.width * width == size.height * height) {
                candidates.add(size);
            }
        }
        if (!candidates.isEmpty()) {
            return Collections.min(candidates, sizeComparator);
        }
        //choose a optimize size
        for (Camera.Size size : commonSize) {
            if (size.width >= width && size.height >= height) {
                return size;
            }
        }
        //choose a big optimize size
        return commonSize.get(commonSize.size()-1);
    }

    private Comparator<Camera.Size> sizeComparator = new Comparator<Camera.Size>() {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            return Long.signum((long) lhs.width * lhs.height - (long) rhs.width * rhs.height);
        }
    };

    private void updateFlashMode(int flashMode) {
        switch (flashMode) {
            case FLASH_MODE_TORCH:
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                break;
            case FLASH_MODE_OFF:
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                break;
            case ICameraControl.FLASH_MODE_AUTO:
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                break;
            default:
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                break;
        }
        camera.setParameters(parameters);
    }

    private int getSurfaceOrientation() {
        @SuperiorCameraView.Orientation
        int orientation = displayOrientation;
        switch (orientation) {
            case SuperiorCameraView.ORIENTATION_PORTRAIT:
                return 90;
            case SuperiorCameraView.ORIENTATION_HORIZONTAL:
                return 0;
            case SuperiorCameraView.ORIENTATION_INVERT:
                return 180;
            default:
                return 90;
        }
    }

    private class PreviewView extends FrameLayout {

        private TextureView textureView;

        private float ratio = 0.75f;

        void setTextureView(TextureView textureView) {
            this.textureView = textureView;
            removeAllViews();
            addView(textureView);
        }

        void setRatio(float ratio) {
            this.ratio = ratio;
            requestLayout();
            relayout(getWidth(), getHeight());
        }

        public PreviewView(Context context) {
            super(context);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            relayout(w, h);
        }

        private void relayout(int w, int h) {
            int width = w;
            int height = h;
            if (w < h) {
                height = (int) (width * ratio);
            } else {
                width = (int) (height * ratio);
            }

            int l = (getWidth() - width) / 2;
            int t = (getHeight() - height) / 2;

            previewFrame.left = l;
            previewFrame.top = t;
            previewFrame.right = l + width;
            previewFrame.bottom = t + height;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            textureView.layout(previewFrame.left, previewFrame.top, previewFrame.right, previewFrame.bottom);
        }
    }

    @Override
    public Rect getPreviewFrame() {
        return previewFrame;
    }
}
