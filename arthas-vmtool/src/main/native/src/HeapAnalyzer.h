#ifndef HEAP_ANALYZER_H
#define HEAP_ANALYZER_H

#include <jni_md.h>
#include <jvmti.h>

#include <cstdlib>
#include <memory>
#include <string>

using std::shared_ptr;
using std::string;

class ClassInfo {
public:
  int id = 0;
  shared_ptr<const char[]> name;
  int instance_count = 0;
  long total_size = 0;

  ClassInfo(int id, shared_ptr<const char[]> name) : id(id) {
    this->name = name;
  }
  static bool compare(shared_ptr<ClassInfo> ci1, shared_ptr<ClassInfo> ci2) {
    return ci1->total_size > ci2->total_size;
  }
};

class TagInfo {
public:
  int class_tag = 0;        // 该对象所属类的tag
  int class_object_tag = 0; // 用于java.lang.Class对象标注其表示的类的tag
  // 如果A是一个java.lang.Class的对象，其表示类A_class，则其class_tag指向java.lang.Class，而class_object_tag指向A_class
  TagInfo *referrer = 0; // 引用者，为0时表示由root(JNI、stack等)引用
  shared_ptr<jvmtiHeapReferenceInfoStackLocal>
      stack_info; // 当引用来自JVMTI_HEAP_REFERENCE_STACK_LOCAL时，记录其reference_info

  TagInfo(int class_object_tag = 0) : class_object_tag(class_object_tag) {}
};

class ObjectInfo {
public:
  int size = 0;
  TagInfo *object_tag = 0;

  ObjectInfo(int size, TagInfo *object_tag)
      : size(size), object_tag(object_tag) {}
  ObjectInfo() {}
  bool less(shared_ptr<ObjectInfo> oi) { return this->size < oi->size; }
};

class ObjectInfoHeap {
private:
  int record_number = 0;
  shared_ptr<shared_ptr<ObjectInfo>[]> array;

  void swap(int i, int j);
  void adjust(int cur, int limit);
  void sort();

public:
  ObjectInfoHeap(int record_number);
  void add(int size, TagInfo *tag);
  void print(shared_ptr<shared_ptr<ClassInfo>[]> class_info_array,
             jvmtiEnv *jvmti, string &output, int backtrace_number);
};

class HeapAnalyzer {
private:
  int object_number = 0;
  jint class_number = 0;
  shared_ptr<shared_ptr<ClassInfo>[]> class_info_array;
  jvmtiEnv *jvmti = 0;

  static jint JNICALL tag(jvmtiHeapReferenceKind reference_kind,
                          const jvmtiHeapReferenceInfo *reference_info,
                          jlong class_tag, jlong referrer_class_tag, jlong size,
                          jlong *tag_ptr, jlong *referrer_tag_ptr, jint length,
                          void *user_data);
  static jint JNICALL untag(jlong class_tag, jlong size, jlong *tag_ptr,
                            jint length, void *user_data);
  static jint JNICALL reference(jlong class_tag, jlong size, jlong *tag_ptr,
                                jint length, void *user_data);
  void get_classes();
  void tag_objects(ObjectInfoHeap *object_info_heap);
  void untag_objects(bool do_delete);

public:
  HeapAnalyzer(jvmtiEnv *jvmti);
  ~HeapAnalyzer();
  char *heap_analyze(int class_show_number = 20, int object_show_number = 20);
  char *reference_analyze(jclass klass, int object_show_number = 20,
                          int backtrace_number = 2);
};

#endif
