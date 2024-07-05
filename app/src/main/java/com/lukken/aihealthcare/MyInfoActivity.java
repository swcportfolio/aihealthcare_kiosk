package com.lukken.aihealthcare;

import static com.lukken.aihealthcare.httpTask.HttpTask.URL_DOMAIN;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MyInfoActivity extends AppCompatActivity {
    private WebView mwv;
    private String URL;
    //private String BASE = "https://lifelogop.ghealth.or.kr/myhealth/U00000";
    private String BASE = "https://lifelogop.ghealth.or.kr/myhealth";


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_info);

        Intent intent = getIntent();
        String ucode = intent.getStringExtra("ucode");

        Log.d("MyInfoActivity -  로그", ucode);
        String query = "/"+ucode;
        Log.d("MyInfoActivity query", query);
        URL = BASE + query;

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
//                Intent intent = new Intent(MyInfoActivity.this, ErrorHandlingActivity.class);
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