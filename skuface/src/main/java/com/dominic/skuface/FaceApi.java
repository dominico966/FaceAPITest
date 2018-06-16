package com.dominic.skuface;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.constraint.solver.widgets.Rectangle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Vector;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by 박우영 on 2018-03-24.
 * Microsoft Azure Face API 구현 클래스
 * REST API 활용
 *
 * @author 박우영
 * @version 0.1
 */
public class FaceApi {

    private static FaceApi instance = null;

    private static final String SERVICE_KEY = "928e20ee83f847ab8acd83affaf1a994";

    private static final String REST_URL = "https://eastasia.api.cognitive.microsoft.com/face/v1.0/detect";

    private Vector<Face> faceList = new Vector<>();

    private OnResponseListener onResponseListener = null;

    private FaceApi() {
    }

    /**
     * {@link FaceApi} 인스턴스 생성
     *
     * @return FaceApi 인스턴스
     */
    public static FaceApi getInstance() {
        if(instance == null)
            instance = new FaceApi();
        return instance;
    }

    /**
     * 이미지의 얼굴부분에 사각형 모양을 그려준다.
     *
     * @param originalBitmap 얼굴이 찍힌 이미지 {@link Bitmap}
     * @param faces 사진 상의 얼굴들에 대한 정보 리스트
     * @return 원본 {@link Bitmap}에서 얼굴 부분에 사격형을 표시를 추가한 {@link Bitmap}
     * @see #getFaceList()
     */
    public static Bitmap drawFaceRectanglesOnBitmap(Bitmap originalBitmap, List<Face> faces) {
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);

        int strokeWidth = 8;
        paint.setStrokeWidth(strokeWidth);

        if (faces != null) {
            for (FaceApi.Face face : faces) {
                Rectangle faceRectangle = face.getFaceRectangle();
                canvas.drawRect(
                        faceRectangle.x,
                        faceRectangle.y,
                        faceRectangle.x + faceRectangle.width,
                        faceRectangle.y + faceRectangle.height,
                        paint
                );

            }
        }

        return bitmap;
    }

    /**
     * REST API를 사용하여 {@link Bitmap}에 대한 분석을 요청한다.
     * 호출 시 이전 {@link #getFaceList()}는 초기화 된다.
     *
     * @param imageBitmap 4MBytes 이하의 이미지
     */
    public void detectAndFrameRest(final Bitmap imageBitmap) {
        faceList.clear();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        final OkHttpClient client = new OkHttpClient();

        String url = REST_URL
                + "?returnFaceId=true"
                + "&returnFaceLandmarks=false"
                + "&returnFaceAttributes=emotion";

        final Request request = new Request.Builder()
                .addHeader("Content-Type", "application/octet-stream")
                .addHeader("Ocp-Apim-Subscription-Key", SERVICE_KEY)
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/octet-stream"), baos.toByteArray()))
                .build();

        new Thread() {
            @Override
            public void run() {
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            final String responseStatus = response.toString();
                            final JSONArray result = new JSONArray(response.body().string());

                            for (int i = 0; i < result.length(); i++) {
                                JSONObject face = result.getJSONObject(i);

                                String faceId = face.getString("faceId");

                                JSONObject faceRectangle = face.getJSONObject("faceRectangle");
                                Rectangle r = new Rectangle();
                                r.width = faceRectangle.getInt("width");
                                r.height = faceRectangle.getInt("height");
                                r.x = faceRectangle.getInt("left");
                                r.y = faceRectangle.getInt("top");

                                JSONObject faceAttributes = face.getJSONObject("faceAttributes");
                                JSONObject emotion = faceAttributes.getJSONObject("emotion");

                                FaceApi.Face.Emotion e = new FaceApi.Face.Emotion(
                                        emotion.getDouble("anger"),
                                        emotion.getDouble("contempt"),
                                        emotion.getDouble("disgust"),
                                        emotion.getDouble("fear"),
                                        emotion.getDouble("happiness"),
                                        emotion.getDouble("neutral"),
                                        emotion.getDouble("sadness"),
                                        emotion.getDouble("surprise")
                                );

                                faceList.add(new FaceApi.Face(faceId, r, e));
                            }

                            Bitmap framedImage = drawFaceRectanglesOnBitmap(imageBitmap,getFaceList());
                            if(onResponseListener != null){
                                onResponseListener.onResponse(framedImage,getFaceList());
                            } else {
                                framedImage.recycle();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }.run();
    }

    /**
     * {@link #detectAndFrameRest(Bitmap)} 호출 후에 응답이 성공하면 호출된다.
     * 스레드가 다름으로 UI처리는 {@link android.app.Activity#runOnUiThread(Runnable)}에서 처리한다.
     */
    public interface OnResponseListener {
        void onResponse(Bitmap framedImage, List<Face> faceList);
    }

    public void setOnResponseListener(OnResponseListener onResponseListener) {
        this.onResponseListener = onResponseListener;
    }

    public List<Face> getFaceList() {
        return this.faceList;
    }

    public void clearFaceList() {
        this.faceList.clear();
    }

    /**
     * 얼굴들에 대한 정보를 저장하는 클래스
     */
    public static class Face {
        private String faceId;

        /**
         *  인식된 얼굴의 비트맵 좌표를 나타낸다. 외부에서 접근 할 때 오류가 발생하면 'com.android.support.constraint:constraint-layout:1.0.2'를 'build.gradle'의 'dependencies' 에 추가한다.
         */
        private Rectangle faceRectangle;

        private Emotion emotion;

        public Face(String faceId, Rectangle faceRectangle, Emotion emotion) {
            this.setFaceId(faceId);
            this.setFaceRectangle(faceRectangle);
            this.setEmotion(emotion);
        }

        public String getFaceId() {
            return faceId;
        }

        public void setFaceId(String faceId) {
            this.faceId = faceId;
        }

        /**
         *  인식된 얼굴의 비트맵 좌표를 나타낸다. 외부에서 접근 할 때 오류가 발생하면 'com.android.support.constraint:constraint-layout:1.0.2'를 'build.gradle'의 'dependencies' 에 추가한다.
         */
        public Rectangle getFaceRectangle() {
            return faceRectangle;
        }

        public void setFaceRectangle(Rectangle faceRectangle) {
            this.faceRectangle = faceRectangle;
        }

        public Emotion getEmotion() {
            return emotion;
        }

        public void setEmotion(Emotion emotion) {
            this.emotion = emotion;
        }

        public static class Emotion implements Serializable {
            public double anger;
            public double contempt;
            public double disgust;
            public double fear;
            public double happiness;
            public double neutral;
            public double sadness;
            public double surprise;

            public Emotion(double anger, double contempt, double disgust, double fear, double happiness, double neutral, double sadness, double surprise) {
                this.anger = anger;
                this.contempt = contempt;
                this.disgust = disgust;
                this.fear = fear;
                this.happiness = happiness;
                this.neutral = neutral;
                this.sadness = sadness;
                this.surprise = surprise;
            }

            @Override
            public String toString() {
                return String.format(
                        "anger\t: %f\n" +
                                "contempt\t: %f\n" +
                                "disgust\t: %f\n" +
                                "fear\t: %f\n" +
                                "happiness: %f\n" +
                                "neutral\t: %f\n" +
                                "sadness\t: %f\n" +
                                "surprise\t: %f\n",
                        anger, contempt, disgust, fear, happiness, neutral, sadness, surprise);
            }
        }
    }
}
