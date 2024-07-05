package com.lukken.aihealthcare;

import static com.lukken.aihealthcare.httpTask.HttpTask.URL_BIODATA;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.lukken.aihealthcare.Scene.SceneFace;
import com.lukken.aihealthcare.Scene.SceneHealthZone;
import com.lukken.aihealthcare.Scene.SceneJoin;
import com.lukken.aihealthcare.httpTask.HttpTask;
import com.lukken.aihealthcare.httpTask.SocketServer;
import com.lukken.aihealthcare.recognition.id3Credentials;
import com.lukken.aihealthcare.utils.DownLoadQueueData;
import com.lukken.aihealthcare.utils.LocalDataStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    //region values
    View layoutHome;
    public View getLayoutHome(){ return layoutHome; }

    //얼굴인식
    View layoutFaceRecognition;
    public View getLayoutFaceRecognition(){ return layoutFaceRecognition; }

    //회원가입
    View layoutJoin;
    public View getLayoutJoin(){ return layoutJoin; }

    //헬스케어존 홈
    View layoutHealthHome;
    public View getLayoutHealthHome(){ return layoutHealthHome; }


    //scene
    //회원가입
    SceneJoin sceneJoin;
    public SceneJoin getSceneJoin(){ return sceneJoin; }

    //얼굴등록, 얼굴확인
    SceneFace sceneFace;
    public SceneFace getSceneFace(){ return sceneFace; }

    //헬스케어존
    SceneHealthZone sceneHealthZone;
    public SceneHealthZone getSceneHealthZone(){ return sceneHealthZone; }
    //endregion

    private ArrayList<String> faceDataList = new ArrayList<>();

    View alertLayout;
    TextView alertTxt;


    //신규데이터 업데이트 타이머
    Timer mUpdateTimer = null;

    private String ucode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        LocalDataStore localDataStore = LocalDataStore.getInstance(MainActivity.this);
        int vendorId = localDataStore.getValueInt(LocalDataStore.SP_OP_CIS_CAM, -1);

        //등록된 카메라 없으면 설정으로
        //@ psw
        // 소켓통신 테스트를 위해 주석처리
        if(vendorId == -1){
            Intent intent = new Intent(this, SettingActivity.class);
            startActivity(intent);
            MainActivity.this.finish();
        }
        else{
            //얼굴인식 라이센스 체크
            boolean isLicenseOk = id3Credentials.registerSdkLicense(getFilesDir().getAbsolutePath() + "/id3FaceLicense.lic");
            if (!isLicenseOk) {
                new AlertDialog.Builder(MainActivity.this).setMessage("라이센스 확인 필요").setPositiveButton("확인", (dialog, which) -> {
                    finish();
                    System.exit(-1);
                }).setCancelable(false).show();
            }
        }

        layoutHome = findViewById(R.id.layout_home);
        layoutFaceRecognition = findViewById(R.id.layout_face_recognition);
        layoutJoin = findViewById(R.id.layout_join);
        layoutHealthHome = findViewById(R.id.layout_health_home);

        alertLayout = findViewById(R.id.alertLayout);
        alertTxt = findViewById(R.id.alertTxt);


        //안면인증
        findViewById(R.id.face_cert).setOnClickListener(v -> {

            sceneFace.StartScene("", SceneFace.RECOGNITION_TYPE.eFace_Verify);
            //@ 임시
            // 안면 인증 후 결과 헬스존 홈화면으로 이동
            //getLayoutHealthHome().setVisibility(View.VISIBLE);
        });

        // 일반인증
        findViewById(R.id.common_join).setOnClickListener(v -> {
            Intent intent = new Intent(this, CommonSignActivity.class);
            commonResult.launch(intent);
        });

        // 회원가입
        findViewById(R.id.home_join).setOnClickListener(v -> {
           sceneJoin.StartScene();
        });

        sceneJoin = new SceneJoin(this);
        sceneFace = new SceneFace(this);
        setOptionMenu();
        requestPermissions();

        //얼굴정보 자동업데이트
        //@ 암시 주석
        mUpdateTimer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                new Thread(() -> getBioDataList()).start();
            }
        };
        mUpdateTimer.schedule(timerTask, RELEASE_DEFINE.UPDATE_DATA, RELEASE_DEFINE.UPDATE_DATA);

        //인터넷 상태 감지
        NetworkRequest networkRequest = new NetworkRequest.Builder().build();
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.registerNetworkCallback(networkRequest, netCallback);

        sceneFace.onStart();
    }

    ActivityResultLauncher<Intent> commonResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {

            Intent intent = result.getData();
            ucode = intent.getStringExtra("ucode" );
            Log.d("CheckUcode:", ucode);

            Intent resultIntent = new Intent(this, ResultWebViewActivity.class);
            resultIntent.putExtra("ucode", ucode);

            Intent myInfoIntent = new Intent(this, MyInfoActivity.class);
            myInfoIntent.putExtra("ucode", ucode);

            sceneHealthZone = new SceneHealthZone(this, resultIntent, myInfoIntent);

            getLayoutHealthHome().setVisibility(View.VISIBLE);
            getSceneHealthZone().StartScene(ucode);
        } else {
            Log.d("Check Ucode:","Not Result OK");
            Log.d("Check Ucode result.getResultCode:", result.getResultCode()+"");
        }
    });


    ConnectivityManager.NetworkCallback netCallback = new ConnectivityManager.NetworkCallback(){
        @Override
        public void onAvailable(@NonNull Network network) {
            SocketServer.getInstance(MainActivity.this).start();
            super.onAvailable(network);
        }

        @Override
        public void onLost(@NonNull Network network) {
            SocketServer.getInstance(MainActivity.this).stop();
            super.onLost(network);
        }
    };


    //region 얼굴정보 다운로드
    DownloadManager mDownloadManager;
    Queue<DownLoadQueueData> mDownQueue = new LinkedList<>();
    void UpdateFaceData(){
        Retrofit retrofit = new Retrofit.Builder().baseUrl(HttpTask.URL_DOMAIN).addConverterFactory(GsonConverterFactory.create()).build();
        HttpTask httpTask = retrofit.create(HttpTask.class);
        httpTask.getBioDataList().enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {

                JsonArray jsonArray = response.body().getAsJsonArray();
                for(JsonElement je : jsonArray){
                    String filename = je.getAsString() + ".dat";
                    File file = new File(String.format("%s/%s", getExternalFilesDir(""), filename));
                    if(file.exists()) {
                        continue;
                    }
                    String url = HttpTask.URL_BIODATA + filename;
                    mDownQueue.add(new DownLoadQueueData(url, filename));

                    Log.d("mDownQueue :", url);
                }
                StartDownLoad();
                Log.d("MainActivity - 로그", "자동 안면인식 업데이트 완료");
            }
            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                Log.d("MainActivity - 로그", "자동 안면인식 업데이트 실패");

            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        IntentFilter completeFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadCompleteReceiver, completeFilter);
    }

    @Override
    public void onPause(){
        super.onPause();
        unregisterReceiver(downloadCompleteReceiver);
    }

    void StartDownLoad(){
        DownLoadQueueData d = mDownQueue.poll(); //queue에 첫번째 값을 반환하고 제거 비어있다면 null
        if(d != null) {
            URLDownloading(d.url, d.name);
        }else{
            sceneFace.getFaceProcessor().loadFaceList();
        }
    }

    void URLDownloading(String url, String filename) {
        if (mDownloadManager == null) {
            mDownloadManager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
        }
        String outpath = String.format("%s/%s", getExternalFilesDir(""), filename);
        File outputFile = new File(outpath);
        Uri downloadUri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(downloadUri);
        request.setDestinationUri(Uri.fromFile(outputFile));
        request.setAllowedOverMetered(true);
        mDownloadManager.enqueue(request);
    }

    public void URLDownloading(String ucode){
        String filename = ucode + ".dat";
        String url = HttpTask.URL_BIODATA + filename;
        if (mDownloadManager == null) {
            mDownloadManager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
        }
        String outpath = String.format("%s/%s", getExternalFilesDir(""), filename);
        File outputFile = new File(outpath);

        Uri downloadUri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(downloadUri);
        request.setDestinationUri(Uri.fromFile(outputFile));
        request.setAllowedOverMetered(true);
        mDownloadManager.enqueue(request);
    }

    private final BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(reference);
            Cursor cursor = mDownloadManager.query(query);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int status = cursor.getInt(columnIndex);
            cursor.close();
            StartDownLoad();
        }
    };
    //endregion



    //region LifeCycle
    @Override
    protected void onStart() {
        super.onStart();

    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//        sceneFace.onStop();
//    }

//    @Override
//    protected void onDestroy(){
//        super.onDestroy();
//        sceneHealthZone.Destroy();
//    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
    //endregion



    //region 팝업메시지
    public interface OnAlertEndListener{
        void onEndAlert();
    }

    public void showAlert(String msg, long delay, OnAlertEndListener listener){
        runOnUiThread(() -> {
            alertTxt.setText(msg);
            alertLayout.setVisibility(View.VISIBLE);
        });

        new Handler().postDelayed(() -> runOnUiThread(() -> {
            alertTxt.setText("");
            alertLayout.setVisibility(View.GONE);
            if(listener != null)
                listener.onEndAlert();
        }), delay);
    }
    //endregion



    //region 옵션
    private long backKeyPressedTime = 0;
    int clickCount = 0;
    void setOptionMenu(){
        findViewById(R.id.option).setOnClickListener(v -> {
            if(clickCount == 0){
                backKeyPressedTime = System.currentTimeMillis();
                clickCount++;
            }
            else if (System.currentTimeMillis() < backKeyPressedTime + 1000) {
                backKeyPressedTime = System.currentTimeMillis();
                clickCount++;
                if (clickCount == 10) {
                    clickCount = 0;
                    Intent intent = new Intent(this, SettingActivity.class);
                    startActivity(intent);
                }
            }else{
                clickCount = 0;
            }
        });

        /// 앱 실행
        findViewById(R.id.option2).setOnClickListener(v -> {
            if(clickCount == 0){
                backKeyPressedTime = System.currentTimeMillis();
                clickCount++;
            }
            else if (System.currentTimeMillis() < backKeyPressedTime + 1000) {
                backKeyPressedTime = System.currentTimeMillis();
                clickCount++;
                if (clickCount == 10) {
                    clickCount = 0;
                    //앱 재실행
                    PackageManager packageManager = getPackageManager();
                    Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
                    ComponentName componentName = intent.getComponent();
                    Intent mainIntent = Intent.makeRestartActivityTask(componentName);
                    startActivity(mainIntent);
                    System.exit(0);
                }
            }else{
                clickCount = 0;
            }
        });



    }
    //endregion



    //region 퍼미션
    static final int RC_VIDEO_APP_PERM = 124;
    int percount = 1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }
    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if(percount >= 2) {
            MainActivity.this.finish();
        } else {
            //if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            String msg = "퍼미션 에러: ";
            for(int idx=0; idx<perms.size(); ++idx){
                if(idx==0)
                    msg = msg+perms.get(idx);
                else
                    msg = msg + " / " + perms.get(idx);
            }
            new AppSettingsDialog.Builder(this).setRationale(msg/*getString(R.string.permission01)*/)
                    .setPositiveButton("설정").setNegativeButton("닫기")
                    .setRequestCode(RC_VIDEO_APP_PERM).build().show();
            //}
            percount++;
        }
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = { Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE };
        if (EasyPermissions.hasPermissions(this, perms)) {

        } else {
            EasyPermissions.requestPermissions(this, "카메라 및 저장공간 사용 권한 필요.", RC_VIDEO_APP_PERM, perms);
        }
    }
    //endregion


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(sceneHealthZone != null)
            sceneHealthZone.onTouchEvent();
        return super.onTouchEvent(event);
    }

    /**
     * 얼굴인증 완료 후 사용자 정보에 바코드 값이 있는 경우
     * @param ucode
     */
    public void initSceneFaceSceneHealthZone(String ucode){

        Intent resultIntent = new Intent(this, ResultWebViewActivity.class);
        resultIntent.putExtra("ucode", ucode);

        Intent myInfoIntent = new Intent(this, MyInfoActivity.class);
        myInfoIntent.putExtra("ucode", ucode);

        sceneHealthZone = new SceneHealthZone(this, resultIntent, myInfoIntent);

        getLayoutHealthHome().setVisibility(View.VISIBLE);
        getSceneHealthZone().StartScene(ucode);
    }

    void getBioDataList(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(HttpTask.URL_DOMAIN)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        HttpTask httpTask = retrofit.create(HttpTask.class);
        httpTask.getBioDataList().enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                JsonArray jsonArray = response.body().getAsJsonArray();
                Log.d("SettingActivity - 로그", "서버로부터 받은 얼굴인식 리스트: "+ jsonArray.toString());

                // 안면인식 데이터 리스트 생성
                for(JsonElement je : jsonArray) {
                    faceDataList.add(je.getAsString()+".dat");
                }

                // 서버로부터 가져온 데이터가 없을 경우
                if(faceDataList.size() == 0){
                    Log.e("MainActivity - 로그 ", "업데이트 / 서버로부터 가져온 바이오 데이터가 없습니다.");
                    return;
                }
                checkListFaceData();
            }

            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                Log.e("MainActivity - 로그 ", "업데이트 / 서버가 불안정하여 회원 안면 인식 데이터 조회에 실패 하였습니다.");
            }
        });
    }


    public void checkListFaceData() {
        // 데이터가 비어있으면 모두 다운로드가 끝났다.
        if (faceDataList == null || faceDataList.isEmpty()) {
            Log.d("MainActivity - 로그 ", "업데이트 / 다운로드 완료.");
            return;
        }

        // 해당파일이 존재시 리스트 항목에서 제거한다.
        File file = new File(String.format("%s/%s", getExternalFilesDir(""), faceDataList.get(0)));
        if(!file.exists()) {
            // 서버로부터 ucode에 해당되는 파일을 다운받는다.
            Log.d("SettingActivity - 로그", "다운받을 예정인 ucode: "+ faceDataList.get(0));
            downloadFaceFile(faceDataList.get(0));
        }
        else {
            Log.d("SettingActivity - 로그", "존재하고 있는 파일: " + faceDataList.get(0));
            removeListData(faceDataList.get(0));
        }
    }


    /**
     * 안면 인식 파일 다운로드
     * @param ucode
     */
    void downloadFaceFile(String ucode) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.connectTimeout(60, TimeUnit.SECONDS);
        httpClient.readTimeout(60, TimeUnit.SECONDS);
        httpClient.writeTimeout(60, TimeUnit.SECONDS);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(HttpTask.URL_DOMAIN)
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        HttpTask downloadService = retrofit.create(HttpTask.class);
        Call<ResponseBody> call = downloadService.downloadFile(URL_BIODATA + ucode);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("MainActivity - 로그", "업데이트 / onResponse isSuccessful");

                    // 파일 다운로드 성공
                    ResponseBody body = response.body();

                    // 파일 저장
                    try {
                        String outPath = String.format("%s/%s", getExternalFilesDir(""), ucode);
                        File file = new File(outPath);
                        BufferedSink sink = Okio.buffer(Okio.sink(file));
                        assert body != null;
                        sink.writeAll(body.source());
                        sink.close();

                        // 다운된 ucode 값 faceDataList에서 제거
                        removeListData(ucode);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // 파일 다운로드 실패
                    Log.e("MainActivity - 로그", "업데이트 / 파일 다운로드 실패: "+ucode);
                    removeListData(ucode);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 파일 다운로드 실패
                Log.e("MainActivity - 로그", "업데이트 / onFailure 파일 다운로드 실패");
            }
        });
    }

    /**
     * 다운로드된 ucode를 faceDataList에서 제거
     * @param ucode
     */
    public void removeListData(String ucode){
        if (faceDataList == null || faceDataList.isEmpty()) {
            Log.d("MainActivity - 로그", "업데이트 / 더 이상 제거할 얼굴 인식 파일이 없습니다."+ucode);
        }
        int removeIdx = faceDataList.indexOf(ucode);
        faceDataList.remove(removeIdx);
        Log.d("SettingActivity - 로그", "지울 데이터 ucode 값: "+ ucode);
        Log.d("SettingActivity - 로그", "지울 데이터 index 값: "+ removeIdx);
        Log.d("SettingActivity - 로그", "남은 faceDataList: "+ faceDataList);

        // 남은 리스트 데이터 호출
        checkListFaceData();
    }
}