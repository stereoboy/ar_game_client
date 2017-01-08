//
// Created by rofox on 1/3/17.
//
#include <jni.h>

#ifndef AR_GAME_CLIENT_FACEANALYSIS_JNI_H
#define AR_GAME_CLIENT_FACEANALYSIS_JNI_H
#ifdef __cplusplus
extern "C" {
#endif

#undef JNI_PREFIX
#define JNI_PREFIX(name) Java_com_viptech_game_vision_FaceAnalysis_##name
JNIEXPORT jlong JNICALL JNI_PREFIX(nativeCreateObject)(JNIEnv *env, jclass, jobject assetManager, jint);

JNIEXPORT void JNICALL JNI_PREFIX(nativeDestroyObject)(JNIEnv *, jclass, jlong);

JNIEXPORT void JNICALL JNI_PREFIX(nativeStart)(JNIEnv *, jclass, jlong);

JNIEXPORT void JNICALL JNI_PREFIX(nativeStop)(JNIEnv *, jclass, jlong);

JNIEXPORT void JNICALL JNI_PREFIX(nativeSetFaceSize)(JNIEnv *, jclass, jlong, jint);

JNIEXPORT jintArray JNICALL JNI_PREFIX(nativeDetect)(JNIEnv *, jclass, jlong, jobject);

#ifdef __cplusplus
}
#endif
#endif //AR_GAME_CLIENT_FACEANALYSIS_JNI_H_H
