LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Set up for OpenCV
OPENCV_CAMERA_MODULES := off
#OPENCV_INSTALL_MODULES := off
OPENCV_LIB_TYPE := STATIC

OPENCV_ANDROID_SDK := ${LOCAL_PATH}/../../../../opencv-4_5_5-android-sdk/OpenCV-android-sdk

include ${OPENCV_ANDROID_SDK}/sdk/native/jni/OpenCV.mk

LOCAL_C_INCLUDES += ${OPENCV_ANDROID_SDK}/sdk/native/jni/include

DLIB_SDK_PATH := ../../../../dlib
LOCAL_C_INCLUDES += ${LOCAL_PATH}/../../../../dlib

# reference: https://developer.android.com/ndk/guides/cpu-arm-neon.html
ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI), armeabi-v7a x86))
    LOCAL_ARM_NEON  := true
endif # TARGET_ARCH_ABI == armeabi-v7a || x86

# ADD DLIB
LOCAL_SRC_FILES += \
    ${DLIB_SDK_PATH}/dlib/base64/base64_kernel_1.cpp \

LOCAL_SRC_FILES += \
    ${DLIB_SDK_PATH}/dlib/bigint/bigint_kernel_1.cpp \
    ${DLIB_SDK_PATH}/dlib/bigint/bigint_kernel_2.cpp \

LOCAL_SRC_FILES += \
    ${DLIB_SDK_PATH}/dlib/bit_stream/bit_stream_kernel_1.cpp \

LOCAL_SRC_FILES += \
    ${DLIB_SDK_PATH}/dlib/entropy_decoder/entropy_decoder_kernel_1.cpp \
    ${DLIB_SDK_PATH}/dlib/entropy_decoder/entropy_decoder_kernel_2.cpp \
    ${DLIB_SDK_PATH}/dlib/entropy_encoder/entropy_encoder_kernel_1.cpp \
    ${DLIB_SDK_PATH}/dlib/entropy_encoder/entropy_encoder_kernel_2.cpp \

LOCAL_SRC_FILES += \
    ${DLIB_SDK_PATH}/dlib/threads/multithreaded_object_extension.cpp \
    ${DLIB_SDK_PATH}/dlib/threads/threaded_object_extension.cpp \
    ${DLIB_SDK_PATH}/dlib/threads/threads_kernel_1.cpp \
    ${DLIB_SDK_PATH}/dlib/threads/threads_kernel_2.cpp \
    ${DLIB_SDK_PATH}/dlib/threads/threads_kernel_shared.cpp \
    ${DLIB_SDK_PATH}/dlib/threads/thread_pool_extension.cpp \
    ${DLIB_SDK_PATH}/dlib/threads/async.cpp. \


#LOCAL_SRC_FILES += \
    ${DLIB_SDK_PATH}/dlib/dnn/cpu_dlib.cpp \
    ${DLIB_SDK_PATH}/dlib/dnn/tensor_tools.cpp \


LOCAL_MODULE := face_analysis

LOCAL_SRC_FILES += FaceAnalysis_jni.cpp
LOCAL_LDLIBS += -llog -ldl -ljnigraphics -landroid

include $(BUILD_SHARED_LIBRARY)