package com.dominic.skuface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by 박우영 on 2018-03-23.
 * 카메라의 셔터동작없이 안면인식 시 해당화면을 캡쳐하여 비트맵으로 표현하기 위한 클래스
 *
 * @author 박우영
 * @version 0.1
 * @see android.hardware.Camera
 * @see android.graphics.Bitmap
 */

public class FaceDetectionCamera implements Camera.FaceDetectionListener {

    private static final String TAG = FaceDetectionCamera.class.getSimpleName();

    private Context context;
    private Camera mCamera = null;
    private boolean isFaceDetectionRunning = false;
    private int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private OnFaceDetectedListener onFaceDetectedListener = null;

    /**
     * @deprecated 결합성 문제로 사용안함.
     */
    @Deprecated
    private Camera.FaceDetectionListener faceDetectionListener = this;

    public FaceDetectionCamera(Context context) {
        this.context = context;
    }

    /**
     * 카메라 객체 생성
     * 하나의 {@link SurfaceView}마다 생성해야한다.
     */
    private void initCameraInstance() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        for (int cameraId = 0; cameraId < Camera.getNumberOfCameras(); cameraId++) {
            Camera.getCameraInfo(cameraId, cameraInfo);
            if (cameraInfo.facing == this.cameraId) {
                mCamera = Camera.open(cameraId);
            }
        }

    }

    /**
     * 전면 또는 후면 카메라 선택
     *
     * @param cameraId 사용하고자 하는 카메라의 ID를 준다. Default : Camera.CameraInfo.CAMERA_FACING_FRONT
     * @see Camera.CameraInfo 카메라에 대한 정보가 정의되어 있으니 필요하면 확인한다.
     */
    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    /**
     * 안면인식 감지 시 이벤트를 설정
     *
     * @param faceDetectionListener 설정하고자 하는 이벤트를
     * @see Camera.FaceDetectionListener
     * @deprecated
     */
    @Deprecated
    public void setFaceDetectionListener(Camera.FaceDetectionListener faceDetectionListener) {
        this.faceDetectionListener = faceDetectionListener;
    }

    /**
     * 안면인식 시작
     * 연속적인 사용 시 Bitmap 생성에 의한 {@link OutOfMemoryError} 주의
     *
     * @see #setCameraId(int)
     * @see #initCameraInstance() 하나의 {@link SurfaceView}마다 생성해야한다.
     * @see #setFaceDetectionListener(Camera.FaceDetectionListener) 설정하고자 하는 이벤트
     */
    public void startFaceDetection() {
        if (isFaceDetectionRunning)
            return;

        if (mCamera == null) {
            initCameraInstance();
            mCamera.setFaceDetectionListener(this);
        }

        try {
            if (mCamera.getParameters().getMaxNumDetectedFaces() > 0) {
                SurfaceView view = new SurfaceView(context);
                mCamera.setPreviewDisplay(view.getHolder());
                mCamera.startPreview();
                mCamera.startFaceDetection();
                isFaceDetectionRunning = true;
                Log.i(TAG, "Face Detection Started");
            } else {
                Log.i(TAG, "Face Detection Not Supported");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Camera instance may be null or Camera Not Support");
        }

    }

    /**
     * 안면인식 중지
     */
    public void stopFaceDetection() {
        if (!isFaceDetectionRunning)
            return;

        Log.i(TAG, "Face Detection Stopped");
        mCamera.stopFaceDetection();
        mCamera.stopPreview();
        isFaceDetectionRunning = false;
        mCamera.setFaceDetectionListener(null);
        mCamera = null;
    }

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera) {
        if (faces.length <= 0) return;

        camera.enableShutterSound(false);
        camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                Bitmap captured = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                int width = captured.getWidth();
                int height = captured.getHeight();

                Matrix matrix = new Matrix();
                matrix.postRotate(270);

                Bitmap resizedBitmap = Bitmap.createBitmap(captured, 0, 0, width, height, matrix, true);
                if (onFaceDetectedListener != null)
                    onFaceDetectedListener.onFaceDetected(resizedBitmap);

                captured.recycle();
            }
        });
    }

    /**
     * 안면인식 시의 이벤트를 작성한다.
     *
     * @see #onFaceDetection(Camera.Face[], Camera)
     * @see Camera.PictureCallback
     */
    public interface OnFaceDetectedListener {
        void onFaceDetected(Bitmap capturedFace);
    }

    public void setOnFaceDetectedListener(OnFaceDetectedListener onFaceDetectedListener) {
        this.onFaceDetectedListener = onFaceDetectedListener;
    }
}