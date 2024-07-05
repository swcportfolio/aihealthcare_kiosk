package com.lukken.aihealthcare.Scene;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lukken.aihealthcare.HealthMenuActivity;
import com.lukken.aihealthcare.MainActivity;
import com.lukken.aihealthcare.R;
import com.lukken.aihealthcare.RELEASE_DEFINE;
import com.lukken.aihealthcare.ResultActivity;
import com.lukken.aihealthcare.httpTask.HttpTask;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SceneHealthZone {
    MainActivity activity;

    Intent resultIntent;
    Intent myInfoIntent;

    public SceneHealthZone(MainActivity a,  Intent resultIntent, Intent myInfoIntent){
        activity = a;
        this.resultIntent = resultIntent;
        this.myInfoIntent = myInfoIntent;

        String code = myInfoIntent.getStringExtra("ucode");

        if(code != null){
            Log.i("SceneHealthZone Intent", code);
        } else {
            Log.i("SceneHealthZone Intent", "code NULL");
        }

        Init();
    }

    String mUCode;
    String mBarcode;
    String mName;
    String mMobile;


    ImageButton btnBack;
    ImageButton btnHome;
    View layoutHome;
    View layoutDisease;

    TextView txtMemberInfo;
    TextView tv_id;
    RecyclerView recyclerViewDisease;
    DiseaseAdp diseaseAdp;
    ConstraintLayout health_top, menu_health;
    ArrayList<GHealthInfo> mGHealthInfos = new ArrayList<>();

    boolean mStartTimer = false;
    Timer mLogoutTimer = null;
    int mLogoutTime = 0;

    @SuppressLint("ClickableViewAccessibility")
    void Init(){
        health_top = activity.findViewById(R.id.health_top);
        menu_health = activity.findViewById(R.id.menu_health);

        btnBack = activity.findViewById(R.id.health_back);
        btnHome = activity.findViewById(R.id.health_home);
        layoutHome = activity.findViewById(R.id.helth_main);
        layoutDisease = activity.findViewById(R.id.helth_disease);
        txtMemberInfo = activity.findViewById(R.id.member_info);
        tv_id = activity.findViewById(R.id.tv_id);
        recyclerViewDisease = activity.findViewById(R.id.disease_list);

        //홈버튼
        activity.findViewById(R.id.health_home).setOnClickListener(v -> {
            activity.getLayoutHome().setVisibility(View.VISIBLE);
            activity.getLayoutHealthHome().setVisibility(View.GONE);
            mStartTimer = false;
        });
        //뒤로가기 버튼
        activity.findViewById(R.id.health_back).setOnClickListener(v -> {
            setHomeUI();
            mLogoutTime = 0;
        });

        //나의 건강정보 조회
        activity.findViewById(R.id.menu1).setOnClickListener(v ->{
//            if(mBarcode.length() > 0) {
//                diseaseAdp = new DiseaseAdp(activity, mGHealthInfos);
//                recyclerViewDisease.setAdapter(diseaseAdp);
//                recyclerViewDisease.setLayoutManager(new LinearLayoutManager(activity));
//                setDiseaseListUI();
//            } else {
//                activity.showAlert("GHealth가입 회원만 이용 가능합니다.", 1500, null);
//            }
//            mLogoutTime = 0;

            // 0614
            // 나의 건강 정보 기능 추기
            activity.startActivity(myInfoIntent);
        });

        //AI헬스케어 체험존 이용하기
        activity.findViewById(R.id.menu2).setOnClickListener(v ->{
            activity.startActivity(resultIntent);
        });

        recyclerViewDisease.setOnTouchListener((v, event) -> {
            mLogoutTime = 0;
            return false;
        });

        //자동 로그아웃
        mStartTimer = false;
        mLogoutTimer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if(mStartTimer) {
                    mLogoutTime++;
                    if (mLogoutTime > RELEASE_DEFINE.AUTO_LOGOUT_TIME) {
                        mStartTimer = false;
                        activity.runOnUiThread(() -> {
                            activity.getLayoutHome().setVisibility(View.VISIBLE);
                            activity.getLayoutHealthHome().setVisibility(View.GONE);
                        });
                    }
                }
            }
        };
        // 00 두개 더 붙임
        mLogoutTimer.schedule(timerTask, 0, 10000000);
    }

    public void Destroy(){
        mLogoutTimer.cancel();
        mLogoutTimer = null;
    }

    public void onTouchEvent(){
        mLogoutTime = 0;
    }

    public void StartScene(String ucode){

        mUCode = ucode;
        txtMemberInfo.setText("");
        setHomeUI();
        setUserInfo(ucode);

        mLogoutTime = 0;
        mStartTimer = true;
    }

    /**
     * 홈화면
     */
    void setHomeUI(){
        menu_health.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.GONE);
        btnHome.setVisibility(View.VISIBLE);
        layoutHome.setVisibility(View.VISIBLE);
        layoutDisease.setVisibility(View.GONE);
        health_top.setVisibility(View.GONE);
    }

    /**
     * 유전체 질환 예측결과 화면
     */
    void setDiseaseListUI(){
        menu_health.setVisibility(View.GONE);
        health_top.setVisibility(View.VISIBLE);
        btnHome.setVisibility(View.GONE);
        layoutHome.setVisibility(View.GONE);
        layoutDisease.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);
    }

    void setUserInfo(String ucode){
        Retrofit retrofit = new Retrofit.Builder().baseUrl(HttpTask.URL_DOMAIN).addConverterFactory(GsonConverterFactory.create()).build();
        HttpTask httpTask = retrofit.create(HttpTask.class);
        httpTask.getGHealth(ucode).enqueue(new Callback<JsonObject>(){
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                int code = response.body().get("code").getAsInt();
                Log.d("회원 정보 조회", response.body().toString());

                if(code == 200) {
                    mName = response.body().get("name").getAsString();
                    mMobile = response.body().get("mobile").getAsString();
                    mBarcode = response.body().get("barcode").getAsString();
                    mGHealthInfos.clear();
                    JsonArray ghealth = response.body().get("ghealth").getAsJsonArray();

                    for (int idx = 0; idx < ghealth.size(); ++idx) {
                        JsonObject obj = ghealth.get(idx).getAsJsonObject();
                        String traitnm = obj.get("traitnm").getAsString();
                        String catnm = obj.get("catnm").getAsString();
                        String value2 = obj.get("value2").getAsString();
                        String value5 = obj.get("value5").getAsString();
                        mGHealthInfos.add(new GHealthInfo(traitnm, catnm, value2, value5));
                    }

                    activity.runOnUiThread(() -> {
                        //txtMemberInfo.setText(mName+"님"+" (" +ucode+ ")");
                        String txt = mName+"님 환영합니다.";
                        SpannableStringBuilder builder = new SpannableStringBuilder(txt);
                        builder.setSpan(new ForegroundColorSpan(Color.parseColor("#3CB6CE")), 0, mName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        txtMemberInfo.setText(builder);
                        tv_id.setText(ucode);
                    });


                } else {
                    new AlertDialog.Builder(activity).setMessage("회원 정보 조회에 실패 하였습니다.").setPositiveButton("확인", (dialog, which) -> {
                        activity.runOnUiThread(() -> {
                            activity.getLayoutHome().setVisibility(View.VISIBLE);
                            activity.getLayoutHealthHome().setVisibility(View.GONE);
                        });
                    }).setCancelable(false).show();
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                new AlertDialog.Builder(activity).setMessage("회원 정보 조회에 실패 하였습니다.").setPositiveButton("확인", (dialog, which) -> {
                    activity.runOnUiThread(() -> {
                        activity.getLayoutHome().setVisibility(View.VISIBLE);
                        activity.getLayoutHealthHome().setVisibility(View.GONE);
                    });
                }).setCancelable(false).show();
            }
        });
    }


    //gHealth 정보
    public static class GHealthInfo{
        String traitnm;     //항목명
        String catnm;       //카테고리명
        String value2;      //항목내용
        String value5;      //상태(danger:주의,info:좋음, success:보통)
        public GHealthInfo(String traintnm, String catnm, String value2, String value5){
            this.traitnm = traintnm;
            this.catnm = catnm;
            this.value2 = value2;
            this.value5 = value5;
        }

        public String getTraitnm() {
            return traitnm;
        }

        public String getCatnm() {
            return catnm;
        }

        public String getValue2() {
            return value2;
        }

        public String getValue5() {
            return value5;
        }
    }
}
