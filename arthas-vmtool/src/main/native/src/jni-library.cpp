#include <string.h>
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

    jvmtiCapabilities caps_potential;
    jvmtiError error = jvmti->GetPotentialCapabilities(&caps_potential);

    jvmtiCapabilities capabilities;
    (void)memset(&capabilities,0,sizeof(capabilities));
    capabilities.can_tag_objects = 1;
    capabilities.can_access_local_variables = caps_potential.can_access_local_variables;

    error = jvmti->AddCapabilities(&capabilities);
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

extern "C"
JNIEXPORT jobjectArray JNICALL Java_arthas_VmTool_getLocalVariableTable0
  (JNIEnv *env, jclass thisClass, jobject jthread, jint depth){
	jmethodID method_ptr;
	jlocation location_ptr;
	jvmtiError error = jvmti->GetFrameLocation(jthread, depth, &method_ptr, &location_ptr);
	if (error) {
		printf("ERROR: JVMTI GetFrameLocation failed!%u\n", error);
		return NULL;
	}

	jint entry_count_ptr;
	jvmtiLocalVariableEntry *table_ptr;
	error = jvmti->GetLocalVariableTable(method_ptr, &entry_count_ptr, &table_ptr);
	if(error){
		printf("ERROR: JVMTI GetLocalVariableTable entry_count_ptr : %u,error : %u\n", entry_count_ptr,error);
		return NULL;
	}

	jclass object_klass = env->FindClass("java/lang/Object");
	jobjectArray ret_array = env->NewObjectArray(entry_count_ptr*2, object_klass, NULL);

	jclass integer_kclass = env->FindClass("java/lang/Integer");
	jmethodID integer_jmethod_valueOf = env->GetStaticMethodID(integer_kclass, "valueOf", "(I)Ljava/lang/Integer;");

	int index = 0;
	for(int i = 0;i < entry_count_ptr;i++){
		jvmtiLocalVariableEntry entry = table_ptr[i];

		jobject slot_val = env->CallStaticObjectMethod(integer_kclass, integer_jmethod_valueOf,entry.slot);
		env->SetObjectArrayElement(ret_array, index, slot_val);

		jstring signature_val = env->NewStringUTF(entry.signature);
		env->SetObjectArrayElement(ret_array, ++index, signature_val);
		env->ReleaseStringChars(signature_val, NULL);

		index++;
	}
	jvmti->Deallocate(reinterpret_cast<unsigned char *>(table_ptr));
	return ret_array;
}

extern "C"
JNIEXPORT jint JNICALL Java_arthas_VmTool_getLocalInt0
  (JNIEnv *env, jclass thisClass, jobject jthread, jint depth, jint slot){
	jint value_ptr;
	jvmtiError error = jvmti->GetLocalInt(jthread, depth, slot, &value_ptr);
	if (error) {
		printf("ERROR: JVMTI GetLocalInt failed!%u\n", error);
	}
	return value_ptr;
}

extern "C"
JNIEXPORT jlong JNICALL Java_arthas_VmTool_getLocalLong0
  (JNIEnv *env, jclass thisClass, jobject jthread, jint depth, jint slot){
	jlong value_ptr;
	jvmtiError error = jvmti->GetLocalLong(jthread, depth, slot, &value_ptr);
	if (error) {
		printf("ERROR: JVMTI GetLocalLong failed!%u\n", error);
	}
	return value_ptr;
}

extern "C"
JNIEXPORT jfloat JNICALL Java_arthas_VmTool_getLocalFloat0
  (JNIEnv *env, jclass thisClass, jobject jthread, jint depth, jint slot){
	jfloat value_ptr;
	jvmtiError error = jvmti->GetLocalFloat(jthread, depth, slot, &value_ptr);
	if (error) {
		printf("ERROR: JVMTI GetLocalFloat failed!%u\n", error);
	}
	return value_ptr;
}

extern "C"
JNIEXPORT jdouble JNICALL Java_arthas_VmTool_getLocalDouble0
  (JNIEnv *env, jclass thisClass, jobject jthread, jint depth, jint slot){
	jdouble value_ptr;
	jvmtiError error = jvmti->GetLocalDouble(jthread, depth, slot, &value_ptr);
	if (error) {
		printf("ERROR: JVMTI GetLocalDouble failed!%u\n", error);
	}
	return value_ptr;
}

extern "C"
JNIEXPORT jobject JNICALL Java_arthas_VmTool_getLocalObject0
  (JNIEnv *env, jclass thisClass, jobject jthread, jint depth, jint slot){
	jobject value_ptr;
	jvmtiError error = jvmti->GetLocalObject(jthread, depth, slot, &value_ptr);

	if (error) {
		printf("ERROR: JVMTI GetLocalObject failed!%u\n", error);
		return NULL;
	}
	return value_ptr;
}