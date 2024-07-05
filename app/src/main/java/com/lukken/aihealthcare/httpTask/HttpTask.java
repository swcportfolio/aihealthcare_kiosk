package com.lukken.aihealthcare.httpTask;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lukken.aihealthcare.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface HttpTask {
    //String URL_DOMAIN = "http://lukken.cafe24.com";

    // 내부 로컬
    //String URL_DOMAIN = "http://192.168.0.9:50104";

    String URL_DOMAIN = "http://lifelogop.ghealth.or.kr";// 외부 접속 가능
    // String URL_DOMAIN = "http://onwards.iptime.org:50003";// 외부 접속 가능
    // String URL_DOMAIN = "http://lifelogop.ghealth.or.kr";
    String URL_BIODATA = URL_DOMAIN + "/ws/biodata/file/";

    //저장된 데이터 가져오기 - 날짜 선택
    @GET("ws/inspection/history")
    Call<ArrayList<Result>> getInspectionHistory(@Query("userID") String userID);


    /**
     * 신규가입 유저키 생성
     */
    @GET("ws/ucode/gen")
    Call<JsonObject> getNewUCode();


    /**
     * 가입가능한 번호인지 체크
     * @param mobile 휴대폰 번호
     * @return
     */
    @GET("ws/mobile/check")
    Call<JsonObject> getCheckMobile(@Query("mobile") String mobile);


    /**
     * 회원가입
     * @param param
     * @param biodata
     * @return
     */
    @Multipart
    @POST("ws/account/create")
    Call<JsonObject> postCreateAccount(@PartMap HashMap<String, RequestBody> param, @Part MultipartBody.Part biodata);


    /**
     * 회원 정보
     * @param ucode
     * @return
     */
    @GET("ws/userinfo")
    Call<JsonObject> getGHealth(@Query("ucode") String ucode);


    /**
     * 얼굴정보 데이터 리스트
     * @return
     */
    @GET("ws/biodata/get")
    Call<JsonArray> getBioDataList();


    /**
     * 서버정보 저장
     * @param address
     * @param port
     * @return
     */
    @FormUrlEncoded
    @POST("ws/sockinfo/set")
    Call<JsonObject> postSetSocketInfo(@Field("address") String address, @Field("port") int port);


    /**
     *  전화 인증
     * @param tel - 휴대폰 번호
     * @param authCode - 패스워드
     * @return
     */
    @FormUrlEncoded
    @POST("ws/user/login")
    Call<JsonObject> postCommonLogin(@Field("tel") String tel, @Field("authCode") String authCode);


 /**
  *  휴대폰 인증
  * @param tel - 휴대폰 번호
  * @return
  */
 @FormUrlEncoded
 @POST("ws/send/auth/message")
 Call<JsonObject> postSendAuth(@Field("tel") String tel);

    /**
     *  얼굴 인증 파일 다운로드
     * @param url
     * @return
     */
 @Streaming
 @GET
 Call<ResponseBody> downloadFile(@Url String url);

}
