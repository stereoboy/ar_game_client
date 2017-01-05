//
// Created by rofox on 1/3/17.
//
#include <FaceAnalysis_jni.h>
#include <opencv2/core.hpp>
#include <opencv2/objdetect.hpp>

#include <android/log.h>

#define LOG_TAG "FaceAnalysis"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

#define DBG_PRINT
#ifdef DBG_PRINT
#define DBG_BEGIN() LOGD("%s()\n", __FUNCTION__)
#else
#define DBG_BEGIN()
#endif
JNIEXPORT jlong JNICALL JNI_PREFIX(nativeCreateObject)(JNIEnv *, jclass, jstring, jint){
    DBG_BEGIN();
    jlong result = 0;
    return result;
}

JNIEXPORT void JNICALL JNI_PREFIX(nativeDestroyObject)(JNIEnv *, jclass, jlong){
    DBG_BEGIN();
}

JNIEXPORT void JNICALL JNI_PREFIX(nativeStart)(JNIEnv *, jclass, jlong){
    DBG_BEGIN();
}

JNIEXPORT void JNICALL JNI_PREFIX(nativeStop)(JNIEnv *, jclass, jlong){
    DBG_BEGIN();
}

JNIEXPORT void JNICALL JNI_PREFIX(nativeSetFaceSize)(JNIEnv *, jclass, jlong, jint){
    DBG_BEGIN();
}

JNIEXPORT jintArray JNICALL JNI_PREFIX(nativeDetect)(JNIEnv *env, jclass, jlong, jobject input_img) {
    DBG_BEGIN();

    int size = 10;
    jintArray face_points = env->NewIntArray(size*2);
    jint *pos = env->GetIntArrayElements(face_points, NULL);
    for (int i = 0; i < size; i++)
    {
        pos[2*i] = 100 + i;
        pos[2*i+1] = 200 + i;
    }

    env->ReleaseIntArrayElements(face_points, pos, NULL);

    return face_points;
}