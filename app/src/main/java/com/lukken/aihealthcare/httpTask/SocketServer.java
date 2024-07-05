package com.lukken.aihealthcare.httpTask;

import static com.lukken.aihealthcare.httpTask.HttpTask.URL_BIODATA;


import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;


import com.google.gson.JsonObject;
import com.lukken.aihealthcare.MainActivity;


import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteOrder;
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
import android.content.ContextWrapper;

public class SocketServer {
    private static SocketServer _instance;
    public static SocketServer getInstance(MainActivity activity){
        if(_instance == null)
            _instance = new SocketServer(activity);
        return _instance;
    }
    MainActivity mActivity;
    private SocketServer(MainActivity activity){
        mActivity = activity;
    }

    final static int PORT = 1055;
    ServerSocket serverSocket;
    boolean runSocket;

    public void start(){
        String ip = getIPAddress();
        try {
            serverSocket = new ServerSocket(PORT);
            updateServiceInfo(ip);
            runSocket = true;
            new Thread(() -> runService()).start();
        } catch (Exception e){
        }
    }

    public void stop(){
        runSocket = false;
    }

    /**
     * 회원가입 테블릿으로부터 가입완료 메시지 수신
     */
    void runService(){
        while(runSocket) {
            try {
                Socket socket = serverSocket.accept();
                ObjectInputStream instream = new ObjectInputStream(socket.getInputStream());

                //가입완료자 ucode수신, 얼굴정보 다운로드
                Object obj = instream.readObject();
                // mActivity.URLDownloading(obj.toString() + ".dat");
                downloadFaceFile(obj.toString() + ".dat");

                //응답
                ObjectOutputStream outstream = new ObjectOutputStream(socket.getOutputStream());
                outstream.writeObject("SUCCESS");
                outstream.flush();

                socket.close();
            } catch (Exception e){
                Log.d("소켓 데이터 받기 에러", ""+e);
            }
        }
    }

    /**
     * 소켓정보 서버저장
     * @param address
     */
    void updateServiceInfo(String address){
        Retrofit retrofit = new Retrofit.Builder().baseUrl(HttpTask.URL_DOMAIN).addConverterFactory(GsonConverterFactory.create()).build();
        HttpTask httpTask = retrofit.create(HttpTask.class);
        httpTask.postSetSocketInfo(address, PORT).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
            }
        });
    }


    String getIPAddress(){
        WifiManager wifiManager = (WifiManager)mActivity.getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        if(ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN))
            ipAddress = Integer.reverseBytes(ipAddress);
        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String address;
        try{
            address = InetAddress.getByAddress(ipByteArray).getHostAddress();
        }catch(Exception e){
            address = "";
        }
        return address;
    }

    /**
     * 안면 인식 파일 다운로드
     * @param ucode
     */
    void downloadFaceFile(String ucode) {
        File file = new File(String.format("%s/%s", mActivity.getExternalFilesDir(""), ucode));
        if(file.exists()) {
            Log.d("SocketServer - 로그", "이미 저장되어 있습니다. ucode: "+ ucode);
            return;
        }
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
                    Log.d("SocketServer - 로그", "onResponse isSuccessful");

                    // 파일 다운로드 성공
                    ResponseBody body = response.body();

                    // 파일 저장
                    try {
                        String outPath = String.format("%s/%s", mActivity.getExternalFilesDir(""), ucode);
                        File file = new File(outPath);
                        BufferedSink sink = Okio.buffer(Okio.sink(file));
                        assert body != null;
                        sink.writeAll(body.source());
                        sink.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d("SocketServer - 로그", "파일 다운로드 실패: "+ucode);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 파일 다운로드 실패
                Log.d("SocketServer - 로그", "onFailure 파일 다운로드 실패");
            }
        });
    }
}
