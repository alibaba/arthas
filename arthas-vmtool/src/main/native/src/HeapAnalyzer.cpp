#include <algorithm>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#include <jni_md.h>
#include <jvmti.h>

#include "HeapAnalyzer.h"

static void check_error(jvmtiError err, char const *name) {
  if (err) {
    fprintf(stderr, "ERROR: JVMTI %s failed!\n", name);
  }
}

template <typename... Args>
static void append_format(std::string &output, const char *data, Args... args) {
  size_t size = 1 + snprintf(0, 0, data, args...);
  char bytes[size];
  snprintf(bytes, size, data, args...);
  output.append(bytes);
}

static char *getClassName(jvmtiEnv *jvmti, jclass cls) {
  char *sig, *name;
  check_error(jvmti->GetClassSignature(cls, &sig, 0), "GetClassSignature");
  if (sig) {
    int index = 0;
    int l = strlen(sig);
    if (l > 1 && sig[l - 1] == ';') {
      l--;
    }
    while (sig[index] == '[') {
      index++;
    }
    int type = 0;
    char buf[10];
    switch (sig[index]) {
    case 'Z':
      strcpy(buf, "boolean");
      l = 7;
      break;
    case 'B':
      strcpy(buf, "byte");
      l = 4;
      break;
    case 'C':
      strcpy(buf, "char");
      l = 4;
      break;
    case 'S':
      strcpy(buf, "short");
      l = 5;
      break;
    case 'I':
      strcpy(buf, "int");
      l = 3;
      break;
    case 'J':
      strcpy(buf, "long");
      l = 4;
      break;
    case 'F':
      strcpy(buf, "float");
      l = 5;
      break;
    case 'D':
      strcpy(buf, "double");
      l = 6;
      break;
    case 'L': {
      type = 1;
      l = l - index - 1;
      break;
    }
    default: {
      type = 2;
      l = l - index;
      break;
    }
    }
    name = (char *)malloc(l + index * 2 + 1);
    if (type == 0) {
      strncpy(name, buf, l);
    } else if (type == 1) {
      strncpy(name, sig + index + 1, l);
    } else if (type == 2) {
      strncpy(name, sig + index, l);
    }
    while (index--) {
      name[l++] = '[';
      name[l++] = ']';
    }
    name[l] = 0;
    check_error(jvmti->Deallocate((unsigned char *)sig), "Deallocate");
    char *t = name;
    while (*t) {
      if (*t == '/') {
        *t = '.';
      }
      t++;
    }
  }
  return name;
}

void ObjectInfoHeap::swap(int i, int j) {
  ObjectInfo *t = array[i];
  array[i] = array[j];
  array[j] = t;
}

void ObjectInfoHeap::adjust(int cur, int limit) {
  int l = 2 * cur + 1, r = 2 * cur + 2, min;
  if (l < limit && array[l]->less(array[cur])) {
    min = l;
  } else {
    min = cur;
  }
  if (r < limit && array[r]->less(array[min])) {
    min = r;
  }
  if (min != cur) {
    swap(min, cur);
    adjust(min, limit);
  }
}

void ObjectInfoHeap::sort() {
  for (int i = record_number - 1; i >= 0; i--) {
    swap(0, i);
    adjust(0, i);
  }
}

ObjectInfoHeap::ObjectInfoHeap(int record_number)
    : record_number(record_number) {
  array = (ObjectInfo **)malloc(sizeof(ObjectInfo *) * record_number);
  for (int i = 0; i < record_number; i++) {
    array[i] = new ObjectInfo();
  }
}

ObjectInfoHeap::~ObjectInfoHeap() {
  for (int i = 0; i < record_number; i++) {
    delete array[i];
  }
  free(array);
}

void ObjectInfoHeap::add(int size, TagInfo *tag) {
  if (array[0]->size < size) {
    array[0]->size = size;
    array[0]->object_tag = tag;
    adjust(0, record_number);
  }
}

void ObjectInfoHeap::print(ClassInfo **class_info_array, jvmtiEnv *jvmti,
                           std::string &output, int backtrace_number) {
  sort();
  append_format(output, "\n%-4s\t%-10s\t%s\n", "id", "#bytes",
                backtrace_number ? "class_name & references" : "class_name");
  append_format(output,
                "----------------------------------------------------\n");
  for (int i = 0; i < record_number && array[i]->object_tag != 0; i++) {
    ObjectInfo *oi = array[i];
    ClassInfo *ci = class_info_array[oi->object_tag->class_tag];
    append_format(output, "%-4d\t%-10d\t%s", i + 1, oi->size, ci->name);
    if (backtrace_number != 0) {
      TagInfo *ref = oi->object_tag->referrer;
      TagInfo *ref_pre = oi->object_tag;
      for (int j = 0;
           (backtrace_number == -1 || j < backtrace_number - 1) && ref != 0;
           j++) {
        ci = class_info_array[ref->class_tag];
        append_format(output, " <-- %s", ci->name);
        ref_pre = ref;
        ref = ref->referrer;
      }
      if (ref == 0) {
        append_format(output, " <-- root");
        if (ref_pre->stack_info != 0) {
          char *name;
          check_error(
              jvmti->GetMethodName(ref_pre->stack_info->method, &name, 0, 0),
              "GetMethodName");
          append_format(output, "(local variable in method: %s)\n", name);
          check_error(jvmti->Deallocate((unsigned char *)name), "Deallocate");
        } else {
          append_format(output, "\n");
        }
      } else {
        append_format(output, " <-- ...\n");
      }
    } else {
      append_format(output, "\n");
    }
  }
  append_format(output, "\n");
}

jint JNICALL HeapAnalyzer::tag(jvmtiHeapReferenceKind reference_kind,
                               const jvmtiHeapReferenceInfo *reference_info,
                               jlong class_tag, jlong referrer_class_tag,
                               jlong size, jlong *tag_ptr,
                               jlong *referrer_tag_ptr, jint length,
                               void *user_data) {
  // java.lang.Class对象的tag已经在get_classes时设置
  // 此处的class_tag与java.lang.Class对象的tag相同
  // 因此该对象TagInfo中的class_tag来源于该对象class_tag所指向的TagInfo中的class_object_tag
  TagInfo *ti = 0;
  if (*tag_ptr == 0) {
    ti = new TagInfo();
    *tag_ptr = (jlong)ti;
  } else {
    ti = (TagInfo *)*tag_ptr;
  }
  if (ti->class_tag == 0) {
    TagInfo *ctti = (TagInfo *)class_tag;
    ti->class_tag = ctti->class_object_tag;
    if (ti->referrer == 0) {
      // 当引用者是root时，referrer_tag_ptr为0
      if (referrer_tag_ptr != 0) {
        ti->referrer = (TagInfo *)*referrer_tag_ptr;
      } else {
        if (reference_kind == JVMTI_HEAP_REFERENCE_STACK_LOCAL) {
          ti->stack_info = (jvmtiHeapReferenceInfoStackLocal *)malloc(
              sizeof(jvmtiHeapReferenceInfoStackLocal));
          memcpy(ti->stack_info, reference_info,
                 sizeof(jvmtiHeapReferenceInfoStackLocal));
        }
      }
    }
    ClassInfo **class_info_array = (ClassInfo **)((void **)user_data)[0];
    int *object_number_pointer = (int *)((void **)user_data)[1];
    ObjectInfoHeap *object_info_heap =
        (ObjectInfoHeap *)((void **)user_data)[2];
    ClassInfo *ci = class_info_array[ti->class_tag];
    ci->instance_count++;
    ci->total_size += size;
    (*object_number_pointer)++;
    if (object_info_heap != 0) {
      object_info_heap->add(size, ti);
    }
  }
  return JVMTI_VISIT_OBJECTS;
}

jint JNICALL HeapAnalyzer::untag(jlong class_tag, jlong size, jlong *tag_ptr,
                                 jint length, void *user_data) {
  bool do_delete = (bool)user_data;
  if (*tag_ptr != 0) {
    if (do_delete) {
      delete (TagInfo *)*tag_ptr;
    }
    *tag_ptr = 0;
  }
  return JVMTI_VISIT_OBJECTS;
}

jint JNICALL HeapAnalyzer::reference(jlong class_tag, jlong size,
                                     jlong *tag_ptr, jint length,
                                     void *user_data) {
  TagInfo *ti = (TagInfo *)*tag_ptr;
  ObjectInfoHeap *object_info_heap = (ObjectInfoHeap *)user_data;
  if (object_info_heap != 0) {
    object_info_heap->add(size, ti);
  }
  return JVMTI_ITERATION_CONTINUE;
}

HeapAnalyzer::HeapAnalyzer(jvmtiEnv *jvmti) : jvmti(jvmti) {
  jvmtiCapabilities capa;
  memset(&capa, 0, sizeof(capa));
  capa.can_tag_objects = 1;
  check_error(jvmti->AddCapabilities(&capa), "AddCapabilities");
  untag_objects(false);
  get_classes();
}

HeapAnalyzer::~HeapAnalyzer() {
  untag_objects(true);
  jvmti = 0;
  for (int i = 1; i < class_number + 1; i++) {
    delete class_info_array[i];
  }
  free(class_info_array);
  class_info_array = 0;
}

void HeapAnalyzer::tag_objects(ObjectInfoHeap *object_info_heap) {
  jvmtiHeapCallbacks heap_callbacks;
  memset(&heap_callbacks, 0, sizeof(heap_callbacks));
  heap_callbacks.heap_reference_callback = HeapAnalyzer::tag;
  void *user_data[3] = {(void *)class_info_array, (void *)&object_number,
                        (void *)object_info_heap};
  check_error(jvmti->FollowReferences(0, 0, 0, &heap_callbacks, user_data),
              "FollowReferences");
}

void HeapAnalyzer::untag_objects(bool do_delete) {
  jvmtiHeapCallbacks heap_callbacks;
  memset(&heap_callbacks, 0, sizeof(heap_callbacks));
  heap_callbacks.heap_iteration_callback = HeapAnalyzer::untag;
  check_error(jvmti->IterateThroughHeap(JVMTI_HEAP_FILTER_UNTAGGED, 0,
                                        &heap_callbacks, (void *)do_delete),
              "IterateThroughHeap");
}

void HeapAnalyzer::get_classes() {
  jclass *classes;
  check_error(jvmti->GetLoadedClasses(&class_number, &classes),
              "GetLoadedClasses");
  class_info_array =
      (ClassInfo **)malloc(sizeof(ClassInfo *) * (class_number + 1));
  for (int i = 1; i < class_number + 1; i++) {
    ClassInfo *ci = new ClassInfo(i, getClassName(jvmti, classes[i - 1]));
    class_info_array[i] = ci;
    TagInfo *ti = new TagInfo(i);
    check_error(jvmti->SetTag(classes[i - 1], (jlong)ti), "SetTag");
  }
  check_error(jvmti->Deallocate((unsigned char *)classes), "Deallocate");
}

char *HeapAnalyzer::heap_analyze(int class_show_number,
                                 int object_show_number) {
  std::string output = "";
  append_format(output, "class_number: %d\n", class_number);
  ObjectInfoHeap *object_info_heap = new ObjectInfoHeap(object_show_number);
  tag_objects(object_info_heap);
  append_format(output, "object_number: %d\n", object_number);
  object_info_heap->print(class_info_array, jvmti, output, 0);

  std::sort(class_info_array + 1, class_info_array + class_number,
            ClassInfo::compare);
  append_format(output, "\n%-4s\t%-12s\t%-15s\t%s\n", "id", "#instances",
                "#bytes", "class_name");
  append_format(output,
                "----------------------------------------------------\n");
  for (int i = 1; i - 1 < class_show_number && i < class_number; i++) {
    ClassInfo *ci = class_info_array[i];
    append_format(output, "%-4d\t%-12d\t%-15ld\t%s\n", i, ci->instance_count,
                  ci->total_size, ci->name);
  }
  append_format(output, "\n");

  delete object_info_heap;
  char *result = (char *)malloc(output.size() + 1);
  strcpy(result, output.c_str());
  return result;
}

char *HeapAnalyzer::reference_analyze(jclass klass, int object_show_number,
                                      int backtrace_number) {
  std::string output = "";
  tag_objects(0);
  ObjectInfoHeap *object_info_heap = new ObjectInfoHeap(object_show_number);
  jvmtiHeapCallbacks heap_callbacks;
  memset(&heap_callbacks, 0, sizeof(heap_callbacks));
  heap_callbacks.heap_iteration_callback = HeapAnalyzer::reference;
  check_error(jvmti->IterateThroughHeap(JVMTI_HEAP_FILTER_UNTAGGED, klass,
                                        &heap_callbacks,
                                        (void *)object_info_heap),
              "IterateThroughHeap");
  object_info_heap->print(class_info_array, jvmti, output, backtrace_number);

  delete object_info_heap;
  char *result = (char *)malloc(output.size() + 1);
  strcpy(result, output.c_str());
  return result;
}
