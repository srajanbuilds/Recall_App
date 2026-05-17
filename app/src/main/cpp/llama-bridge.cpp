#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_recall_app_core_ai_LlamaEngine_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "llama.cpp bridge initialized (stub)";
    return env->NewStringUTF(hello.c_str());
}
