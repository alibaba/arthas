#ifndef ARTHAS_VMTOOL_HEAP_ANALYZER_H
#define ARTHAS_VMTOOL_HEAP_ANALYZER_H

#include <jni.h>
#include <jvmti.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * 使用 JVMTI FollowReferences 从 GC Root 遍历可达对象，统计各类实例占用，并输出占用最大的对象/类。
 *
 * 返回值为 malloc 分配的 C 字符串，调用方负责 free。
 */
char *arthas_vmtool_heap_analyze(jvmtiEnv *jvmti, jint class_num,
                                 jint object_num);

/**
 * 使用 JVMTI FollowReferences 建立“从对象回溯到 root 的一条路径”，并输出指定类中占用最大的若干对象及回溯链。
 *
 * 返回值为 malloc 分配的 C 字符串，调用方负责 free。
 */
char *arthas_vmtool_reference_analyze(jvmtiEnv *jvmti, jclass klass,
                                      jint object_num, jint backtrace_num);

#ifdef __cplusplus
}
#endif

#endif
