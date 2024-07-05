package com.lukken.aihealthcare;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

// 웹뷰 에러 핸들링
public class ErrorHandlingActivity extends AppCompatActivity {


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_handling);

        findViewById(R.id.home_comeback).setOnClickListener(view -> {
            this.finish();
        });

    }
}