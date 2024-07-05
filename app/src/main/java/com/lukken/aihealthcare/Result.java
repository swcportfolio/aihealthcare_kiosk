package com.lukken.aihealthcare;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;


public class Result implements Serializable {
    @SerializedName("inspectionIdx")
    String inspectionIdx;
    @SerializedName("deviceID")
     String deviceID;
    @SerializedName("deviceName")
     String deviceName;
    @SerializedName("inspectionDate")
     String inspectionDate;

    // getter and setter methods
}