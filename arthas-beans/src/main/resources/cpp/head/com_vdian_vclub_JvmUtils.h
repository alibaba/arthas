/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_vdian_vclub_JvmUtils */

#ifndef _Included_com_vdian_vclub_JvmUtils
#define _Included_com_vdian_vclub_JvmUtils
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_vdian_vclub_JvmUtils
 * Method:    check
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_vdian_vclub_JvmUtils_check
  (JNIEnv *, jclass);

/*
 * Class:     com_vdian_vclub_JvmUtils
 * Method:    getInstances
 * Signature: (Ljava/lang/Class;)Ljava/util/LinkedList;
 */
JNIEXPORT jobject JNICALL Java_com_vdian_vclub_JvmUtils_getInstances
  (JNIEnv *, jclass, jclass);

/*
 * Class:     com_vdian_vclub_JvmUtils
 * Method:    sumInstanceSize
 * Signature: (Ljava/lang/Class;)J
 */
JNIEXPORT jlong JNICALL Java_com_vdian_vclub_JvmUtils_sumInstanceSize
  (JNIEnv *, jclass, jclass);

/*
 * Class:     com_vdian_vclub_JvmUtils
 * Method:    getInstanceSize
 * Signature: (Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL Java_com_vdian_vclub_JvmUtils_getInstanceSize
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_vdian_vclub_JvmUtils
 * Method:    countInstances
 * Signature: (Ljava/lang/Class;)J
 */
JNIEXPORT jlong JNICALL Java_com_vdian_vclub_JvmUtils_countInstances
  (JNIEnv *, jclass, jclass);

/*
 * Class:     com_vdian_vclub_JvmUtils
 * Method:    getAllLoadedClasses
 * Signature: ()Ljava/util/LinkedList;
 */
JNIEXPORT jobject JNICALL Java_com_vdian_vclub_JvmUtils_getAllLoadedClasses
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
