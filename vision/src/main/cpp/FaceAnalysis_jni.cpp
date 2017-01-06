//
// Created by rofox on 1/3/17.
//

#include <FaceAnalysis_jni.h>


#include <android/bitmap.h>
#include <android/log.h>
#include <cmath>

#include <dlib/image_loader/load_image.h>
#include <dlib/image_processing.h>
#include <dlib/image_processing/frontal_face_detector.h>
#include <dlib/image_processing/render_face_detections.h>

#include <opencv2/core.hpp>
#include <opencv2/objdetect.hpp>

using namespace dlib;
using namespace std;


#define LOG_TAG "FaceAnalysis"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))
#define ERROR(fmt, ...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "[%s:%d]" fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__))
#define DBG_PRINT
#ifdef DBG_PRINT
#define DBG_BEGIN() LOGD("[%s:%d]\n", __FUNCTION__, __LINE__)
#define DBG(fmt, ...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "[%s:%d]" fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__))
#else
#define DBG_BEGIN()
#define DBG
#endif


class detector_wrapper {

private:
    dlib::frontal_face_detector detector;

public:
    detector_wrapper() {
        detector = dlib::get_frontal_face_detector();
        //std::round(1);
    }

    ~detector_wrapper(){

    }

    std::vector<dlib::rectangle> detect(dlib::array2d<dlib::rgb_pixel> input){
        std::vector<dlib::rectangle> ret = detector(input);
        return ret;
    }

};
detector_wrapper *g_detector = NULL;

JNIEXPORT jlong JNICALL JNI_PREFIX(nativeCreateObject)(JNIEnv *, jclass, jstring, jint){
    DBG_BEGIN();
    jlong result = (jlong)new detector_wrapper();
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

    int ret;
    AndroidBitmapInfo  infocolor;
    void* pixelscolor;

    if ((ret = AndroidBitmap_getInfo(env, input_img, &infocolor)) < 0) {
        ERROR("AndroidBitmap_getInfo() failed.");
        return NULL;
    }

    DBG("AndroidBitmapInfo: %d x %d", infocolor.width, infocolor.height);

    if ((ret = AndroidBitmap_lockPixels(env, input_img, &pixelscolor)) < 0) {
        ERROR("AndroidBitmap_lockPixels() failed.");
        return NULL;
    }

    AndroidBitmap_unlockPixels(env, input_img);

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