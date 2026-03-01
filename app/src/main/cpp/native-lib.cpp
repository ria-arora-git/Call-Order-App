#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <chrono>
#include "whisper.h"

#define LOG_TAG "WHISPER"
#define LOGI(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static whisper_context *g_ctx = nullptr;

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_callorderapp_NativeBridge_transcribe(
        JNIEnv *env,
        jobject,
        jstring modelPath_,
        jstring audioPath_) {

    auto totalStart = std::chrono::high_resolution_clock::now();

    const char *modelPath = env->GetStringUTFChars(modelPath_, 0);
    const char *audioPath = env->GetStringUTFChars(audioPath_, 0);

    LOGI("Transcription requested");

    if (g_ctx == nullptr) {
        LOGI("Loading model...");
        auto modelStart = std::chrono::high_resolution_clock::now();

        g_ctx = whisper_init_from_file(modelPath);

        if (!g_ctx) {
            return env->NewStringUTF("Failed to load model");
        }

        auto modelEnd = std::chrono::high_resolution_clock::now();
        auto modelMs = std::chrono::duration_cast<std::chrono::milliseconds>(modelEnd - modelStart).count();
        LOGI("Model loaded in %lld ms", modelMs);
    } else {
        LOGI("Model already loaded, reusing context");
    }

    LOGI("Opening audio file...");
    FILE *file = fopen(audioPath, "rb");
    if (!file) {
        return env->NewStringUTF("Failed to open audio file");
    }

    fseek(file, 0, SEEK_END);
    long fileSize = ftell(file);
    fseek(file, 44, SEEK_SET); // skip WAV header

    long dataSize = fileSize - 44;
    int sampleCount = dataSize / sizeof(int16_t);

    LOGI("Audio file size: %ld bytes", fileSize);
    LOGI("PCM samples: %d", sampleCount);

    std::vector<float> pcmf32;
    pcmf32.reserve(sampleCount);

    int16_t sample;
    while (fread(&sample, sizeof(int16_t), 1, file)) {
        pcmf32.push_back(sample / 32768.0f);
    }

    fclose(file);

    LOGI("PCM loaded into float buffer");

    whisper_full_params params =
            whisper_full_default_params(WHISPER_SAMPLING_GREEDY);

    params.language = "hi";
    params.translate = true;
    params.detect_language = false;
    params.n_threads = 6;

    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;


    LOGI("Starting inference...");
    auto inferStart = std::chrono::high_resolution_clock::now();

    if (whisper_full(g_ctx, params, pcmf32.data(), pcmf32.size()) != 0) {
        return env->NewStringUTF("Inference failed");
    }

    auto inferEnd = std::chrono::high_resolution_clock::now();
    auto inferMs = std::chrono::duration_cast<std::chrono::milliseconds>(inferEnd - inferStart).count();
    LOGI("Inference finished in %lld ms", inferMs);


    std::string result;

    int n_segments = whisper_full_n_segments(g_ctx);
    LOGI("Segments detected: %d", n_segments);

    for (int i = 0; i < n_segments; ++i) {
        result += whisper_full_get_segment_text(g_ctx, i);
    }

    auto totalEnd = std::chrono::high_resolution_clock::now();
    auto totalMs = std::chrono::duration_cast<std::chrono::milliseconds>(totalEnd - totalStart).count();
    LOGI("Total transcription time: %lld ms", totalMs);

    env->ReleaseStringUTFChars(modelPath_, modelPath);
    env->ReleaseStringUTFChars(audioPath_, audioPath);

    return env->NewStringUTF(result.c_str());
}