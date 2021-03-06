#include <jni.h>
#include <string>

// Mes emplacements de fonctions (package name) contiennent des "1" car en réalité,
// la lettre suivant un "1" est une majuscule

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_td3_1source_1code_1with_1c_MainActivity_getAPIconfig(JNIEnv* env,jobject /* this */)
{
    std::string url = "_neRg1)\u007Fw$(*#H%-0Ts%\\+!q%.*&q%)2~OcZeRR]%c`p`X\\SDUee\u007FEce`ZI#";
    return env->NewStringUTF(url.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_td3_1source_1code_1with_1c_Accounts_getAPIaccounts(JNIEnv* env,jobject /* this */)
{
    std::string url = "_neRg1)\u007Fw$(*#H%-0Ts%\\+!q%.*&q%)2~OcZeRR]%c`p`X\\SDUee\u007FCWZifPhj";
    return env->NewStringUTF(url.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_td3_1source_1code_1with_1c_MainActivity_getKeyEncryption(JNIEnv* env,jobject /* this */)
{
    std::string keyForEncryption = "Key";
    return env->NewStringUTF(keyForEncryption.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_td3_1source_1code_1with_1c_Accounts_getKeyEncryption(JNIEnv* env,jobject /* this */)
{
    std::string keyForEncryption = "Key";
    return env->NewStringUTF(keyForEncryption.c_str());
}



