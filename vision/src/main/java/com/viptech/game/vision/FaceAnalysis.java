package com.viptech.game.vision;

import android.graphics.Bitmap;

/**
 * Created by rofox on 1/3/17.
 */

public class FaceAnalysis {
    static {
        System.loadLibrary("face_analysis");
    }
    public FaceAnalysis(String datapath, int minFaceSize) {
        mNativeObj = nativeCreateObject(datapath, minFaceSize);
    }

    public void start() {
        nativeStart(mNativeObj);
    }

    public void stop() {
        nativeStop(mNativeObj);
    }

    public void setMinFaceSize(int size) {
        nativeSetFaceSize(mNativeObj, size);
    }

    /*
        return first detected face only
        int array of (x, y)
        size = len(array)/2
     */
    public int[] detect(Bitmap input) {
        int[] face_points = nativeDetect(mNativeObj, input);
        return face_points;
    }

    public void release() {
        nativeDestroyObject(mNativeObj);
        mNativeObj = 0;
    }

    private long mNativeObj = 0;

    private static native long nativeCreateObject(String datapath, int minFaceSize);
    private static native void nativeDestroyObject(long thiz);
    private static native void nativeStart(long thiz);
    private static native void nativeStop(long thiz);
    private static native void nativeSetFaceSize(long thiz, int size);
    private static native int[] nativeDetect(long thiz, Bitmap inputImage);
}
