package com.admin.texturedemo;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, View.OnTouchListener {

    private TextureView texture;
    private TextureView texture2;
    private CameraManager mCameraManager;
    private CameraDevice cameraDevice;
    private Surface surface;
    private Surface surface2;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraCaptureSession mCameraCaptureSession;
    private WindowManager windowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private boolean isShow;
    private float startX;
    private float startY;
    private int viewX;
    private int viewY;
    private boolean isMove;
    private String cameraId;
    private Size mPreviewSize;
    private ImageReader mReader;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private Surface mReaderSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandlerThread = new HandlerThread("QuickCamera");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        initView();
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        texture.setSurfaceTextureListener(this);
        showFloatView();
        findViewById(R.id.btn_show).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isShow) {
                    texture.setVisibility(View.VISIBLE);
                    texture2.setVisibility(View.GONE);
                } else {
                    texture.setVisibility(View.GONE);
                    texture2.setVisibility(View.VISIBLE);
                }
                isShow = !isShow;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCameraDevice(cameraDevice);
        releaseCameraSession(mCameraCaptureSession);
        releaseImageReader(mReader);
    }

    private void showFloatView() {
        if (null == windowManager) {
            windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        }

        if (texture2 == null) {
            texture2 = new TextureView(this);
        }
        texture2.setOnTouchListener(this);

        mLayoutParams = new WindowManager.LayoutParams();
        mLayoutParams.windowAnimations = android.R.style.Animation_Translucent;

        // ????????????????????????
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        mLayoutParams.format = PixelFormat.RGBA_8888;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_FULLSCREEN;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mLayoutParams.width = dip2px(96);
        mLayoutParams.height = dip2px(96);
        windowManager.addView(texture2, mLayoutParams);
    }

    private void openCamera() {
        try {
            mCameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    surface = new Surface(texture.getSurfaceTexture());
                    //??????TextureView???????????????
                    texture.getSurfaceTexture().setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                    surface2 = new Surface(texture2.getSurfaceTexture());
                    texture2.getSurfaceTexture().setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

                    mReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);
                    mReaderSurface = mReader.getSurface();
                    //??????ImageReader(ImageFormat????????????jpeg,?????????????????????)
                    mReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                        @Override
                        public void onImageAvailable(ImageReader reader) {
                            //????????????acquireLatestImage()???close(),???????????????
                            Image image = reader.acquireLatestImage();
                            //?????????????????????????????????????????????Camera1???PreviewCallback????????????????????????
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] data = new byte[buffer.remaining()];
                            buffer.get(data);
                            //Log.d(TAG, "onImageAvailable: data size"+data.length);
                            image.close();
                        }
                    }, mHandler);

                    cameraDevice = camera;
                    try {
                        cameraDevice.createCaptureSession(Arrays.asList(surface, surface2, mReaderSurface), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                mCameraCaptureSession = session;
                                try {
                                    mPreviewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                                    //??????????????????set(key,value)??????????????????????????????????????????????????????
                                    //mCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                    mPreviewRequestBuilder.addTarget(MainActivity.this.surface);
                                    mPreviewRequestBuilder.addTarget(surface2);
                                    mPreviewRequestBuilder.addTarget(mReaderSurface);
                                    mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mHandler);

                                    texture2.setVisibility(View.GONE);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {

                }
            }, new Handler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        texture = (TextureView) findViewById(R.id.texture);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        configCamera(surface, width, height);
        openCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private void configCamera(SurfaceTexture surface, int width, int height) {
        try {
            //???????????????????????????????????????
            for (String cid : mCameraManager.getCameraIdList()) {
                //??????????????????
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cid);
                //??????????????????
                int facing = characteristics.get(CameraCharacteristics.LENS_FACING);//??????????????????
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                //????????????????????????/????????????
                StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                //????????????????????????
                mPreviewSize = getOptimalSize(configs.getOutputSizes(SurfaceTexture.class), width, height);
                cameraId = cid;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * ???????????????????????????Size??????????????????????????????width???height???size
     *
     * @param sizeMap
     * @param width
     * @param height
     * @return
     */
    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        if (sizeList.size() > 0)
            return sizeList.get(0);
        return sizeMap[0];
    }

    public int dip2px(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getRawX();
                startY = event.getRawY();
                viewX = mLayoutParams.x;
                viewY = mLayoutParams.y;
                break;
            case MotionEvent.ACTION_MOVE:
                float offsetX = event.getRawX() - startX;
                float offsetY = event.getRawY() - startY;
                if (Math.abs(offsetX) >= 35 || Math.abs(offsetY) >= 35) {
                    isMove = true;
                    mLayoutParams.x = (int) (viewX + offsetX);
                    mLayoutParams.y = (int) (viewY + offsetY);
                    windowManager.updateViewLayout(view, mLayoutParams);
                } else {
                    isMove = false;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isMove = false;
                break;
        }
        return true;
    }

    /**
     * ??????????????????
     *
     * @param cameraDevice
     */
    public void releaseCameraDevice(CameraDevice cameraDevice) {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    /**
     * ??????????????????
     *
     * @param session
     */
    public void releaseCameraSession(CameraCaptureSession session) {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    /**
     * ?????? ImageReader
     *
     * @param reader
     */
    public void releaseImageReader(ImageReader reader) {
        if (reader != null) {
            reader.close();
            reader = null;
        }
    }

}
