package com.lukken.aihealthcare;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.lukken.aihealthcare.httpTask.HttpTask;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// 결과 리스트 화면
// 리사이클 뷰를 사용하여 13종 결과를 보여준다.
public class ResultActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ResultAdapter resultAdapter;
    private ArrayList<Result> resultItem;
    private String ucode;
    private TextView tv_date;
    private TextView tv_empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        Intent intent = getIntent();
        String code = intent.getStringExtra("ucode");

        getInspectionHistoryList(code);

        tv_date = findViewById(R.id.tv_date);
        tv_empty = findViewById(R.id.tv_empty);
        recyclerView = findViewById(R.id.recyclerview_main_list);

        // Appbar 종료버튼
        findViewById(R.id.im_back).setOnClickListener(v ->finish());
    }

    // 키오스크 분셕 결과 리스트 가져오기
    private void getInspectionHistoryList(String code){

        // Retrofit API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(HttpTask.URL_DOMAIN)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        HttpTask httpTask = retrofit.create(HttpTask.class);
        httpTask.getInspectionHistory("U00001").enqueue(new Callback<ArrayList<Result>>() {
            /**
             * Retrofit 라이브러리를 통한 Restful api 응답 합수, Success network
             * @param call
             * @param response
             */
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(Call<ArrayList<Result>> call, Response<ArrayList<Result>> response) {
                if (response.isSuccessful()) {
                    resultItem = response.body();

                    if(resultItem.size() == 0){
                        recyclerView.setVisibility(View.GONE);
                        tv_empty.setVisibility(View.VISIBLE);
                        return;
                    }

                    for (int i=0 ; i<resultItem.size() ; i++){
                        Log.d("deviceName :", resultItem.get(i).deviceName);
                    }
                    tv_date.setText("측정일: "+resultItem.get(0).inspectionDate);

                    recyclerView.setLayoutManager(new LinearLayoutManager(ResultActivity.this));
                    recyclerView.setLayoutManager(new GridLayoutManager(ResultActivity.this, 3));
                    resultAdapter = new ResultAdapter(ResultActivity.this, resultItem);
                    recyclerView.setAdapter(resultAdapter);

                    resultAdapter.setOnItemClickListener(position -> {
                        Intent intent = new Intent(ResultActivity.this, WebViewActivity.class);
                        intent.putExtra("ucode", code);
                        intent.putExtra("Result", resultItem.get(position));
                        startActivity(intent);
                    });
                    // TODO: 데이터 처리
                } else {
                    // TODO: 에러 처리
                    Log.d("getInspectionHistoryList :", "Error");
                    Toast.makeText(ResultActivity.this, "error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ArrayList<Result>> call, Throwable t) {
                Toast.makeText(ResultActivity.this, "error", Toast.LENGTH_SHORT).show();
                Log.d("getInspectionHistoryList :", "Error");
                // TODO: 에러 처리
            }
        });
    }
}