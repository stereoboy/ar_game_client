NDK_TOOLCHAIN_VERSION := clang
APP_PLATFORM := android-8
#APP_STL := c++_static  # This is for DLIB
APP_STL := gnustl_static # This is for OPENCV
# References
#  - https://developer.android.com/ndk/guides/abis.html
#  - https://developer.qualcomm.com/blog/android-3d-use-opengl-es-20-and-neon-compiler-options
APP_CPPFLAGS := -std=c++11 -frtti -fexceptions -mfpu=neon