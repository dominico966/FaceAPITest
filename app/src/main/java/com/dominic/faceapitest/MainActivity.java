package com.dominic.faceapitest;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FaceApi faceAPI = null;
    private FaceDetectionCamera faceDetectionCamera = null;

    private static final int PICK_IMAGE = 1;
    private static final int FACE_DETECTION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 카메라 퍼미션
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, FACE_DETECTION);
        } else {
            initFaceDetectionCamera();
        }

        faceAPI = FaceApi.getInstance();

        initEvents();
    }

    // 카메라 초기화 및 안면인식 시 이벤트
    private void initFaceDetectionCamera() {
        faceDetectionCamera = new FaceDetectionCamera(MainActivity.this);

        faceDetectionCamera.setOnFaceDetectedListener(new FaceDetectionCamera.OnFaceDetectedListener() {
            @Override
            public void onFaceDetected(Bitmap capturedFace) {
                ImageView imageView = findViewById(R.id.imageView1);
                imageView.setImageBitmap(capturedFace);

                faceAPI.detectAndFrameRest(capturedFace);
                faceDetectionCamera.stopFaceDetection();
            }
        });

        findViewById(R.id.button_front_camera_capture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 얼굴정보 초기화
                faceAPI.clearFaceList();

                // 카메라 활성
                faceDetectionCamera.startFaceDetection();
            }
        });
    }

    private void initEvents() {
        Button buttonBrowse = findViewById(R.id.button_browse);
        final ImageView imageView1 = findViewById(R.id.imageView1);

        // 이미지 선택 이벤트
        buttonBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
                gallIntent.setType("image/*");
                startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE);
            }
        });

        // faceAPI 요청 후 성공 적으로 응답 했을 때의 이벤트
        faceAPI.setOnResponseListener(new FaceApi.OnResponseListener() {
            @Override
            public void onResponse(final Bitmap framedImage, List<FaceApi.Face> faceList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView imageView = findViewById(R.id.imageView1);
                        imageView.setImageBitmap(framedImage);
                    }
                });
            }
        });

        // 좌표 변환 과정. 중요하지 않다.
        imageView1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                    // 터치 좌표 이미지 비트맵 좌표로 변환하는 과정------------------
                    // touchedPoint on ImageView
                    float x = motionEvent.getX();
                    float y = motionEvent.getY();

                    Matrix imageMatrix = imageView1.getImageMatrix();
                    float[] values = new float[9];
                    imageMatrix.getValues(values);

                    //get the distance from the left and top of the image bounds
                    float scaledImageOffsetX = x - values[Matrix.MTRANS_X];
                    float scaledImageOffsetY = y - values[Matrix.MTRANS_Y];

                    float originalImageOffsetX = scaledImageOffsetX / values[Matrix.MSCALE_X];
                    float originalImageOffsetY = scaledImageOffsetY / values[Matrix.MSCALE_Y];

                    // -------------------------------------------------------------

                    for (FaceApi.Face face : faceAPI.getFaceList()) {
                        if (face.faceRectangle.contains((int) originalImageOffsetX, (int) originalImageOffsetY)) {
                            Context context = MainActivity.this;
                            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
                            View dialogView = layoutInflater.inflate(R.layout.dialog_face_emotion, null, false);

                            ListView listView = dialogView.findViewById(R.id.list_view);
                            listView.setAdapter(new DialogFaceEmotionAdaptor(MainActivity.this, face));

                            AlertDialog ad = new AlertDialog.Builder(context)
                                    .setView(dialogView)
                                    .setPositiveButton("OK", null)
                                    .create();
                            ad.show();
                            break;
                        }
                    }
                }
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICK_IMAGE: // Browse 버튼을 통해 갤러리에서 이미지 선택 시
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    faceAPI.clearFaceList();
                    Uri uri = data.getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        ImageView imageView = findViewById(R.id.imageView1);
                        imageView.setImageBitmap(bitmap);

                        faceAPI.detectAndFrameRest(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case FACE_DETECTION: // 카메라 퍼미션 요청이 성공했을 시
                if (resultCode == RESULT_OK) {
                    initFaceDetectionCamera();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        faceDetectionCamera.stopFaceDetection();
        super.onDestroy();
    }

}
