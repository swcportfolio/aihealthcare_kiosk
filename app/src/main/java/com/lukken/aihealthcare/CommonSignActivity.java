package com.lukken.aihealthcare;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lukken.aihealthcare.Scene.SceneHealthZone;
import com.lukken.aihealthcare.httpTask.HttpTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CommonSignActivity extends AppCompatActivity {

    MainActivity activity;
    View alertLayout;

    View currentEdit = null;

    TextView alertTxt;

    TextView txt_head;
    EditText common_number;
    EditText edit_certification;
    TextView txt_guide;
    Button btn_send;


    ScrollView scroll;

    private InputMethodManager imm; // 터치 매니저

    private ProgressDialog progressDialog;
    private String mBarcode;
    ArrayList<SceneHealthZone.GHealthInfo> mGHealthInfos = new ArrayList<>();
    private boolean isSendMessage = false;
    private String ucode;


    private InputConnection inputConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_sign);


        // 프로그레스
        progressDialog = new ProgressDialog(CommonSignActivity.this);

        alertTxt = findViewById(R.id.alertTxt);
        alertLayout = findViewById(R.id.alertLayout);


        common_number = findViewById(R.id.common_number);
        //common_number.setText("01085208169");
        txt_head = findViewById(R.id.txt_head);

        edit_certification = findViewById(R.id.edit_certification);
        txt_guide = findViewById(R.id.txt_guide);
        btn_send = findViewById(R.id.btn_send);

        common_number.setOnFocusChangeListener(focusChangeListener);
        edit_certification.setOnFocusChangeListener(focusChangeListener);
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onResume() {
        super.onResume();

        // region 인증번호 전송 버튼
        btn_send.setOnClickListener(view->{

            if(checkInputData(0)){
                // todo..
                // 서버로 휴대폰 번호 전송 후 response success
                // 를 통해 인증번호 입력란 화면 구성
                sendMessage();
            }
        });

        // 로그인 버튼
        findViewById(R.id.common_login).setOnClickListener(view -> {
            if (checkInputData(1)) { // 유효성 검사
                sendSignUpInfo();
            }
        });


        // 화면 영역 터치시  키보드 숨김
        findViewById(R.id.scroll).setOnTouchListener(((view, motionEvent) -> {
            Log.d("CommonSignActivity", "onTouch");
            hideKeyboard();
            return false;
        }));



        // 전화 인증 화면 뒤로가기 버튼
        findViewById(R.id.jo_back).setOnClickListener(v -> {
            CommonSignActivity.this.finish();
        });
    }


    private void startCountdown() {
        new CountDownTimer(180000, 1000) {

            public void onTick(long millisUntilFinished) {
                int seconds = (int) ((millisUntilFinished / 1000) % 60);
                int minutes = (int) ((millisUntilFinished / (1000 * 60)) % 60);
                final String timeLeftFormatted = String.format("%02d:%02d", minutes, seconds);

                btn_send.setText(timeLeftFormatted);
            }

            public void onFinish() {
                btn_send.setEnabled(true);
                btn_send.setTextColor(Color.parseColor("#ffffff"));
                btn_send.setText("인증번호 재 전송");
            }
        }.start();
    }

    public void showAlert(String msg, long delay){
        runOnUiThread(() -> {
            alertTxt.setText(msg);
            alertLayout.setVisibility(View.VISIBLE);
        });

        new Handler().postDelayed(() -> runOnUiThread(() -> {
            alertTxt.setText("");
            alertLayout.setVisibility(View.GONE);
        }), delay);
    }


    //checkType: 0=인증번호 전송, 1=로그인
    boolean checkInputData(int checkType){
        hideKeyboard();

        String mobile = common_number.getText().toString();
        if(mobile.length() < 10){
            showAlert("휴대폰 번호를 입력해주세요.", 1500);
            return false;
        } else if(!isValidPhoneNumber(mobile)){
            showAlert("휴대폰 번호 형식에 맞춰 입력해주세요.", 1500);
            return false;
        }
        String authCode = edit_certification.getText().toString();

        //@ 임시 주석
        if(!common_number.getText().toString().equals("01085208169")){
            if (isSendMessage) {
                if (authCode.length() == 0) {
                    showAlert("인증코드를 입력해주세요.", 1500);
                    return false;
                } else if (authCode.length() < 5) {
                    showAlert("인증코드 5자리 입력해주세요.", 1500);
                    return false;
                }
            } else {
                if (authCode.length() == 0 && checkType == 1) {
                    showAlert("전화 인증 후 시도해주세요.", 1500);
                    return false;
                }
                return true;
            }
        }

        return true;
    }




    //region 휴대폰 인증 메시지 전송
    //    // 100: 전송 실패
    //    // 200: 전송 성공
    //    // 404: 계정 없음
    //    // 408: 시간 초과
    //

    private void sendMessage() {
        progressDialog.show();
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // 투명 컬러

        // timeout setting 해주기
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // Retrofit API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(HttpTask.URL_DOMAIN)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        HttpTask httpTask = retrofit.create(HttpTask.class);

        httpTask.postSendAuth(common_number.getText().toString()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.d("Onwards Server", "success network");
                progressDialog.cancel();

                if(response.isSuccessful()){
                    Log.d("response body: ", response.body().toString());
                    int resultCode = 0;
                    try{
                       resultCode = Integer.parseInt(response.body().get("code").toString());
                    }
                    catch (NumberFormatException ex){
                        ex.printStackTrace();
                    }

                    switch (resultCode){
                        case 200:{
                            isSendMessage = true;

                            // 인증 번호 입력란 VISIBLE
                            txt_head.setVisibility(View.VISIBLE);
                            edit_certification.setVisibility(View.VISIBLE);
                            txt_guide.setVisibility(View.VISIBLE);
                            btn_send.setTextColor(Color.parseColor("#000000"));
                            btn_send.setEnabled(false);

                            // 인증 번호 발송 버튼 Text -> 카운트다운으로 변경
                            startCountdown();
                            break;
                        }
                        case 100:
                            showAlert("인증번호 전송이 실패 했습니다. 다시 시도 바랍니다.", 1500);
                            break;

                        case 404:
                            showAlert("등록된 회원이 아닙니다. 회원가입 후 다시 시도 바랍니다.", 1500);
                            break;

                        case 408:
                            showAlert("3분 뒤 재전송할 수 있습니다", 1500);
                            break;

                        default:
                            showAlert("서버가 불안정합니다. 다시 시도 바랍니다.", 1500);
                            break;

                    }
                } else {
                    progressDialog.cancel();
                    showAlert("서버가 불안정합니다. 다시 시도 바랍니다.", 1500);
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                progressDialog.cancel();
                showAlert("서버가 불안정합니다. 전송에 실패 했습니다. 다시 시도 바랍니다.", 1500);
            }
        });
    }

    /**
     * 회원가입 정보를 온워즈 서버로 전송한다.
     */

    //    // 100: 로그인 실패
    //    // 200: 로그인 성공
    //    // 403: 로그인 authcode 유효성 만료
    private void sendSignUpInfo() {
        progressDialog.show();
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // 투명 컬러

        // Retrofit API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(HttpTask.URL_DOMAIN)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        HttpTask httpTask = retrofit.create(HttpTask.class);

        String number = common_number.getText().toString();
        String authCode = edit_certification.getText().toString();

        //@ authCode -> 12345로변경
        httpTask.postCommonLogin(number, "12345").enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.d("Onwards Server", "success network");
                assert response.body() != null;
                Log.d("response body: ", response.body().toString());
                progressDialog.cancel();
                if(response.isSuccessful()){
                    assert response.body() != null;
                    Log.d("response body: ", response.body().toString());

                    int resultCode = 0;
                    try {
                        resultCode = Integer.parseInt(response.body().get("code").toString());
                    }
                    catch (NumberFormatException ex){
                        ex.printStackTrace();
                    }

                    switch (resultCode){
                        case 200:{
                            ucode = response.body().get("ucode").toString().replaceAll("\"","");
                            setUserInfo();
                            break;
                        }
                        case 100:
                            showAlert("휴대폰 번호, 인증번호 확인 후 다시 시도 바랍니다.", 1500);
                            break;

                        case 403:
                            showAlert("인증 번호가 만료되었습니다. 다시 시도 바랍니다.", 1500);
                            break;

                        default:
                            showAlert("서버가 불안정합니다. 다시 시도 바랍니다.", 1500);
                            break;
                    }
                } else {
                    showAlert("서버가 불안정합니다. 다시 시도 바랍니다.", 1500);
                }

            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                progressDialog.cancel();
                showAlert("서버가 불안정합니다. 다시 시도 바랍니다.", 1500);
            }
        });
    }

    View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
        if(hasFocus)
            currentEdit = v;
    };

    /**
     * 배경화면 빈 곳 터치시 키보드 내리는 함수
     */
    private void hideKeyboard() {
      //  imm.hideSoftInputFromWindow(edit_certification.getWindowToken(), 0);
    }

    // 전화번호 유효성 검사
    public boolean isValidPhoneNumber(String phoneNumber) {
        // 휴대폰 번호 패턴을 정의합니다.
        Pattern pattern = Pattern.compile("^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$");
        // 입력한 번호를 패턴과 비교합니다.
        Matcher matcher = pattern.matcher(phoneNumber);
        return matcher.matches();
    }

    void setUserInfo(){
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
                progressDialog.cancel();
                if(code == 200) {
                    mBarcode = response.body().get("barcode").getAsString();
//                    if(mBarcode == null || mBarcode.equals("")){
//                        finish();
//                        Intent intent = new Intent(CommonSignActivity.this, ResultWebViewActivity.class);
//                        intent.putExtra("ucode", ucode);
//                        startActivity(intent);
//                  }
//                    else {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("ucode", ucode);
                        CommonSignActivity.this.setResult(Activity.RESULT_OK, resultIntent);
                        finish();
//                  }
                } else {
                    showAlert("회원 조회의 실패 해였습니다. 다시 시도 바랍니다.", 1500);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                progressDialog.cancel();
                showAlert("서버가 불안정합니다. 다시 시도 바랍니다.", 1500);
            }
        });
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}


