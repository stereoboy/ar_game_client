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

#if 0 // not needed now with NDK r21
#include <cmath>
// reference: https://stackoverflow.com/questions/22922961/c11-cmath-functions-not-in-std-namespace-for-android-ndk-w-gcc-4-8-or-clang-3
namespace std {

    double round(double in){
        return ceil(in + 0.5);
    }
}
#endif

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
#define DBG_BEGIN() LOGD(">>%s:%d\n", __FUNCTION__, __LINE__)
#define DBG_END() LOGD("<<%s:%d\n", __FUNCTION__, __LINE__)
#define DBG(fmt, ...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "[%s:%d]" fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__))
#else
#define DBG_BEGIN()
#define DBG_END()
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
    /*
     * references
     * - https://developer.android.com/ndk/reference/group___asset.html
     * - http://stackoverflow.com/questions/13317387/how-to-get-file-in-assets-from-android-ndk
     */
    detector_wrapper(JNIEnv *env, jobject assetManager) {

        DBG("load detectors");
        AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
        if (mgr == NULL) {
            ERROR("failed\n");
            abort();
        }

        m_face_detector = dlib::get_frontal_face_detector();

        DBG("load face detector\n");
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

        int remain = size;
        while ((nb_read = AAsset_read(asset, bytes + offset, size)) > 0)
        {
            offset += nb_read;
            remain -= nb_read;
            DBG("read from Asset nb_read=%d, offset=%d, remain=%d\n", nb_read, offset, remain);
        }

        membuf buffer(bytes, bytes + size);
        //buffer.pubsetbuf(bytes, size);
        std::istream is(&buffer);

        dlib::deserialize(m_shape_predictor, is);
//        m_shape_predictor = sp;
        DBG("load shape detector\n");

        free(bytes);
        AAsset_close(asset);
    }

    ~detector_wrapper(){

    }

    std::vector<int> face_detect(char* byte, int w, int h, int bpp){

#if 1
        std::vector<int> face_points;

#define INPUT_SIZE 256.0f

        // Comvert raw data into dlib-specific data
        cv::Mat matInput_= cv::Mat(h, w, CV_8UC4, byte);

        cv::Mat matInput(h, w, CV_8UC3);
        cv::cvtColor(matInput_, matInput, COLOR_RGBA2BGR);
        dlib::cv_image<dlib::bgr_pixel>  inputImage(matInput);
        dlib::array2d<dlib::bgr_pixel> input;
        dlib::assign_image(input, inputImage);


        float scaledown = INPUT_SIZE/matInput.rows;
        float scaleup = (float)matInput.rows/INPUT_SIZE;

        int r = scaledown * matInput.rows;
        int c = scaledown * matInput.cols;
        cv::Mat matScaleDown;
        cv::resize(matInput, matScaleDown, Size(c, r));

        dlib::cv_image<dlib::bgr_pixel>  imageScaleDown(matScaleDown);
        dlib::array2d<dlib::bgr_pixel> scaleDown;
        dlib::assign_image(scaleDown, imageScaleDown);

        // detect faces and use only first face;
        //dlib::pyramid_up(input);
        DBG("A\n");
        std::vector<dlib::rectangle> faces = m_face_detector(scaleDown);

        DBG("B\n");
        if (faces.size() > 0)
        {
            dlib::rectangle rect = faces[0];

            int l = rect.left()*scaleup;
            int t = rect.top()*scaleup;
            int w = rect.width()*scaleup;
            int h = rect.height()*scaleup;

            dlib::rectangle rect2(l, t, l + w, t + h);
            cv::rectangle(matInput_, cv::Rect(l, t, w, h), cv::Scalar(255, 0, 255, 255), 2);

            DBG("C\n");
            dlib::full_object_detection face_info = m_shape_predictor(input, rect2);

            for (int i = 0; i < face_info.num_parts(); i++)
            {
                dlib::point p = face_info.part(i);
                int x = (int)p.x();
                int y = (int)p.y();
                face_points.push_back(x);
                face_points.push_back(y);
                cv::circle(matInput_, Point(x, y), 3, cv::Scalar(0, 255, 0, 255));
                DBG("(%d, %d)", x, y);
            }
            DBG("D\n");
        }
        else{
            ERROR("There is no Face");

        }

        cv::rectangle(matInput_, cv::Point(0, 0), cv::Point(20, 20), cv::Scalar(255, 0, 255, 255), -1);

        return face_points;
#else
        std::vector<int> face_points;

        // Comvert raw data into dlib-specific data
        cv::Mat matInput_= cv::Mat(h, w, CV_8UC4, byte);

        cv::Mat matInput(h, w, CV_8UC3);
        cv::cvtColor(matInput_, matInput, COLOR_RGBA2BGR);
        dlib::cv_image<dlib::bgr_pixel>  inputImage(matInput);

        cv::rectangle(matInput_, cv::Point(0, 0), cv::Point(20, 20), cv::Scalar(255, 0, 255, 255), -1);
        // detect faces and use only first face;
        dlib::array2d<dlib::bgr_pixel> input;
        dlib::assign_image(input, inputImage);
        //dlib::pyramid_up(input);
        DBG("A\n");
        //std::vector<dlib::rectangle> faces = m_face_detector(input);
        dlib::rectangle face(0, 0, w, h);
        DBG("B\n");


        DBG("C\n");
        dlib::full_object_detection face_info = m_shape_predictor(input, face);

        for (int i = 0; i < face_info.num_parts(); i++)
        {
            dlib::point p = face_info.part(i);
            int x = (int)p.x();
            int y = (int)p.y();
            face_points.push_back(x);
            face_points.push_back(y);
            cv::circle(matInput_, Point(x, y), 3, cv::Scalar(0, 255, 0, 255));
            DBG("(%d, %d)", x, y);
        }
        DBG("D\n");


        return face_points;
#endif
    }
};

JNIEXPORT jlong JNICALL JNI_PREFIX(nativeCreateObject)(JNIEnv *env, jclass, jobject assetManager, jint){
    DBG_BEGIN();

    jlong result = (jlong)new detector_wrapper(env, assetManager);

    DBG_END();
    return result;
}

JNIEXPORT void JNICALL JNI_PREFIX(nativeDestroyObject)(JNIEnv *, jclass, jlong thiz){
    DBG_BEGIN();
    //detector_wrapper *detector = (detector_wrapper*) thiz;
    delete (detector_wrapper*) thiz;

    DBG_END();
}

JNIEXPORT void JNICALL JNI_PREFIX(nativeStart)(JNIEnv *, jclass, jlong){
    DBG_BEGIN();
    DBG_END();
}

JNIEXPORT void JNICALL JNI_PREFIX(nativeStop)(JNIEnv *, jclass, jlong){
    DBG_BEGIN();
    DBG_END();
}

JNIEXPORT void JNICALL JNI_PREFIX(nativeSetFaceSize)(JNIEnv *, jclass, jlong, jint){
    DBG_BEGIN();
    DBG_END();
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
        //DBG("[%d] (%d, %d)\n", i, _face_points[2*i], _face_points[2*i+1]);
        pos[2*i] = _face_points[2*i];
        pos[2*i+1] = _face_points[2*i+1];

    }

    env->ReleaseIntArrayElements(face_points, pos, NULL);

    DBG_END();
    return face_points;
}