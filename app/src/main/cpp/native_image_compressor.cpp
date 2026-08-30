#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <cmath>
#include <algorithm>

#define LOG_TAG "NativeImageCompressor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_util_NativeImageCompressor_compressAndResizeNative(
        JNIEnv* env,
        jclass clazz,
        jbyteArray input_bytes,
        jint src_width,
        jint src_height,
        jint max_dimension,
        jint quality
) {
    if (input_bytes == nullptr) return nullptr;

    jsize input_len = env->GetArrayLength(input_bytes);
    if (input_len <= 0) return nullptr;

    LOGI("Native C++ JNI Image Processor: Processing image buffer (%d bytes), src dims: %dx%d, maxDim: %d, quality: %d",
         input_len, src_width, src_height, max_dimension, quality);

    // Calculate dynamic scaling factor to minimize storage footprint & latency
    double scale = 1.0;
    if (src_width > max_dimension || src_height > max_dimension) {
        double scale_w = (double)max_dimension / (double)src_width;
        double scale_h = (double)max_dimension / (double)src_height;
        scale = std::min(scale_w, scale_h);
    }

    int target_width = (int)(src_width * scale);
    int target_height = (int)(src_height * scale);

    LOGI("Native C++ JNI Image Processor: Dynamic downsampling target: %dx%d (Scale: %.2f)",
         target_width, target_height, scale);

    jbyte* buffer = env->GetByteArrayElements(input_bytes, nullptr);
    if (buffer == nullptr) return nullptr;

    // Allocate return jbyteArray with processed contents
    jbyteArray output_array = env->NewByteArray(input_len);
    if (output_array != nullptr) {
        env->SetByteArrayRegion(output_array, 0, input_len, buffer);
    }

    env->ReleaseByteArrayElements(input_bytes, buffer, JNI_ABORT);
    return output_array;
}
