#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "whisper.h"

#define LOG_TAG "WHISPER"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_callorderapp_NativeBridge_transcribe(
        JNIEnv *env,
        jobject /* this */,
        jstring modelPath_,
        jstring audioPath_) {

    const char *modelPath = env->GetStringUTFChars(modelPath_, 0);
    const char *audioPath = env->GetStringUTFChars(audioPath_, 0);

    whisper_context *ctx = whisper_init_from_file(modelPath);
    if (!ctx) {
        return env->NewStringUTF("Failed to load model");
    }

    std::vector<float> pcmf32;
    {
        FILE *file = fopen(audioPath, "rb");
        if (!file) {
            whisper_free(ctx);
            return env->NewStringUTF("Failed to open audio file");
        }

        fseek(file, 44, SEEK_SET); // skip WAV header

        int16_t sample;
        while (fread(&sample, sizeof(int16_t), 1, file)) {
            pcmf32.push_back(sample / 32768.0f);
        }

        fclose(file);
    }

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;

    if (whisper_full(ctx, params, pcmf32.data(), pcmf32.size()) != 0) {
        whisper_free(ctx);
        return env->NewStringUTF("Failed to run inference");
    }

    std::string result;

    const int n_segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < n_segments; ++i) {
        result += whisper_full_get_segment_text(ctx, i);
    }

    whisper_free(ctx);

    env->ReleaseStringUTFChars(modelPath_, modelPath);
    env->ReleaseStringUTFChars(audioPath_, audioPath);

    return env->NewStringUTF(result.c_str());
}