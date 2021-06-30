package com.beidou.superior.camera.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IntDef;

import com.beidou.superior.camera.R;
import com.beidou.superior.camera.builder.SuperiorSetting;
import com.beidou.superior.camera.utils.CameraThreadPool;
import com.beidou.superior.camera.utils.DimensionUtil;
import com.beidou.superior.camera.utils.SuperiorImageUtil;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Camera View ctrl
 */
public class SuperiorCameraView extends FrameLayout {

    private int maskType;
    public int status = -1;
    public int RESULT_STATUS = -1;
    public final int SCAN_FREE = -1;
    public final int SCAN_SUCCESS = 0;
    public String SHOUT_CAMERA = "SHOUT_OCR";
    public String ALBUM_ORC = "ALBUM_OCR";
    public String AUTO_ORC = "AUTO_ORC";
    public String CAMERA_TYPE = AUTO_ORC;
    public long showMinInterval = 1000L;
    public AtomicLong lastShowTime = new AtomicLong(-1);

    /**
     * on Picture Taken Callback
     */
    public interface OnTakePictureCallback {
        void onPictureTaken(Bitmap bitmap);

    }

    /**
     * PORTRAIT {@link #setOrientation(int)}
     */
    public static final int ORIENTATION_PORTRAIT = 0;
    /**
     * HORIZONTAL {@link #setOrientation(int)}
     */
    public static final int ORIENTATION_HORIZONTAL = 90;
    /**
     * INVERT {@link #setOrientation(int)}
     */
    public static final int ORIENTATION_INVERT = 270;

    public static final int NATIVE_AUTH_INIT_SUCCESS = 0;


    public void setInitNativeStatus(int initNativeStatus) {
        this.initNativeStatus = initNativeStatus;
    }

    private int initNativeStatus = NATIVE_AUTH_INIT_SUCCESS;

    @IntDef({ORIENTATION_PORTRAIT, ORIENTATION_HORIZONTAL, ORIENTATION_INVERT})
    public @interface Orientation {

    }

    private CameraViewTakePictureCallback cameraViewTakePictureCallback = new CameraViewTakePictureCallback();

    private ICameraControl cameraControl;

    private View displayView;
    private SuperiorMaskView maskView;
    private ImageView hintView;
    private TextView hintViewText;
    private LinearLayout hintViewTextWrapper;
    private boolean isEnableScan;
    public void setEnableScan(boolean enableScan) {
        isEnableScan = enableScan;
    }
    Handler uiHandler = new Handler(Looper.getMainLooper());

    public ICameraControl getCameraControl() {
        return cameraControl;
    }

    public void setOrientation(@Orientation int orientation) {
        cameraControl.setDisplayOrientation(orientation);
    }

    public SuperiorCameraView(Context context) {
        super(context);
        init();
    }

    public SuperiorCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SuperiorCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void start() {
        cameraControl.start();
        setKeepScreenOn(true);
    }

    public void stop() {
        cameraControl.stop();
        setKeepScreenOn(false);
    }

    public void takePicture(final OnTakePictureCallback callback) {
        cameraViewTakePictureCallback.callback = callback;
        cameraControl.takePicture(cameraViewTakePictureCallback);
    }

    private OnTakePictureCallback autoPictureCallback;

    public void setAutoPictureCallback(OnTakePictureCallback callback) {
        autoPictureCallback = callback;
    }

    public void setMaskType(@SuperiorMaskView.MaskType int maskType, final Context ctx) {
        maskView.setMaskType(maskType);

        maskView.setVisibility(VISIBLE);
        hintView.setVisibility(VISIBLE);

        int hintResourceId = R.drawable.bd_superior_camera_round_corner;
        this.maskType = maskType;
        boolean isNeedSetImage = true;
        switch (maskType) {
            case SuperiorMaskView.MASK_TYPE_FILLET_SQUARE:
                hintResourceId = R.drawable.bd_superior_camera_round_corner;
                isNeedSetImage = false;
                break;
            case SuperiorMaskView.MASK_TYPE_NONE:
            default:
                maskView.setVisibility(INVISIBLE);
                hintView.setVisibility(INVISIBLE);
                break;
        }

        if (isNeedSetImage) {
            hintView.setImageResource(hintResourceId);
            hintViewTextWrapper.setVisibility(INVISIBLE);
        }

        if (maskType == SuperiorMaskView.MASK_TYPE_FILLET_SQUARE && isEnableScan) {
            cameraControl.setDetectCallback(new ICameraControl.OnDetectPictureCallback() {
                @Override
                public int onDetect(byte[] data, int rotation) {
                    return detect(data, rotation);
                }
            });
        }
    }

    private int detect(byte[] data, final int rotation) {
        if (initNativeStatus != NATIVE_AUTH_INIT_SUCCESS) {
            showTipMessage(initNativeStatus);
            return 1;
        }
        if (cameraControl.getAbortingScan().get()) {
            return 0;
        }
        if (!CAMERA_TYPE.equals(AUTO_ORC))
            return 0;

        Rect previewFrame = cameraControl.getPreviewFrame();

        if (maskView.getWidth() == 0 || maskView.getHeight() == 0
                || previewFrame.width() == 0 || previewFrame.height() == 0) {
            return 0;
        }
        BitmapRegionDecoder decoder = null;
        try {
            decoder = BitmapRegionDecoder.newInstance(data, 0, data.length, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int width = rotation % 180 == 0 ? decoder.getWidth() : decoder.getHeight();
        int height = rotation % 180 == 0 ? decoder.getHeight() : decoder.getWidth();

        Rect frameRect = maskView.getFrameRectExtend();

        int left = width * frameRect.left / maskView.getWidth();
        int top = height * frameRect.top / maskView.getHeight();
        int right = width * frameRect.right / maskView.getWidth();
        int bottom = height * frameRect.bottom / maskView.getHeight();

        if (previewFrame.top < 0) {
            int adjustedPreviewHeight = previewFrame.height() * getWidth() / previewFrame.width();
            int topInFrame = ((adjustedPreviewHeight - frameRect.height()) / 2)
                    * getWidth() / previewFrame.width();
            int bottomInFrame = ((adjustedPreviewHeight + frameRect.height()) / 2) * getWidth()
                    / previewFrame.width();
            top = topInFrame * height / previewFrame.height();
            bottom = bottomInFrame * height / previewFrame.height();
        } else {
            if (previewFrame.left < 0) {
                int adjustedPreviewWidth = previewFrame.width() * getHeight() / previewFrame.height();
                int leftInFrame = ((adjustedPreviewWidth - maskView.getFrameRect().width()) / 2) * getHeight()
                        / previewFrame.height();
                int rightInFrame = ((adjustedPreviewWidth + maskView.getFrameRect().width()) / 2) * getHeight()
                        / previewFrame.height();
                left = leftInFrame * width / previewFrame.width();
                right = rightInFrame * width / previewFrame.width();
            }
        }

        Rect region = new Rect();
        region.left = left;
        region.top = top;
        region.right = right;
        region.bottom = bottom;

        if (rotation % 180 == 90) {
            int x = decoder.getWidth() / 2;
            int y = decoder.getHeight() / 2;

            int rotatedWidth = region.height();
            int rotated = region.width();

            region.left = x - rotatedWidth / 2;
            region.top = y - rotated / 2;
            region.right = x + rotatedWidth / 2;
            region.bottom = y + rotated / 2;
            region.sort();
        }

        BitmapFactory.Options options = new BitmapFactory.Options();

        int maxPreviewImageSize = 2560;
        int size = Math.min(decoder.getWidth(), decoder.getHeight());
        size = Math.min(size, maxPreviewImageSize);

        options.inSampleSize = SuperiorImageUtil.calculateInSampleSize(options, size, size);
        options.inScaled = true;
        options.inDensity = Math.max(options.outWidth, options.outHeight);
        options.inTargetDensity = size;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = decoder.decodeRegion(region, options);
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            if (bitmap != rotatedBitmap) {
                bitmap.recycle();
            }
            bitmap = rotatedBitmap;
        }

        if (RESULT_STATUS == SCAN_SUCCESS) {
            if (!cameraControl.getAbortingScan().compareAndSet(false, true)) {
                bitmap.recycle();
                return SCAN_SUCCESS;
            }
        }
        if(SuperiorSetting.CAMERA_FACING == Camera.CameraInfo.CAMERA_FACING_FRONT){
            Matrix m = new Matrix();
            m.setScale(1, -1);
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, m, true);
        }
        autoPictureCallback.onPictureTaken(bitmap);
        showTipMessage(status);
        return status;
    }

    private void showTipMessage(final int status) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (status == 0) {
                    hintViewText.setVisibility(View.INVISIBLE);
                } else if (!cameraControl.getAbortingScan().get()) {
                    hintViewText.setVisibility(View.VISIBLE);
                    long currentTime = System.currentTimeMillis();
                    if(currentTime - lastShowTime.get() > showMinInterval){
                        lastShowTime.set(currentTime);
                        hintViewText.setText(getScanMessage(status));
                    }
                }
            }
        });
    }

    /**
     * @param status
     * @return
     */
    private String getScanMessage(int status) {
        String message;
        switch (status) {
            default:
                message = this.getContext().getString(R.string.bd_superior_camera_tip_face_to_frame);;
        }
        return message;
    }

    private void init() {
        cameraControl = new SuperiorCameraControl(getContext());
        displayView = cameraControl.getDisplayView();
        addView(displayView);
        maskView = new SuperiorMaskView(getContext());
        addView(maskView);

        hintView = new ImageView(getContext());
        addView(hintView);

        hintViewTextWrapper = new LinearLayout(getContext());
        hintViewTextWrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, DimensionUtil.dpToPx(25));

        lp.gravity = Gravity.CENTER;
        hintViewText = new TextView(getContext());
        hintViewText.setBackgroundResource(R.drawable.bd_superior_camera_round_corner);
        hintViewText.setAlpha(0.5f);
        hintViewText.setPadding(DimensionUtil.dpToPx(10), 0, DimensionUtil.dpToPx(10), 0);
        hintViewTextWrapper.addView(hintViewText, lp);


        hintViewText.setGravity(Gravity.CENTER);
        hintViewText.setTextColor(Color.WHITE);
        hintViewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        hintViewText.setText(getScanMessage(-1));


        addView(hintViewTextWrapper, lp);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        displayView.layout(left, 0, right, bottom - top);
        maskView.layout(left, 0, right, bottom - top);

        int hintViewWidth = DimensionUtil.dpToPx(250);
        int hintViewHeight = DimensionUtil.dpToPx(25);

        int hintViewLeft = (getWidth() - hintViewWidth) / 2;
        int hintViewTop = maskView.getFrameRect().bottom + DimensionUtil.dpToPx(16);

        hintViewTextWrapper.layout(hintViewLeft, hintViewTop,
                hintViewLeft + hintViewWidth, hintViewTop + hintViewHeight);

        hintView.layout(hintViewLeft, hintViewTop,
                hintViewLeft + hintViewWidth, hintViewTop + hintViewHeight);
    }

    /**
     * crop bitmap
     * @param data       data
     * @param rotation   rotation
     * @return crop bitmap
     */
    private Bitmap crop(byte[] data, int rotation) {
        try {
            Rect previewFrame = cameraControl.getPreviewFrame();
            if (maskView.getWidth() == 0 || maskView.getHeight() == 0 || previewFrame.width() == 0 || previewFrame.height() == 0) {
                return null;
            }
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(data, 0, data.length, true);
            int width = rotation % 180 == 0 ? decoder.getWidth() : decoder.getHeight();
            int height = rotation % 180 == 0 ? decoder.getHeight() : decoder.getWidth();

            Rect frameRect = maskView.getFrameRect();

            int left = width * frameRect.left / maskView.getWidth();
            int top = height * frameRect.top / maskView.getHeight();
            int right = width * frameRect.right / maskView.getWidth();
            int bottom = height * frameRect.bottom / maskView.getHeight();
            if (previewFrame.top < 0) {
                int adjustedPreviewHeight = previewFrame.height() * getWidth() / previewFrame.width();
                int topInFrame = ((adjustedPreviewHeight - frameRect.height()) / 2)
                        * getWidth() / previewFrame.width();
                int bottomInFrame = ((adjustedPreviewHeight + frameRect.height()) / 2) * getWidth()
                        / previewFrame.width();
                top = topInFrame * height / previewFrame.height();
                bottom = bottomInFrame * height / previewFrame.height();
            } else {
                if (previewFrame.left < 0) {
                    int adjustedPreviewWidth = previewFrame.width() * getHeight() / previewFrame.height();
                    int leftInFrame = ((adjustedPreviewWidth - maskView.getFrameRect().width()) / 2) * getHeight()
                            / previewFrame.height();
                    int rightInFrame = ((adjustedPreviewWidth + maskView.getFrameRect().width()) / 2) * getHeight()
                            / previewFrame.height();
                    left = leftInFrame * width / previewFrame.width();
                    right = rightInFrame * width / previewFrame.width();
                }
            }

            Rect region = new Rect();
            region.left = left;
            region.top = top;
            region.right = right;
            region.bottom = bottom;
            if (rotation % 180 == 90) {
                int x = decoder.getWidth() / 2;
                int y = decoder.getHeight() / 2;

                int rotatedWidth = region.height();
                int rotated = region.width();

                region.left = x - rotatedWidth / 2;
                region.top = y - rotated / 2;
                region.right = x + rotatedWidth / 2;
                region.bottom = y + rotated / 2;
                region.sort();
            }

            BitmapFactory.Options options = new BitmapFactory.Options();

            int maxPreviewImageSize = 2560;
            int size = Math.min(decoder.getWidth(), decoder.getHeight());
            size = Math.min(size, maxPreviewImageSize);

            options.inSampleSize = SuperiorImageUtil.calculateInSampleSize(options, size, size);
            options.inScaled = true;
            options.inDensity = Math.max(options.outWidth, options.outHeight);
            options.inTargetDensity = size;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = decoder.decodeRegion(region, options);

            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                if (bitmap != rotatedBitmap) {
                    bitmap.recycle();
                }
                bitmap = rotatedBitmap;
            }
            if(SuperiorSetting.CAMERA_FACING == Camera.CameraInfo.CAMERA_FACING_FRONT){
                Matrix m = new Matrix();
                m.setScale(1, -1);
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, m, true);
            }
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class CameraViewTakePictureCallback implements ICameraControl.OnTakePictureCallback {

        private OnTakePictureCallback callback;

        @Override
        public void onPictureTaken(final byte[] data) {
            CameraThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    final int rotation = SuperiorImageUtil.getOrientation(data);
                    Bitmap bitmap = crop(data, rotation);
                    if (callback != null){
                        callback.onPictureTaken(bitmap);
                    }
                }
            });
        }
    }
}
