package com.lukken.aihealthcare;

import static com.lukken.aihealthcare.httpTask.HttpTask.URL_DOMAIN;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ResultWebViewActivity extends AppCompatActivity {
    private WebView mwv;
    private String URL;
    //private String BASE = "http://192.168.0.54:50104/inspection/result/kiosk";
    private String BASE = URL_DOMAIN+"/inspection/result/kiosk";


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        Intent intent = getIntent();
        String ucode = intent.getStringExtra("ucode");

        Log.d("WebViewActivity -  로그", ucode);
        //@ 유저 아이디바꿔야됨
        String query = "/"+ucode;
        Log.d("WebViewActivity query", query);
        URL= BASE + query;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // 가로 고정

        mwv = (WebView)findViewById(R.id.webView);

        // Appbar 종료버튼
        findViewById(R.id.web_back).setOnClickListener(v ->{
            if (mwv.canGoBack()) {
                mwv.goBack();
            } else {
                super.onBackPressed();
            }
        });
        WebSettings mws = mwv.getSettings(); // Mobile Web Setting
        mws.setBuiltInZoomControls(false);   // 확대 축소
        mws.setSupportZoom(false);           // 확대 축소 지원
        mws.setJavaScriptEnabled(true);      // 자바스크립트 허용
        mws.setUseWideViewPort(true);
        mws.setLoadWithOverviewMode(false);   // 컨텐츠가 웹뷰보다 클 경우 스크린 크기에 맞게 조정

        mwv.setInitialScale(50);
        mwv.setWebViewClient(new WebViewClient(){
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

//            @Override
//            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
//                super.onReceivedError(view, request, error);
//
//                Intent intent = new Intent(ResultWebViewActivity.this, ErrorHandlingActivity.class);
//                startActivity(intent);
//                finish();
//            }
        });
        mwv.loadUrl(URL);
        mwv.setWebChromeClient(new WebChromeClient(){
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mwv.canGoBack()) {
                mwv.goBack();
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}