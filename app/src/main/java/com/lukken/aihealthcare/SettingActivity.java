package com.lukken.aihealthcare;

import static com.lukken.aihealthcare.httpTask.HttpTask.URL_BIODATA;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.lukken.aihealthcare.httpTask.HttpTask;
import com.lukken.aihealthcare.utils.DownLoadQueueData;
import com.lukken.aihealthcare.utils.LocalDataStore;
import com.lukken.aihealthcare.utils.myDropDown;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// 앱 설치 후 첫 화면 액티비티
// 카메라 선택과, 안면인식 데이터 복구 절차
public class SettingActivity extends AppCompatActivity implements View.OnClickListener{
    Spinner mCISCamListSpinner;
    ArrayList<Integer> mCISCamList;
    UsbManager mUsbManager;

    DownloadManager mDownloadManager;
    ProgressDialog progressDialog;

    TextView logText;
    private int REQUEST_CODE = 100;

    private ArrayList<String> faceDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        onCheckPermission();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Downloading...");
        progressDialog.setCancelable(true);
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Horizontal);

        mCISCamListSpinner = findViewById(R.id.cis_cam_list);
        findViewById(R.id.cis_refresh).setOnClickListener(this);
        findViewById(R.id.save).setOnClickListener(this);
        findViewById(R.id.bio_down).setOnClickListener(this);

        logText = findViewById(R.id.logTextView);

        mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);

        setUSBCameraList();
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

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.cis_refresh){
            setUSBCameraList();
        }
        
        else if(v.getId() == R.id.bio_down){
            getBioDataList();
        }

        else if(v.getId() == R.id.save){
            int select = mCISCamListSpinner.getSelectedItemPosition();
            if(select == -1){
                showSnackBar(v, "설정된 카메라가 없습니다.", Snackbar.LENGTH_SHORT);
                return;
            }
            LocalDataStore localDataStore = LocalDataStore.getInstance(SettingActivity.this);
            int vendorId = mCISCamList.get(select);
            localDataStore.setValueInt(LocalDataStore.SP_OP_CIS_CAM, vendorId);


            //앱 재실행
            PackageManager packageManager = getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
            ComponentName componentName = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(componentName);
            startActivity(mainIntent);
            System.exit(0);
        }
    }

    /**
     * 연결된 USB 리스트
     */
    void setUSBCameraList(){
        try {
            ArrayList<String> camList = new ArrayList<>();
            mCISCamList = new ArrayList<>();
            final HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

            for (final UsbDevice device: deviceList.values() ) {
                String prodName = device.getProductName();
                String manfName = device.getManufacturerName();

                if(prodName == null || prodName.equals("null"))
                    prodName = "";
                if(manfName == null || manfName.equals("null"))
                    manfName = "";
                camList.add(String.format("%s(%s)", prodName, manfName));
                mCISCamList.add(device.getVendorId());
            }

            myDropDown adp = new myDropDown(SettingActivity.this, android.R.layout.simple_spinner_dropdown_item, camList);
            adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mCISCamListSpinner.setAdapter(adp);

            LocalDataStore localDataStore = LocalDataStore.getInstance(SettingActivity.this);
            int vendorid = localDataStore.getValueInt(LocalDataStore.SP_OP_CIS_CAM, -1);
            for(int idx=0; idx<mCISCamList.size(); ++idx){
                if(vendorid == mCISCamList.get(idx)){
                    mCISCamListSpinner.setSelection(idx);
                    break;
                }
            }
        } catch (Exception e) {}
    }

    //@ 0412
    // 안면 인식 데이터 복구 우측 버튼 클릭시 바이오 데이터 리스트를 가져온다.
    Queue<DownLoadQueueData> mDownQueue = new LinkedList<>();


    void StartDownLoad(){
            DownLoadQueueData d = mDownQueue.poll();
            logText.append("mDownQueue size: "+mDownQueue.size()+"\n");
            if(d != null){
                URLDownloading(d.url, d.name);
            } else {
                //progressDialog.dismiss();
                new AlertDialog.Builder(SettingActivity.this).setMessage("다운로드 완료.").setPositiveButton("확인", null).setCancelable(false).show();
            }
    }

    /**
     * 얼굴데이터 다운로드
     */
    void URLDownloading(String url, String filename) {
        if (mDownloadManager == null) {
            mDownloadManager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
        }
            String outpath = String.format("%s/%s", getExternalFilesDir(""), filename);
            logText.append("outpath: "+outpath+"\n");
            File outputFile = new File(outpath);
            Uri downloadUri = Uri.parse(url);
            logText.append("downloadUri: "+downloadUri+"\n");
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.setDestinationUri(Uri.fromFile(outputFile));
            logText.append("request.setDestinationUri: "+"진행"+"\n");
            request.setAllowedOverMetered(true);

            Long qid = mDownloadManager.enqueue(request);
    }

    private final BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            DownloadManager.Query query = new DownloadManager.Query();  // 다운로드 항목 조회에 필요한 정보 포함
            query.setFilterById(reference);
            Cursor cursor = mDownloadManager.query(query);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            //int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
            int status = cursor.getInt(columnIndex);
            //int reason = cursor.getInt(columnReason);
            cursor.close();
            StartDownLoad();
        }
    };

    public void showSnackBar(View view, String alert, int time)
    {
        final Snackbar snackbar = Snackbar.make(view, alert, time);
        snackbar.show();
    }

    void onCheckPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 파일 저장 권한이 허용된 경우 다운로드를 시작합니다.
            } else {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
        } else {
            // 안드로이드 6.0 미만에서는 권한 요청 없이 다운로드를 시작합니다.
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 100: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한이 허용된 경우 다운로드를 시작합니다.
                    Toast.makeText(SettingActivity.this, "권한 허용", Toast.LENGTH_SHORT).show();
                } else {
                    // 권한이 거부된 경우 다운로드를 중단합니다.\
                    Toast.makeText(SettingActivity.this, "권한 거부", Toast.LENGTH_SHORT).show();

                }
                return;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + requestCode);
        }
    }

    /**
     * @psw
     * 1. 안면인식 리스트 API 호출
     */
    void getBioDataList(){
        progressDialog.show();

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
                    new AlertDialog.Builder(SettingActivity.this)
                            .setMessage("서버로부터 가져온 바이오 데이터가 없습니다.")
                            .setPositiveButton("확인", null)
                            .setCancelable(false)
                            .show();
                    return;
                }
                checkListFaceData();
            }

            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                progressDialog.dismiss();
                new AlertDialog.Builder(SettingActivity.this)
                        .setMessage("서버가 불안정하여 회원 안면 인식 데이터 조회에 실패 하였습니다.")
                        .setPositiveButton("확인", null)
                        .setCancelable(false)
                        .show();
            }
        });
    }


    public void checkListFaceData() {
        // 데이터가 비어있으면 모두 다운로드가 끝났다.
        if (faceDataList == null || faceDataList.isEmpty()) {
            progressDialog.dismiss();
            new AlertDialog.Builder(SettingActivity.this)
                    .setMessage("다운로드 완료.")
                    .setPositiveButton("확인", null)
                    .setCancelable(false)
                    .show();
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
                    Log.d("SettingActivity - 로그", "onResponse isSuccessful");

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
                    progressDialog.dismiss();
                    Log.d("SettingActivity - 로그", "파일 다운로드 실패: "+ucode);
//                    new AlertDialog.Builder(SettingActivity.this)
//                            .setMessage(ucode + "/ 파일 다운로드 실패하였습니다.")
//                            .setPositiveButton("확인", null)
//                            .setCancelable(false)
//                            .show();
                    removeListData(ucode);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 파일 다운로드 실패
                progressDialog.dismiss();
                Log.d("SettingActivity - 로그", "onFailure 파일 다운로드 실패");
                new AlertDialog.Builder(SettingActivity.this)
                        .setMessage(ucode + "/ 서버가 불안정하여 다운받지 못했습니다..")
                        .setPositiveButton("확인", null)
                        .setCancelable(false)
                        .show();
            }
        });
    }

    /**
     * 다운로드된 ucode를 faceDataList에서 제거
     * @param ucode
     */
    public void removeListData(String ucode){
        if (faceDataList == null || faceDataList.isEmpty()) {
            new AlertDialog.Builder(SettingActivity.this)
                    .setMessage("더 이상 제거할 얼굴 인식 파일이 없습니다.")
                    .setPositiveButton("확인", null)
                    .setCancelable(false)
                    .show();
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