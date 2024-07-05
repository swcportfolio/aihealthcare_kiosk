package com.lukken.aihealthcare.Scene;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.usb.UsbDevice;
import android.media.Image;
import android.media.ImageReader;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;
import com.google.gson.JsonObject;
import com.jiangdg.usbcamera.UVCCameraHelper;
import com.lukken.aihealthcare.CommonSignActivity;
import com.lukken.aihealthcare.MainActivity;
import com.lukken.aihealthcare.R;
import com.lukken.aihealthcare.RELEASE_DEFINE;
import com.lukken.aihealthcare.ResultWebViewActivity;
import com.lukken.aihealthcare.httpTask.HttpTask;
import com.lukken.aihealthcare.recognition.FaceProcessor;
import com.lukken.aihealthcare.recognition.id3Parameters;
import com.lukken.aihealthcare.utils.LocalDataStore;
import com.lukken.aihealthcare.utils.ScanBG;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.widget.CameraViewInterface;
import com.serenegiant.usb.widget.UVCCameraTextureView;

import eu.id3.face.DetectedFace;
import eu.id3.face.FaceCandidate;
import eu.id3.face.FaceTemplate;
import eu.id3.face.PixelFormat;
import eu.id3.face.Rectangle;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SceneFace {
    MainActivity activity;
    public SceneFace(MainActivity a){
        activity = a;
        Init();
    }

    UVCCameraHelper mCameraHelper;
    UVCCameraTextureView mUVCCameraView;

    FaceProcessor faceProcessor = null;
    public FaceProcessor getFaceProcessor(){ return faceProcessor; }

    ScanBG scanBG;
    ImageView imgTitle;

    public enum RECOGNITION_TYPE{
        eFace_None,     //
        eFace_Add,      //얼굴 등록
        eFace_Verify    //얼굴 검사
    };
    RECOGNITION_TYPE eSceneType = RECOGNITION_TYPE.eFace_None;

    CountDownAnimation countDownAnimation = null;
    TextView mCountdownText;
    Animation mFadeAnim;
    int mCountdown = 3;


    void Init(){
        scanBG = activity.findViewById(R.id.scan_bg);
        scanBG.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        //imgTitle = activity.findViewById(R.id.fr_title);

        mCountdownText = activity.findViewById(R.id.fr_countdown);
        mFadeAnim = AnimationUtils.loadAnimation(activity, R.anim.fade_in);

        activity.findViewById(R.id.fd_back).setOnClickListener(v -> {
            if(eSceneType == RECOGNITION_TYPE.eFace_Add){
                activity.getLayoutJoin().setVisibility(View.VISIBLE);
            }
            else { //if(eSceneType == RECOGNITION_TYPE.eFace_Verify)
                activity.getLayoutHome().setVisibility(View.VISIBLE);
            }

            eSceneType = RECOGNITION_TYPE.eFace_None;
            StopCountDown();
        });

        faceProcessor = new FaceProcessor(activity);
        mUVCCameraView = activity.findViewById(R.id.camera_view);
        mUVCCameraView.setCallback(mCallback);
        //@내일 확인
        // FHD로 변경후 카메라 테스트 진행해봐야됨
        mCameraHelper = new UVCCameraHelper(1280, 720);
        mCameraHelper.initUSBMonitor(activity, mUVCCameraView, listener);
        mCameraHelper.setOnPreviewFrameListener(mPreviewListener);


    }

    public void onStart(){
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
        }
    }

    public void onStop(){
        if (mCameraHelper != null) {
            mCameraHelper.unregisterUSB();
        }
    }

    String addName;
    public void StartScene(String name, RECOGNITION_TYPE type){
        eSceneType = type;
        addName = name;
        canStartCountdown = true;
        if(type == RECOGNITION_TYPE.eFace_Add) {
            //imgTitle.setImageResource(R.drawable.title06);
        }else{
            //imgTitle.setImageResource(R.drawable.title02);
            activity.getLayoutHome().setVisibility(View.GONE);
            activity.getLayoutHealthHome().setVisibility(View.GONE);
            activity.getLayoutJoin().setVisibility(View.GONE);
        }
    }

    boolean canStartCountdown = true;
    void StartCountDown(){
        canStartCountdown = false;
        mCountdown = 3;
        activity.runOnUiThread(() -> {
            mCountdownText.setVisibility(View.VISIBLE);
            countDownAnimation = new CountDownAnimation(3000, 1000);
            countDownAnimation.start();
        });
    }

    void StopCountDown(){
        if (countDownAnimation != null) {
            countDownAnimation.cancel();
            countDownAnimation = null;
        }
        activity.runOnUiThread(() -> {
            mCountdownText.setText("");
            mCountdownText.setVisibility(View.GONE);
        });
    }

    eu.id3.face.Image processingImage;
    DetectedFace detectedFace;
    AbstractUVCCameraHandler.OnPreViewResultListener mPreviewListener = nv21 -> {
        if(eSceneType == RECOGNITION_TYPE.eFace_None) return;
        try{
            int yStride = mCameraHelper.getPreviewWidth();
            int uvStride = (mCameraHelper.getPreviewWidth() + 1) / 2;
            int uvHeight = mCameraHelper.getPreviewHeight() / 2;
            int stride = yStride + (uvStride * uvHeight) * 2;

            processingImage = eu.id3.face.Image.fromRawBuffer(nv21, mCameraHelper.getPreviewWidth(), mCameraHelper.getPreviewHeight(), stride, PixelFormat.NV12, PixelFormat.BGR_24BITS);
            processingImage.downscale(id3Parameters.maxProcessingImageSize);
            if(RELEASE_DEFINE.CAM_ROTATE)
                processingImage.rotate(90);

            //얼굴 인식
            detectedFace = faceProcessor.detectLargestFace(processingImage);
            if(detectedFace != null) {
                Rectangle rectangle = detectedFace.getBounds();
                boolean center = scanBG.setBounding(rectangle, processingImage.getWidth(), processingImage.getHeight());
                //얼굴이 화면 중앙
                if (center && canStartCountdown && eSceneType != RECOGNITION_TYPE.eFace_None) {
                    StartCountDown();
                }
            }
        }catch(Exception e){

        }
    };

    void RecognitionFace(){
        //카운트다운 완료 후 얼굴이 있으면
        detectedFace = faceProcessor.detectLargestFace(processingImage);
        if(detectedFace != null) {
            int quality = faceProcessor.checkQuality(processingImage, detectedFace, eSceneType);

            if(quality == -1){
                activity.showAlert("검색된 회원이 없습니다.", 1500, alertEndListener);
            }else if(quality < 50){
                activity.showAlert("카메라를 바라보며 얼굴이 중앙에 오도록 서주세요.", 1500, alertEndListener);
            }else{
                //얼굴 등록
                if(eSceneType == RECOGNITION_TYPE.eFace_Add) {
                    try{
                        FaceTemplate template = faceProcessor.enrollLargestFace(processingImage, detectedFace);
                        if (template != null) {
                            eSceneType = RECOGNITION_TYPE.eFace_None;
                            faceProcessor.saveTemplate(template, addName);
                            activity.getSceneJoin().setAddFace(true);
                            activity.showAlert("안면정보 등록 완료", 1000, ()->{
                                activity.runOnUiThread(() -> {
                                    activity.getLayoutJoin().setVisibility(View.VISIBLE);
                                });
                            });
                        }
                    } catch (Exception e){
                        Toast.makeText(activity, e.toString(), Toast.LENGTH_LONG).show();
                        activity.showAlert("얼굴 등록 \n너무 멀리 있습니다.", 1500, alertEndListener);
                    }
                }

                //얼굴 인식
                else if(eSceneType == RECOGNITION_TYPE.eFace_Verify){
//                    try {
//                        FaceCandidate data = faceProcessor.verifyLargestFace(processingImage, detectedFace);
//                        if (data != null) {
//                            if (data.getScore() > id3Parameters.fmrThreshold.getValue()) {
//                                //activity.runOnUiThread(() -> {
//                                    eSceneType = RECOGNITION_TYPE.eFace_None;
//                                    setUserInfo(data.getId());
//                                //});
//                            } else {
//                                activity.showAlert("검색된 회원이 없습니다.", 1500, alertEndListener);
//                            }
//                        }
//                        //등록된얼굴 없음
//                        else {
//                            activity.showAlert("검색된 회원이 없습니다.", 1500, alertEndListener);
//                        }
//                    } catch(Exception e){
//                        activity.showAlert("얼굴 인식 \n너무 멀리 있습니다.", 1500, alertEndListener);
//                    }

                    eSceneType = RECOGNITION_TYPE.eFace_None;
                    setUserInfo("U07001");
                }
            }
        } else {
            activity.showAlert("카메라를 바라보며 얼굴이 중앙에 오도록 서주세요.", 1500, alertEndListener);
        }
        activity.runOnUiThread(() -> {
            mCountdownText.setText("");
            mCountdownText.setVisibility(View.GONE);
        });
    }

    private CameraViewInterface.Callback mCallback = new CameraViewInterface.Callback(){
        @Override
        public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
            setCamScale();
        }
        @Override
        public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {}
        @Override
        public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {}
    };
    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {
        @Override
        public void onAttachDev(UsbDevice device) {
            LocalDataStore localDataStore = LocalDataStore.getInstance(activity);
            int venderID = localDataStore.getValueInt(LocalDataStore.SP_OP_CIS_CAM);
            if (mCameraHelper != null && device.getVendorId() == venderID)
                mCameraHelper.requestPermission(device);
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            LocalDataStore localDataStore = LocalDataStore.getInstance(activity);
            int venderID = localDataStore.getValueInt(LocalDataStore.SP_OP_CIS_CAM);
            if (mCameraHelper != null && device.getVendorId() == venderID)
                mCameraHelper.closeCamera();
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {}
        @Override
        public void onDisConnectDev(UsbDevice device) {}
    };

    void setCamScale() {
        int camWidth = mCameraHelper.getPreviewWidth();
        int camHeight = mCameraHelper.getPreviewHeight();
        int viewWidth = mUVCCameraView.getWidth();
        int viewHeight = mUVCCameraView.getHeight();

        float xScale = (float) viewWidth / camWidth;
        float yScale = (float) viewHeight / camHeight;
        float scale = Math.max(xScale, yScale)/1.25f;   //늘리기
        //float scale = Math.min(xScale, yScale);   //원본비율

        float scaledWidth = scale * camWidth;
        float scaledHeight = scale * camHeight;

        ConstraintLayout constraintLayout = activity.findViewById(R.id.camlayout);
        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);
        set.constrainWidth(mUVCCameraView.getId(), (int)scaledWidth);
        set.constrainHeight(mUVCCameraView.getId(), (int)scaledHeight);
        set.connect(mUVCCameraView.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        set.connect(mUVCCameraView.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
        set.connect(mUVCCameraView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
        set.connect(mUVCCameraView.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        set.setHorizontalBias(mUVCCameraView.getId(), 0.5f);
        set.setVerticalBias(mUVCCameraView.getId(), 0.5f);
        set.applyTo(constraintLayout);

        //좌우반전
        final RectF viewRect = new RectF(0, 0, scaledWidth, scaledHeight);
        Matrix matrix = new Matrix();
        matrix.postScale(-1, 1, viewRect.centerX(), viewRect.centerY());
        if(RELEASE_DEFINE.CAM_ROTATE)
            matrix.postRotate(90, scaledWidth/2, scaledHeight/2);
        mUVCCameraView.setTransform(matrix);
    }

    class CountDownAnimation extends CountDownTimer {
        public CountDownAnimation(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mCountdownText.setText(String.valueOf(mCountdown--));
            mCountdownText.startAnimation(mFadeAnim);
        }

        @Override
        public void onFinish() {
            //카운트다운 완료
            RecognitionFace();
        }
    }

    void setUserInfo(String ucode){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(HttpTask.URL_DOMAIN)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        HttpTask httpTask = retrofit.create(HttpTask.class);

        Log.d("sendSignUpInfoToOnwardsService", ucode);
        httpTask.getGHealth(ucode).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                assert response.body() != null;
                int code = response.body().get("code").getAsInt();
                Log.d("회원 정보 조회", response.body().toString());
                if(code == 200) {
                    String mBarcode = response.body().get("barcode").getAsString();
//                    if(mBarcode == null || mBarcode.equals("")){
                        activity.getLayoutHome().setVisibility(View.VISIBLE);

                        Intent intent = new Intent(activity, ResultWebViewActivity.class);
                        intent.putExtra("ucode", ucode);
                        activity.startActivity(intent);
//                    }
//                    else {
                        // SceneFace 종료후 바코드 값이 있어
                        //initSceneFaceSceneHealthZone 실행
                        //activity.initSceneFaceSceneHealthZone(ucode);
                    //}
                } else {
                    activity.showAlert("회원 조회의 실패 해였습니다. 다시 시도 바랍니다.", 1500, alertEndListener);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                activity.showAlert("서버가 불안정합니다. 다시 시도 바랍니다.", 1500, alertEndListener);
            }
        });
    }


    MainActivity.OnAlertEndListener alertEndListener = () -> canStartCountdown = true;
}
