#include <jni.h>
#include <string>

#define FUNCTION(name) Java_com_secure_keychain_RNSecureKeychainModule_ ## name

std::string jstring2string(JNIEnv *env, jstring jStr) {
    if (!jStr)
        return "";

    const jclass stringClass = env->GetObjectClass(jStr);
    const jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(jStr, getBytes, env->NewStringUTF("UTF-8"));

    size_t length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte* pBytes = env->GetByteArrayElements(stringJbytes, NULL);

    std::string ret = std::string((char *)pBytes, length);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

    env->DeleteLocalRef(stringJbytes);
    env->DeleteLocalRef(stringClass);
    return ret;
}

extern "C" {
    jstring FUNCTION(fmd5)(JNIEnv *env, jobject obj, jstring j_data) {
        std::string data = jstring2string(env, j_data);
        std::string data2( data.size(), 'a');
        std::string data3( data.size(), 'a');

        for(int i=0; i<data.length(); i++)
        {
            char ch = data[i];
            data2[i] = static_cast<char>( ch == 'z' || ch == 'Z' ? 'a' : ch + 1  );
        }

        for(int i=0,ii=data.length()-1; i<data.length(); i++,ii--)
        {
            data3[i] = data2[ii];
        }

        return env->NewStringUTF(data3.c_str());
    }
}