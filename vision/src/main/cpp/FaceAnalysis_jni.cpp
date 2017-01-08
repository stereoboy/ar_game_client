//
// Created by rofox on 1/3/17.
//
#include <FaceAnalysis_jni.h>

#include <istream>
#include <streambuf>

#include <android/bitmap.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>

#include <cmath>
namespace std {

    double round(double in){
        return ceil(in + 0.5);
    }
}

#include <dlib/opencv/cv_image.h>
#include <dlib/image_loader/load_image.h>
#include <dlib/image_processing.h>
#include <dlib/image_processing/frontal_face_detector.h>
#include <dlib/image_processing/render_face_detections.h>

#include "opencv2/imgproc/imgproc.hpp"
#include <opencv2/objdetect.hpp>

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


    using namespace cv;

struct membuf: std::streambuf {
    membuf(char* begin, char* end) {
        this->setg(begin, begin, end);
    }
};

class detector_wrapper {

private:
    dlib::frontal_face_detector m_face_detector;
    dlib::shape_predictor m_shape_predictor;
public:
    detector_wrapper(JNIEnv *env, jobject assetManager) {
        AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
        if (mgr == NULL) {
            ERROR("failed\n");
            abort();
        }

        m_face_detector = dlib::get_frontal_face_detector();


        AAsset* asset = AAssetManager_open(mgr, "shape_predictor_68_face_landmarks.dat", AASSET_MODE_UNKNOWN);
        if (asset == NULL) {
            ERROR("failed\n");
            abort();
        }

        int offset = 0;
        int nb_read = 0;
        int size = AAsset_getLength(asset);
        char *bytes = (char*)malloc(size);

        DBG("size = %d\n", size);

        while ((nb_read = AAsset_read(asset, bytes + offset, BUFSIZ)) > 0)
        {
            offset += nb_read;
        }

        membuf buffer(bytes, bytes + size);
        //buffer.pubsetbuf(bytes, size);
        std::istream is(&buffer);

        dlib::deserialize(m_shape_predictor, is);;
//        m_shape_predictor = sp;


        free(bytes);
        AAsset_close(asset);
    }

    ~detector_wrapper(){

    }

    std::vector<int> face_detect(char* byte, int w, int h, int bpp){


        std::vector<int> face_points;

        // Comvert raw data into dlib-specific data
        cv::Mat matInput_(h, w, CV_8UC4, byte);
        cv::Mat matInput(h, w, CV_8UC3);
        cv::cvtColor(matInput, matInput_, COLOR_RGBA2RGB);
        dlib::cv_image<dlib::rgb_pixel>  inputImage(matInput);


        // detect faces and use only first face;
        dlib::array2d<dlib::rgb_pixel> input;
        dlib::assign_image(input, inputImage);
        dlib::pyramid_up(input);

        std::vector<dlib::rectangle> faces = m_face_detector(input);

        if (faces.size() > 0)
        {
            dlib::full_object_detection face_info = m_shape_predictor(input, faces[0]);

            for (int i = 0; i < face_info.num_parts(); i++)
            {
                dlib::point p = face_info.part(i);
                face_points.push_back((int)p.x());
                face_points.push_back((int)p.y());
            }
        }

        return face_points;
    }



};

JNIEXPORT jlong JNICALL JNI_PREFIX(nativeCreateObject)(JNIEnv *env, jclass, jobject assetManager, jint){
    DBG_BEGIN();

    jlong result = (jlong)new detector_wrapper(env, assetManager);
    return result;
}

JNIEXPORT void JNICALL JNI_PREFIX(nativeDestroyObject)(JNIEnv *, jclass, jlong thiz){
    DBG_BEGIN();
    //detector_wrapper *detector = (detector_wrapper*) thiz;
    delete (detector_wrapper*) thiz;
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

JNIEXPORT jintArray JNICALL JNI_PREFIX(nativeDetect)(JNIEnv *env, jclass, jlong thiz, jobject input_img) {
    DBG_BEGIN();
    int ret;
    detector_wrapper *detector = (detector_wrapper*) thiz;
    AndroidBitmapInfo  infocolor;
    void* pixels;

    if ((ret = AndroidBitmap_getInfo(env, input_img, &infocolor)) < 0) {
        ERROR("AndroidBitmap_getInfo() failed.");
        return NULL;
    }

    DBG("AndroidBitmapInfo: %d x %d", infocolor.width, infocolor.height);


    if ((ret = AndroidBitmap_lockPixels(env, input_img, &pixels)) < 0) {
        ERROR("AndroidBitmap_lockPixels() failed.");
        return NULL;
    }


    std::vector<int> _face_points = detector->face_detect((char*)pixels, infocolor.width, infocolor.height, 3);


    AndroidBitmap_unlockPixels(env, input_img);

    // convert native info into java info
    int size = _face_points.size()/2;
    jintArray face_points = env->NewIntArray(size*2);
    jint *pos = env->GetIntArrayElements(face_points, NULL);

    for (int i = 0; i < size; i++)
    {
        DBG("[%d] (%d, %d)\n", i, _face_points[2*i], _face_points[2*i+1]);
        pos[2*i] = _face_points[2*i];
        pos[2*i+1] = _face_points[2*i+1];

    }

    env->ReleaseIntArrayElements(face_points, pos, NULL);

    return face_points;
}