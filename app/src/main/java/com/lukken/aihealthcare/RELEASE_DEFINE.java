package com.lukken.aihealthcare;

public interface RELEASE_DEFINE {
    /**
     * 라이센스키
     */
    // String licenseSerialKey = "0597-DB47-CB11-F63A"; // 실사용
    String licenseSerialKey = "44CE-5DFE-D854-55F8"; // new 0419 실사용 키 사용중인
    // String licenseSerialKey = "8209-F4A6-92F2-E4C0"; // 태블릿에 사용

    // String licenseSerialKey = "FBAD-F0FB-9B87-D66B"; // 1순위 예비키
    // String licenseSerialKey = "D262-8634-2C66-CCAB"; // 2순위 예비키
    // String licenseSerialKey = "3113-735D-F476-1A3C";// 개발용


    /**
     * 헬스케어 자동 화면이동 시간
     */
    int AUTO_LOGOUT_TIME = 60;



    /**
     * 카메라 화면 회전
     */
    boolean CAM_ROTATE = true;




    /**
     * 얼굴정보 업데이트 타이머
     */
    //long UPDATE_DATA = 60 * 30 * 1000; 30분마다?
    long UPDATE_DATA = 60 * 10 * 1000; // 위에 코드 기반으로 5분으로 변경
}
