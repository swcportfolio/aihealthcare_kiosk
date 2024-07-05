package com.lukken.aihealthcare.Scene;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.lukken.aihealthcare.MainActivity;
import com.lukken.aihealthcare.R;
import com.lukken.aihealthcare.httpTask.HttpTask;

import java.io.File;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SceneJoin {
    MainActivity activity;
    public SceneJoin(MainActivity a){
        activity = a;
        Init();
    }

    EditText editName;
    EditText editPhone;
    EditText editBirth;
    View currentEdit = null;
    RadioButton rbGenderM;
    RadioButton rbGenderF;

    String mUCode;

    boolean mAddFace = false;
    public void setAddFace(boolean f){ mAddFace = f; }

    @SuppressLint("ClickableViewAccessibility")
    void Init(){
        editName = activity.findViewById(R.id.name);
        editPhone = activity.findViewById(R.id.phone);
        editBirth = activity.findViewById(R.id.birth);
        rbGenderM = activity.findViewById(R.id.male);
        rbGenderF = activity.findViewById(R.id.female);
        editName.setOnFocusChangeListener(focusChangeListener);
        editPhone.setOnFocusChangeListener(focusChangeListener);
        editBirth.setOnFocusChangeListener(focusChangeListener);

        //화면영역 터치시  키보드 숨김
        activity.getLayoutJoin().setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });
        activity.findViewById(R.id.scroll).setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });
        rbGenderM.setOnClickListener(v -> hideKeyboard());
        rbGenderF.setOnClickListener(v -> hideKeyboard());

        activity.findViewById(R.id.jo_back).setOnClickListener(v -> {
            hideKeyboard();
            activity.getLayoutHome().setVisibility(View.VISIBLE);
            activity.getLayoutJoin().setVisibility(View.GONE);
            activity.getSceneFace().getFaceProcessor().deleteTemplate(mUCode);
        });

        activity.findViewById(R.id.add_face).setOnClickListener(v -> {
            SetAddFace();
        });

        activity.findViewById(R.id.join).setOnClickListener(v -> {
            hideKeyboard();
            if(checkInputData()){
                checkMobile(true);
            }
        });
    }

    /**
     * 회원가입신으로 변경
     */
    public void StartScene(){
        mUCode = "";
        editName.setText("");
        editPhone.setText("");
        editBirth.setText("");
        rbGenderM.setChecked(false);
        activity.getLayoutHome().setVisibility(View.GONE);
        activity.getLayoutJoin().setVisibility(View.VISIBLE);
    }

    /**
     * 얼굴등록 화면 변경
     */
    void SetAddFace(){
        hideKeyboard();
        if(checkInputData()) {
            checkMobile(false);
        }
    }

    /**
     * 회원가입 정보 체크
     */
    boolean checkInputData(){
        String name = editName.getText().toString();
        if(name.length() == 0){
            activity.showAlert("사용자명을 입력해주세요.", 1500, null);
            return false;
        }

        String mobile = editPhone.getText().toString();
        if(mobile.length() < 10){
            activity.showAlert("휴대폰 번호를 입력해주세요.", 1500, null);
            return false;
        } else if(!isValidPhoneNumber(mobile)){
            activity.showAlert("휴대폰 번호 형식에 맞춰 입력해주세요.", 1500, null);
            return false;
        }

        String birth = editBirth.getText().toString();
        if(birth.length() < 8){
            activity.showAlert("출생 년도를 입력해주세요. ex)20001234", 1500, null);
            return false;
        } else if(!isValidDateOfBirth(birth)){
            activity.showAlert("출생년도 형식에 맞춰서 입력해주세요. ex)20001234", 1500, null);
            return false;
        }
        if(!rbGenderM.isChecked() && !rbGenderF.isChecked()){
            activity.showAlert("성별을 선택해 주세요.", 1500, null);
            return false;
        }
        return true;
    }

    // 전화번호 유효성 검사
    public boolean isValidPhoneNumber(String phoneNumber) {
        // 휴대폰 번호 패턴을 정의합니다.
        Pattern pattern = Pattern.compile("^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$");
        // 입력한 번호를 패턴과 비교합니다.
        Matcher matcher = pattern.matcher(phoneNumber);
        return matcher.matches();
    }

    // 생년월일 유효성 검사
    public boolean isValidDateOfBirth(String dateOfBirth) {
        // 생년월일 패턴을 정의합니다.
        Pattern pattern = Pattern.compile("^\\d{4}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])$");
        // 입력한 생년월일을 패턴과 비교합니다.
        Matcher matcher = pattern.matcher(dateOfBirth);
        if (!matcher.matches()) {
            return false;
        }
        // 날짜가 유효한지 검사합니다.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setLenient(false);
        try {
            Date date = sdf.parse(dateOfBirth);
            Log.i("date", date.toString());
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
        if(hasFocus)
            currentEdit = v;
    };



    //region HTTP TASK

    /**
     * 신규 유저 코드 생성
     */
    void getUCode(){
        Retrofit retrofit = new Retrofit.Builder().baseUrl(HttpTask.URL_DOMAIN).addConverterFactory(GsonConverterFactory.create()).build();
        HttpTask httpTask = retrofit.create(HttpTask.class);
        httpTask.getNewUCode().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                mUCode = response.body().get("ucode").getAsString();
                if(mUCode == null || mUCode.length() == 0){
                    new AlertDialog.Builder(activity).setMessage("신규 유저코드 생성에 실패 하였습니다.").setPositiveButton("확인", (dialog, which) -> {
                        hideKeyboard();
                        activity.getLayoutHome().setVisibility(View.VISIBLE);
                        activity.getLayoutJoin().setVisibility(View.GONE);
                    }).setCancelable(false).show();
                } else {
                    createAccount();
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                new AlertDialog.Builder(activity).setMessage("신규 유저코드 생성에 실패 하였습니다.").setPositiveButton("확인", (dialog, which) -> {
                    hideKeyboard();
                    activity.getLayoutHome().setVisibility(View.VISIBLE);
                    activity.getLayoutJoin().setVisibility(View.GONE);
                }).setCancelable(false).show();
            }
        });
    }

    /**
     * 가입가능한 휴대폰 번호인지 체크
     * @param isCreateAccount true-신규 생성 false-얼굴등록
     */
    void checkMobile(final boolean isCreateAccount){
        Retrofit retrofit = new Retrofit.Builder().baseUrl(HttpTask.URL_DOMAIN).addConverterFactory(GsonConverterFactory.create()).build();
        HttpTask httpTask = retrofit.create(HttpTask.class);
        httpTask.getCheckMobile(editPhone.getText().toString()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                int code = response.body().get("code").getAsInt();
                if(code == 201){
                    new AlertDialog.Builder(activity).setMessage("이미 가입된 휴대폰번호 입니다..").setPositiveButton("확인", (dialog, which) -> {
                        hideKeyboard();
                        activity.getLayoutHome().setVisibility(View.VISIBLE);
                        activity.getLayoutJoin().setVisibility(View.GONE);
                    }).setCancelable(false).show();
                }else{
                    if(isCreateAccount){
                        if(!mAddFace){
                            new AlertDialog.Builder(activity).setMessage("안면등록을 해 주세요..").setPositiveButton("확인", null).setCancelable(false).show();
                        }else {
                            getUCode();
                        }
                    }else {
                        mAddFace = false;
                        activity.getLayoutJoin().setVisibility(View.GONE);
                        activity.getSceneFace().StartScene(mUCode, SceneFace.RECOGNITION_TYPE.eFace_Add);
                    };
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                new AlertDialog.Builder(activity).setMessage("휴대폰번호 체크 오류.").setPositiveButton("확인", (dialog, which) -> {
                    hideKeyboard();
                    //activity.getLayoutHome().setVisibility(View.VISIBLE);
                    //activity.getLayoutJoin().setVisibility(View.GONE);
                }).setCancelable(false).show();
            }
        });
    }

    void createAccount(){
        RequestBody ucode = RequestBody.create(MediaType.parse("multipart/form-data"), mUCode);
        RequestBody name = RequestBody.create(MediaType.parse("multipart/form-data"), editName.getText().toString());
        RequestBody mobile = RequestBody.create(MediaType.parse("multipart/form-data"), editPhone.getText().toString());
        RequestBody birth_y = RequestBody.create(MediaType.parse("multipart/form-data"), editBirth.getText().toString());
        RequestBody gender = RequestBody.create(MediaType.parse("multipart/form-data"), rbGenderM.isChecked() ? "M" : "F");

        String fileName = mUCode + ".dat";
        File file = new File(activity.getExternalFilesDir("").getPath() + "/" + fileName);
        MultipartBody.Part biodata = MultipartBody.Part.createFormData("biodata", fileName, RequestBody.create(MediaType.parse("application/octet-stream"), file));

        HashMap<String, RequestBody> map = new HashMap<>();
        map.put("ucode", ucode);
        //@ucode issue: 키오스크, 운용자에서 동시에 회원가입 화면으로 넘어가면 동시에 같은 코드가 발급됨
        // 예외처리가 필요하다.
        map.put("name", name);
        map.put("mobile", mobile);
        map.put("birth_y", birth_y);
        map.put("gender", gender);

        Retrofit retrofit = new Retrofit.Builder().baseUrl(HttpTask.URL_DOMAIN).addConverterFactory(GsonConverterFactory.create()).build();
        HttpTask httpTask = retrofit.create(HttpTask.class);
        httpTask.postCreateAccount(map, biodata).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                int code = response.body().get("code").getAsInt();
                if(code == 200) {
                    new AlertDialog.Builder(activity).setMessage("회원가입이 완료되었습니다.").setPositiveButton("확인", (dialog, which) -> {
                        hideKeyboard();
                        activity.getLayoutHome().setVisibility(View.VISIBLE);
                        activity.getLayoutJoin().setVisibility(View.GONE);
                    }).setCancelable(false).show();
                } else {
                    activity.showAlert("회원가입 실패.", 1500, null);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                new AlertDialog.Builder(activity).setMessage("회원가입 오류.\n"+t.getMessage()).setPositiveButton("확인", null).setCancelable(false).show();
            }
        });
    }
    //endregion



    void hideKeyboard(){
        if(currentEdit != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(currentEdit.getWindowToken(), 0);
        }
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
