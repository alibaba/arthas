#include <iostream>
#include <jni.h>
#include <jni_md.h>
#include <jvmti.h>
#include "arthas_VmTool.h" // under target/native/javah/


static jvmtiEnv *jvmti;
static jlong tagCounter = 0;

struct LimitCounter {
    jint currentCounter;
    jint limitValue;

    void init(jint limit) {
        currentCounter = 0;
        limitValue = limit;
    }

    void countDown() {
        currentCounter++;
    }

    bool allow() {
        if (limitValue < 0) {
            return true;
        }
        return limitValue > currentCounter;
    }
};

// 每次 IterateOverInstancesOfClass 调用前需要先 init
static LimitCounter limitCounter = {0, 0};

extern "C"
int init_agent(JavaVM *vm, void *reserved) {
    jint rc;
    /* Get JVMTI environment */
    rc = vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_2);
    if (rc != JNI_OK) {
        fprintf(stderr, "ERROR: arthas vmtool Unable to create jvmtiEnv, GetEnv failed, error=%d\n", rc);
        return -1;
    }

    jvmtiCapabilities capabilities = {0};
    capabilities.can_tag_objects = 1;
    capabilities.can_signal_thread = 1;
    jvmtiError error = jvmti->AddCapabilities(&capabilities);
    if (error) {
        fprintf(stderr, "ERROR: arthas vmtool JVMTI AddCapabilities failed!%u\n", error);
        return JNI_FALSE;
    }

    return JNI_OK;
}

extern "C" JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
    return init_agent(vm, reserved);
}

extern "C" JNIEXPORT jint JNICALL
Agent_OnAttach(JavaVM* vm, char* options, void* reserved) {
    return init_agent(vm, reserved);
}

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    init_agent(vm, reserved);
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL
Java_arthas_VmTool_forceGc0(JNIEnv *env, jclass thisClass) {
    jvmti->ForceGarbageCollection();
}

extern "C"
JNIEXPORT void JNICALL
Java_arthas_VmTool_interruptSpecialThread0(JNIEnv *env, jclass thisClass,  jint threadId) {
    jint threads_count_ptr = 0;
    jthread* threads_ptr;
    jvmtiError error = jvmti->GetAllThreads(&threads_count_ptr,&threads_ptr);
    if (error) {
        printf("ERROR: JVMTI get all thread failed!%u\n", error);
        return;
    }

    jclass thread_class = env->FindClass("java/lang/Thread");
    if (thread_class == NULL) {
        printf("Error: Failed to find class java/lang/Thread.\n");
        return;
   }

    jfieldID tid_field = env->GetFieldID(thread_class, "tid", "J");
    if (tid_field == NULL) {
        printf("Error: Failed to find field tid.\n");
        return;
    }

    for (int i = 0; i < threads_count_ptr; ++i) {
        jvmtiThreadInfo threadInfo;
        error = jvmti->GetThreadInfo(threads_ptr[i], &threadInfo);
        jlong tid = env->GetLongField(threads_ptr[i], tid_field);
        // release name filed
        // https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#jvmtiThreadInfo:~:text=The%20pointer%20returned%20in%20the%20field%20name%20of%20jvmtiThreadInfo%20is%20a%20newly%20allocated%20array.%20The%20array%20should%20be%20freed%20with%20Deallocate
        jvmti->Deallocate(reinterpret_cast<unsigned char *>(threadInfo.name));
        if (error != JVMTI_ERROR_NONE || tid != threadId) {
            continue;
        }

        error = jvmti->InterruptThread(threads_ptr[i]);
        if (error != JVMTI_ERROR_NONE) {
            printf("ERROR: JVMTI interrupt thread failed!%u\n", error);
        }

        break;
    }
    jvmti->Deallocate(reinterpret_cast<unsigned char *>(threads_ptr));
}

extern "C"
jlong getTag() {
    return ++tagCounter;
}

extern "C"
jvmtiIterationControl JNICALL
HeapObjectCallback(jlong class_tag, jlong size, jlong *tag_ptr, void *user_data) {
    jlong *data = static_cast<jlong *>(user_data);
    *tag_ptr = *data;

    limitCounter.countDown();
    if (limitCounter.allow()) {
        return JVMTI_ITERATION_CONTINUE;
    }else {
        return JVMTI_ITERATION_ABORT;
    }
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_arthas_VmTool_getInstances0(JNIEnv *env, jclass thisClass, jclass klass, jint limit) {
    jlong tag = getTag();
    limitCounter.init(limit);
    jvmtiError error = jvmti->IterateOverInstancesOfClass(klass, JVMTI_HEAP_OBJECT_EITHER,
                                               HeapObjectCallback, &tag);
    if (error) {
        printf("ERROR: JVMTI IterateOverInstancesOfClass failed!%u\n", error);
        return NULL;
    }

    jint count = 0;
    jobject *instances;
    error = jvmti->GetObjectsWithTags(1, &tag, &count, &instances, NULL);
    if (error) {
        printf("ERROR: JVMTI GetObjectsWithTags failed!%u\n", error);
        return NULL;
    }

    jobjectArray array = env->NewObjectArray(count, klass, NULL);
    //添加元素到数组
    for (int i = 0; i < count; i++) {
        env->SetObjectArrayElement(array, i, instances[i]);
    }
    jvmti->Deallocate(reinterpret_cast<unsigned char *>(instances));
    return array;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_arthas_VmTool_sumInstanceSize0(JNIEnv *env, jclass thisClass, jclass klass) {
    jlong tag = getTag();
    limitCounter.init(-1);
    jvmtiError error = jvmti->IterateOverInstancesOfClass(klass, JVMTI_HEAP_OBJECT_EITHER,
                                               HeapObjectCallback, &tag);
    if (error) {
        printf("ERROR: JVMTI IterateOverInstancesOfClass failed!%u\n", error);
        return -1;
    }

    jint count = 0;
    jobject *instances;
    error = jvmti->GetObjectsWithTags(1, &tag, &count, &instances, NULL);
    if (error) {
        printf("ERROR: JVMTI GetObjectsWithTags failed!%u\n", error);
        return -1;
    }

    jlong sum = 0;
    for (int i = 0; i < count; i++) {
        jlong size = 0;
        jvmti->GetObjectSize(instances[i], &size);
        sum = sum + size;
    }
    jvmti->Deallocate(reinterpret_cast<unsigned char *>(instances));
    return sum;
}

extern "C"
JNIEXPORT jlong JNICALL Java_arthas_VmTool_getInstanceSize0
        (JNIEnv *env, jclass thisClass, jobject instance) {
    jlong size = -1;
    jvmtiError error = jvmti->GetObjectSize(instance, &size);
    if (error) {
        printf("ERROR: JVMTI GetObjectSize failed!%u\n", error);
    }
    return size;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_arthas_VmTool_countInstances0(JNIEnv *env, jclass thisClass, jclass klass) {
    jlong tag = getTag();
    limitCounter.init(-1);
    jvmtiError error = jvmti->IterateOverInstancesOfClass(klass, JVMTI_HEAP_OBJECT_EITHER,
                                               HeapObjectCallback, &tag);
    if (error) {
        printf("ERROR: JVMTI IterateOverInstancesOfClass failed!%u\n", error);
        return -1;
    }

    jint count = 0;
    error = jvmti->GetObjectsWithTags(1, &tag, &count, NULL, NULL);
    if (error) {
        printf("ERROR: JVMTI GetObjectsWithTags failed!%u\n", error);
        return -1;
    }
    return count;
}

extern "C"
JNIEXPORT jobjectArray JNICALL Java_arthas_VmTool_getAllLoadedClasses0
        (JNIEnv *env, jclass thisClass, jclass kclass) {
    jclass *classes;
    jint count = 0;

    jvmtiError error = jvmti->GetLoadedClasses(&count, &classes);
    if (error) {
        printf("ERROR: JVMTI GetLoadedClasses failed!\n");
        return NULL;
    }

    jobjectArray array = env->NewObjectArray(count, kclass, NULL);
    //添加元素到数组
    for (int i = 0; i < count; i++) {
        env->SetObjectArrayElement(array, i, classes[i]);
    }
    jvmti->Deallocate(reinterpret_cast<unsigned char *>(classes));
    return array;
}