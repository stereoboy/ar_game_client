LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

#OPENCV_CAMERA_MODULES := off
OPENCV_INSTALL_MODULES := off
OPENCV_LIB_TYPE := STATIC

OPENCV_ANDROID_SDK := ${LOCAL_PATH}/../../../../OpenCV-android-sdk

include ${OPENCV_ANDROID_SDK}/sdk/native/jni/OpenCV.mk

LOCAL_MODULE := face_analysis

LOCAL_SRC_FILES := FaceAnalysis_jni.cpp
LOCAL_LDLIBS += -llog -ldl
include $(BUILD_SHARED_LIBRARY)