LOCAL_PATH := $(call my-dir)

# static library info
LOCAL_MODULE := libopensmile
LOCAL_MODULE_FILENAME := libopensmile
LOCAL_SRC_FILES := ../prebuild/libopensmile.a
LOCAL_EXPORT_C_INCLUDES := ../prebuild/include
include $(PREBUILT_STATIC_LIBRARY)

# wrapper info
include $(CLEAR_VARS)
LOCAL_C_INCLUDES += ../prebuild/include
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
LOCAL_CPPFLAGS += -fexceptions
LOCAL_MODULE    := opensmiletest
LOCAL_SRC_FILES := opensmiletest.cpp
LOCAL_STATIC_LIBRARIES := libopensmile
include $(BUILD_SHARED_LIBRARY)
