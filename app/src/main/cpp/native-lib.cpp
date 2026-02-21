#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_callorderapp_NativeBridge_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {

    std::string hello = "Whisper Native Ready";
    return env->NewStringUTF(hello.c_str());
}