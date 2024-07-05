package com.lukken.aihealthcare;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

// 체험존 이용하기
// 바코드 값이 있을경우 4가지 분류 메뉴로 보여준다.
public class HealthMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_menu);
    }
}