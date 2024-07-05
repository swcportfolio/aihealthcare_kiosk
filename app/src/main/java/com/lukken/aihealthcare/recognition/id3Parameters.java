package com.lukken.aihealthcare.recognition;

import android.hardware.camera2.CameraCharacteristics;

import eu.id3.face.FaceMatcherThreshold;

public class id3Parameters {
    public final static int detectorThreadCount = 4;
    public final static int detectorConfidenceThreshold = 70;
    public final static int encoderThreadCount = 4;
    public final static int encodingQualityThreshold = 40;
    public final static int maxProcessingImageSize = 512;
    public final static FaceMatcherThreshold fmrThreshold = FaceMatcherThreshold.FMR10000;
    //public final static int cameraType = CameraCharacteristics.LENS_FACING_BACK;
    public final static int cameraType = CameraCharacteristics.LENS_FACING_FRONT;
}
