package com.admin.texturedemo;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowManager;

import java.util.Arrays;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

        // 设置窗体显示类型
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
            mCameraManager.openCamera("0", new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    surface = new Surface(texture.getSurfaceTexture());
                    surface2 = new Surface(texture2.getSurfaceTexture());
                    cameraDevice = camera;
                    try {
                        cameraDevice.createCaptureSession(Arrays.asList(surface, surface2), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                mCameraCaptureSession = session;
                                try {
                                    mPreviewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                    mPreviewRequestBuilder.addTarget(surface);
                                    mPreviewRequestBuilder.addTarget(surface2);
                                    mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);

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
}
