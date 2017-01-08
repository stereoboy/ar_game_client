NDK_TOOLCHAIN_VERSION := clang
APP_PLATFORM := android-8
#APP_STL := c++_static  # This is for DLIB
APP_STL := gnustl_static # This is for OPENCV
APP_CPPFLAGS := -std=c++11 -frtti -fexceptions