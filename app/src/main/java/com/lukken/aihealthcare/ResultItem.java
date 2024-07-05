package com.lukken.aihealthcare;

public class ResultItem {
    String imageName;
    String deviceName;
    String date;

    public ResultItem(String imageName, String deviceName, String date) {
        this.imageName = imageName;
        this.deviceName = deviceName;
        this.date = date;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
