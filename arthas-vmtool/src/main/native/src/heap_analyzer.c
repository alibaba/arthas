#include "heap_analyzer.h"

#include <limits.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/**
 * 设计说明（纯 C 实现）：
 *
 * - FollowReferences 的 heap_reference_callback 会被多次调用（同一对象可能被不同引用边命中）
 * - 为了“每个对象只统计一次”，需要对对象打 tag 做去重
 * - Arthas 的 vmtool 其它功能也会使用 JVMTI tag；因此这里采用“带 magic + run_id 的数值 tag”，并在结束后恢复原 tag，避免冲突
 * - class_tag 参数来自“对象所属的 java.lang.Class 对象的 tag”，所以遍历前需要先给所有已加载类的 Class 对象打上 class tag（同样会恢复）
 * - 为了不破坏 class_tag 映射，遍历过程中不会覆盖 Class 对象的 tag；对 Class 对象去重使用额外数组标记
 */

#define HEAP_TAG_MAGIC 0xA7A5
#define HEAP_TAG_MAGIC_SHIFT 48
#define HEAP_TAG_RUN_SHIFT 32
#define HEAP_TAG_TYPE_SHIFT 30

#define HEAP_TAG_MAGIC_MASK ((jlong)0xFFFF000000000000LL)
#define HEAP_TAG_RUN_MASK ((jlong)0x0000FFFF00000000LL)
#define HEAP_TAG_TYPE_MASK ((jlong)0x00000000C0000000LL)
#define HEAP_TAG_ID_MASK ((jlong)0x000000003FFFFFFFLL)

#define HEAP_TAG_TYPE_CLASS 0
#define HEAP_TAG_TYPE_OBJECT 1

static jlong g_heap_analyzer_run_counter = 0;

#ifdef __cplusplus
#define JVMTI_CALL(jvmti, func, ...)                                           \
  ((jvmti)->functions->func((jvmti), __VA_ARGS__))
#else
#define JVMTI_CALL(jvmti, func, ...)                                           \
  ((*(jvmti))->func((jvmti), __VA_ARGS__))
#endif

typedef struct {
  char *name;
  jlong instance_count;
  jlong total_size;
} class_info_t;

typedef struct {
  jlong original_tag;
  jlong referrer_tag;
  jlong size;
  jint class_id;
  jmethodID root_method;
  unsigned char has_root_method;
} object_entry_t;

typedef struct {
  jlong size;
  jlong tag;
  jint class_id;
} top_object_t;

typedef struct {
  char *buf;
  size_t len;
  size_t cap;
  int oom;
} sb_t;

typedef struct {
  jvmtiEnv *jvmti;
  jlong tag_magic_bits;
  jlong tag_run_bits;

  jint class_count;
  class_info_t *class_info;
  jlong *class_original_tags;

  unsigned char *class_object_seen;
  unsigned char *class_object_traversed;
  unsigned char *class_object_referrer_set;
  jlong *class_object_referrer_tag;
  jint *class_object_class_id;
  unsigned char *class_object_has_root_method;
  jmethodID *class_object_root_method;

  object_entry_t *objects;
  jint object_capacity;
  jint object_tag_count;
  jlong object_number;

  top_object_t *top_objects;
  jint top_object_max;
  jint top_object_count;

  int callback_error;
} heap_ctx_t;

static void sb_init(sb_t *sb) {
  sb->buf = NULL;
  sb->len = 0;
  sb->cap = 0;
  sb->oom = 0;
}

static void sb_free(sb_t *sb) {
  if (sb->buf) {
    free(sb->buf);
  }
  sb_init(sb);
}

static int sb_reserve(sb_t *sb, size_t need) {
  size_t required;
  size_t new_cap;
  char *new_buf;

  if (sb->oom) {
    return 0;
  }

  required = sb->len + need + 1;
  if (required <= sb->cap) {
    return 1;
  }

  new_cap = sb->cap ? sb->cap : 1024;
  while (new_cap < required) {
    new_cap *= 2;
  }

  new_buf = (char *)realloc(sb->buf, new_cap);
  if (!new_buf) {
    sb->oom = 1;
    return 0;
  }
  sb->buf = new_buf;
  sb->cap = new_cap;
  return 1;
}

static int sb_append_bytes(sb_t *sb, const char *data, size_t n) {
  if (!sb_reserve(sb, n)) {
    return 0;
  }
  memcpy(sb->buf + sb->len, data, n);
  sb->len += n;
  sb->buf[sb->len] = '\0';
  return 1;
}

static int sb_append_cstr(sb_t *sb, const char *s) {
  if (!s) {
    s = "<null>";
  }
  return sb_append_bytes(sb, s, strlen(s));
}

static int sb_vprintf(sb_t *sb, const char *fmt, va_list ap) {
  va_list ap_copy;
  int required;
  size_t need;

  if (sb->oom) {
    return 0;
  }

  va_copy(ap_copy, ap);
  required = vsnprintf(NULL, 0, fmt, ap_copy);
  va_end(ap_copy);
  if (required < 0) {
    sb->oom = 1;
    return 0;
  }
  need = (size_t)required;
  if (!sb_reserve(sb, need)) {
    return 0;
  }
  vsnprintf(sb->buf + sb->len, sb->cap - sb->len, fmt, ap);
  sb->len += need;
  sb->buf[sb->len] = '\0';
  return 1;
}

static int sb_printf(sb_t *sb, const char *fmt, ...) {
  va_list ap;
  int ok;
  va_start(ap, fmt);
  ok = sb_vprintf(sb, fmt, ap);
  va_end(ap);
  return ok;
}

static jlong make_tag(heap_ctx_t *ctx, jint type, jint id) {
  return ctx->tag_magic_bits | ctx->tag_run_bits |
         (((jlong)type) << HEAP_TAG_TYPE_SHIFT) | ((jlong)id & HEAP_TAG_ID_MASK);
}

static int is_our_tag(heap_ctx_t *ctx, jlong tag) {
  return ((tag & HEAP_TAG_MAGIC_MASK) == ctx->tag_magic_bits) &&
         ((tag & HEAP_TAG_RUN_MASK) == ctx->tag_run_bits);
}

static int tag_type(jlong tag) {
  return (int)((tag & HEAP_TAG_TYPE_MASK) >> HEAP_TAG_TYPE_SHIFT);
}

static jint tag_id(jlong tag) { return (jint)(tag & HEAP_TAG_ID_MASK); }

static jint decode_class_id(heap_ctx_t *ctx, jlong class_tag) {
  if (!is_our_tag(ctx, class_tag)) {
    return 0;
  }
  if (tag_type(class_tag) != HEAP_TAG_TYPE_CLASS) {
    return 0;
  }
  jint id = tag_id(class_tag);
  if (id <= 0 || id > ctx->class_count) {
    return 0;
  }
  return id;
}

static const char *class_name_by_id(heap_ctx_t *ctx, jint class_id) {
  if (!ctx || !ctx->class_info) {
    return "<unknown>";
  }
  if (class_id < 0 || class_id > ctx->class_count) {
    return "<unknown>";
  }
  if (!ctx->class_info[class_id].name) {
    return "<unknown>";
  }
  return ctx->class_info[class_id].name;
}

static void top_objects_swap(top_object_t *a, top_object_t *b) {
  top_object_t t = *a;
  *a = *b;
  *b = t;
}

// top_objects 数组按 size 升序存放（arr[0] 最小）
static void top_objects_add(heap_ctx_t *ctx, jlong size, jlong tag,
                            jint class_id) {
  jint i;
  top_object_t obj;
  if (!ctx || ctx->top_object_max <= 0 || !ctx->top_objects) {
    return;
  }

  obj.size = size;
  obj.tag = tag;
  obj.class_id = class_id;

  if (ctx->top_object_count < ctx->top_object_max) {
    ctx->top_objects[ctx->top_object_count] = obj;
    ctx->top_object_count++;
    i = ctx->top_object_count - 1;
    while (i > 0 && ctx->top_objects[i].size < ctx->top_objects[i - 1].size) {
      top_objects_swap(&ctx->top_objects[i], &ctx->top_objects[i - 1]);
      i--;
    }
    return;
  }

  if (ctx->top_object_count <= 0) {
    return;
  }
  if (size <= ctx->top_objects[0].size) {
    return;
  }

  ctx->top_objects[0] = obj;
  i = 0;
  while (i + 1 < ctx->top_object_count &&
         ctx->top_objects[i].size > ctx->top_objects[i + 1].size) {
    top_objects_swap(&ctx->top_objects[i], &ctx->top_objects[i + 1]);
    i++;
  }
}

static void record_object(heap_ctx_t *ctx, jint object_class_id, jlong size,
                          jlong object_tag) {
  ctx->object_number++;
  if (object_class_id > 0 && object_class_id <= ctx->class_count) {
    ctx->class_info[object_class_id].instance_count++;
    ctx->class_info[object_class_id].total_size += size;
  }
  top_objects_add(ctx, size, object_tag, object_class_id);
}

static int ensure_object_capacity(heap_ctx_t *ctx, jint object_id) {
  jint new_cap;
  object_entry_t *new_arr;

  if (object_id <= ctx->object_capacity) {
    return 1;
  }
  new_cap = ctx->object_capacity ? ctx->object_capacity : 1024;
  while (new_cap < object_id) {
    new_cap *= 2;
  }
  new_arr =
      (object_entry_t *)realloc(ctx->objects, (size_t)(new_cap + 1) *
                                                   sizeof(object_entry_t));
  if (!new_arr) {
    return 0;
  }
  // 新增区域清零（避免读取未初始化数据）
  memset(new_arr + ctx->object_capacity + 1, 0,
         (size_t)(new_cap - ctx->object_capacity) * sizeof(object_entry_t));
  ctx->objects = new_arr;
  ctx->object_capacity = new_cap;
  return 1;
}

static const char *primitive_name(char c) {
  switch (c) {
  case 'Z':
    return "boolean";
  case 'B':
    return "byte";
  case 'C':
    return "char";
  case 'S':
    return "short";
  case 'I':
    return "int";
  case 'J':
    return "long";
  case 'F':
    return "float";
  case 'D':
    return "double";
  default:
    return NULL;
  }
}

static char *class_name_from_signature(const char *sig) {
  int dims = 0;
  const char *p;
  const char *base;
  size_t base_len;
  const char *prim;
  char *name;
  size_t total_len;
  size_t i;

  if (!sig) {
    name = (char *)malloc(strlen("<unknown>") + 1);
    if (name) {
      strcpy(name, "<unknown>");
    }
    return name;
  }

  p = sig;
  while (*p == '[') {
    dims++;
    p++;
  }

  prim = primitive_name(*p);
  if (prim) {
    base = prim;
    base_len = strlen(prim);
  } else if (*p == 'L') {
    // Ljava/lang/String;
    base = p + 1;
    {
      const char *semi = strchr(base, ';');
      base_len = semi ? (size_t)(semi - base) : strlen(base);
    }
  } else {
    base = p;
    base_len = strlen(base);
  }

  total_len = base_len + (size_t)dims * 2;
  name = (char *)malloc(total_len + 1);
  if (!name) {
    return NULL;
  }

  memcpy(name, base, base_len);
  name[base_len] = '\0';

  // 替换 '/' 为 '.'
  for (i = 0; i < base_len; i++) {
    if (name[i] == '/') {
      name[i] = '.';
    }
  }

  for (i = 0; i < (size_t)dims; i++) {
    strcat(name, "[]");
  }

  return name;
}

static jint JNICALL heap_reference_callback(
    jvmtiHeapReferenceKind reference_kind,
    const jvmtiHeapReferenceInfo *reference_info, jlong class_tag,
    jlong referrer_class_tag, jlong size, jlong *tag_ptr,
    jlong *referrer_tag_ptr, jint length, void *user_data) {
  heap_ctx_t *ctx = (heap_ctx_t *)user_data;
  jlong obj_tag;
  jlong ref_tag = 0;
  jint obj_class_id;

  (void)referrer_class_tag;
  (void)length;

  if (!ctx || !tag_ptr) {
    return 0;
  }
  if (ctx->callback_error) {
    return JVMTI_VISIT_ABORT;
  }

  obj_tag = *tag_ptr;
  obj_class_id = decode_class_id(ctx, class_tag);

  if (referrer_tag_ptr != NULL) {
    ref_tag = *referrer_tag_ptr;
    if (!is_our_tag(ctx, ref_tag)) {
      // 避免把外部 tag 作为 referrer 链的一部分
      ref_tag = 0;
    }
  }

  // Class 对象：tag 为 class tag，不允许覆盖（否则 class_tag 映射会失效）
  if (is_our_tag(ctx, obj_tag) && tag_type(obj_tag) == HEAP_TAG_TYPE_CLASS) {
    jint represented_class_id = tag_id(obj_tag);
    if (represented_class_id > 0 && represented_class_id <= ctx->class_count) {
      if (!ctx->class_object_seen[represented_class_id]) {
        ctx->class_object_seen[represented_class_id] = 1;
        ctx->class_object_class_id[represented_class_id] = obj_class_id;
        record_object(ctx, obj_class_id, size, obj_tag);
      }

      if (!ctx->class_object_referrer_set[represented_class_id]) {
        ctx->class_object_referrer_set[represented_class_id] = 1;
        ctx->class_object_referrer_tag[represented_class_id] = ref_tag;
        if (ref_tag == 0 &&
            reference_kind == JVMTI_HEAP_REFERENCE_STACK_LOCAL &&
            reference_info != NULL) {
          const jvmtiHeapReferenceInfoStackLocal *info =
              (const jvmtiHeapReferenceInfoStackLocal *)reference_info;
          ctx->class_object_root_method[represented_class_id] = info->method;
          ctx->class_object_has_root_method[represented_class_id] = 1;
        }
      }

      if (!ctx->class_object_traversed[represented_class_id]) {
        ctx->class_object_traversed[represented_class_id] = 1;
        return JVMTI_VISIT_OBJECTS;
      }
      return 0;
    }
  }

  // 已经是本次运行打过的 object tag，直接跳过
  if (is_our_tag(ctx, obj_tag) && tag_type(obj_tag) == HEAP_TAG_TYPE_OBJECT) {
    return 0;
  }

  // 非 Class 对象：打上 object tag 并记录元信息
  {
    jint object_id = ctx->object_tag_count + 1;
    jlong new_tag;
    object_entry_t *e;

    if (object_id <= 0) {
      ctx->callback_error = 1;
      return JVMTI_VISIT_ABORT;
    }
    if (!ensure_object_capacity(ctx, object_id)) {
      ctx->callback_error = 1;
      return JVMTI_VISIT_ABORT;
    }
    new_tag = make_tag(ctx, HEAP_TAG_TYPE_OBJECT, object_id);

    e = &ctx->objects[object_id];
    memset(e, 0, sizeof(*e));
    e->original_tag = obj_tag;
    e->class_id = obj_class_id;
    e->size = size;
    e->referrer_tag = ref_tag;
    e->has_root_method = 0;
    if (ref_tag == 0 && reference_kind == JVMTI_HEAP_REFERENCE_STACK_LOCAL &&
        reference_info != NULL) {
      const jvmtiHeapReferenceInfoStackLocal *info =
          (const jvmtiHeapReferenceInfoStackLocal *)reference_info;
      e->root_method = info->method;
      e->has_root_method = 1;
    }

    *tag_ptr = new_tag;
    ctx->object_tag_count = object_id;
    record_object(ctx, obj_class_id, size, new_tag);
    return JVMTI_VISIT_OBJECTS;
  }
}

static jint JNICALL restore_tag_callback(jlong class_tag, jlong size,
                                        jlong *tag_ptr, jint length,
                                        void *user_data) {
  heap_ctx_t *ctx = (heap_ctx_t *)user_data;
  jlong tag;
  (void)class_tag;
  (void)size;
  (void)length;

  if (!ctx || !tag_ptr) {
    return JVMTI_VISIT_OBJECTS;
  }
  tag = *tag_ptr;
  if (!is_our_tag(ctx, tag)) {
    return JVMTI_VISIT_OBJECTS;
  }

  if (tag_type(tag) == HEAP_TAG_TYPE_CLASS) {
    jint cid = tag_id(tag);
    if (cid > 0 && cid <= ctx->class_count && ctx->class_original_tags) {
      *tag_ptr = ctx->class_original_tags[cid];
    } else {
      *tag_ptr = 0;
    }
    return JVMTI_VISIT_OBJECTS;
  }

  if (tag_type(tag) == HEAP_TAG_TYPE_OBJECT) {
    jint oid = tag_id(tag);
    if (oid > 0 && oid <= ctx->object_tag_count && ctx->objects) {
      *tag_ptr = ctx->objects[oid].original_tag;
    } else {
      *tag_ptr = 0;
    }
    return JVMTI_VISIT_OBJECTS;
  }

  *tag_ptr = 0;
  return JVMTI_VISIT_OBJECTS;
}

static void free_ctx(heap_ctx_t *ctx) {
  jint i;
  if (!ctx) {
    return;
  }

  if (ctx->class_info) {
    for (i = 0; i <= ctx->class_count; i++) {
      if (ctx->class_info[i].name) {
        free(ctx->class_info[i].name);
        ctx->class_info[i].name = NULL;
      }
    }
    free(ctx->class_info);
    ctx->class_info = NULL;
  }

  if (ctx->class_original_tags) {
    free(ctx->class_original_tags);
    ctx->class_original_tags = NULL;
  }
  if (ctx->class_object_seen) {
    free(ctx->class_object_seen);
    ctx->class_object_seen = NULL;
  }
  if (ctx->class_object_traversed) {
    free(ctx->class_object_traversed);
    ctx->class_object_traversed = NULL;
  }
  if (ctx->class_object_referrer_set) {
    free(ctx->class_object_referrer_set);
    ctx->class_object_referrer_set = NULL;
  }
  if (ctx->class_object_referrer_tag) {
    free(ctx->class_object_referrer_tag);
    ctx->class_object_referrer_tag = NULL;
  }
  if (ctx->class_object_class_id) {
    free(ctx->class_object_class_id);
    ctx->class_object_class_id = NULL;
  }
  if (ctx->class_object_has_root_method) {
    free(ctx->class_object_has_root_method);
    ctx->class_object_has_root_method = NULL;
  }
  if (ctx->class_object_root_method) {
    free(ctx->class_object_root_method);
    ctx->class_object_root_method = NULL;
  }

  if (ctx->objects) {
    free(ctx->objects);
    ctx->objects = NULL;
  }

  if (ctx->top_objects) {
    free(ctx->top_objects);
    ctx->top_objects = NULL;
  }
}

static int prepare_classes(heap_ctx_t *ctx) {
  jclass *classes = NULL;
  jint count = 0;
  jvmtiError err;
  jint i;

  err = JVMTI_CALL(ctx->jvmti, GetLoadedClasses, &count, &classes);
  if (err != JVMTI_ERROR_NONE || !classes || count <= 0) {
    return 0;
  }

  ctx->class_count = count;

  ctx->class_info =
      (class_info_t *)calloc((size_t)(count + 1), sizeof(class_info_t));
  ctx->class_original_tags =
      (jlong *)calloc((size_t)(count + 1), sizeof(jlong));

  ctx->class_object_seen =
      (unsigned char *)calloc((size_t)(count + 1), sizeof(unsigned char));
  ctx->class_object_traversed =
      (unsigned char *)calloc((size_t)(count + 1), sizeof(unsigned char));
  ctx->class_object_referrer_set =
      (unsigned char *)calloc((size_t)(count + 1), sizeof(unsigned char));
  ctx->class_object_referrer_tag =
      (jlong *)calloc((size_t)(count + 1), sizeof(jlong));
  ctx->class_object_class_id =
      (jint *)calloc((size_t)(count + 1), sizeof(jint));
  ctx->class_object_has_root_method =
      (unsigned char *)calloc((size_t)(count + 1), sizeof(unsigned char));
  ctx->class_object_root_method =
      (jmethodID *)calloc((size_t)(count + 1), sizeof(jmethodID));

  if (!ctx->class_info || !ctx->class_original_tags || !ctx->class_object_seen ||
      !ctx->class_object_traversed || !ctx->class_object_referrer_set ||
      !ctx->class_object_referrer_tag || !ctx->class_object_class_id ||
      !ctx->class_object_has_root_method || !ctx->class_object_root_method) {
    JVMTI_CALL(ctx->jvmti, Deallocate, (unsigned char *)classes);
    return 0;
  }

  // class_info[0] 预留为 unknown
  ctx->class_info[0].name = (char *)malloc(strlen("<unknown>") + 1);
  if (ctx->class_info[0].name) {
    strcpy(ctx->class_info[0].name, "<unknown>");
  }

  for (i = 1; i <= count; i++) {
    jclass cls = classes[i - 1];
    jlong old_tag = 0;
    char *sig = NULL;
    char *name = NULL;

    JVMTI_CALL(ctx->jvmti, GetTag, cls, &old_tag);
    ctx->class_original_tags[i] = old_tag;

    JVMTI_CALL(ctx->jvmti, SetTag, cls, make_tag(ctx, HEAP_TAG_TYPE_CLASS, i));

    err = JVMTI_CALL(ctx->jvmti, GetClassSignature, cls, &sig, NULL);
    if (err == JVMTI_ERROR_NONE) {
      name = class_name_from_signature(sig);
      if (sig) {
        JVMTI_CALL(ctx->jvmti, Deallocate, (unsigned char *)sig);
      }
    }
    if (!name) {
      name = (char *)malloc(strlen("<unknown>") + 1);
      if (name) {
        strcpy(name, "<unknown>");
      }
    }
    ctx->class_info[i].name = name;
  }

  JVMTI_CALL(ctx->jvmti, Deallocate, (unsigned char *)classes);
  return 1;
}

static void restore_tags(heap_ctx_t *ctx) {
  jvmtiHeapCallbacks callbacks;
  jvmtiError err;
  memset(&callbacks, 0, sizeof(callbacks));
  callbacks.heap_iteration_callback = restore_tag_callback;

  err = JVMTI_CALL(ctx->jvmti, IterateThroughHeap, JVMTI_HEAP_FILTER_TAGGED, NULL,
                   &callbacks, (void *)ctx);
  (void)err;
}

static char *build_heap_analyze_output(heap_ctx_t *ctx, jint class_num,
                                       jint object_num) {
  sb_t sb;
  jint i;

  sb_init(&sb);

  if (class_num < 0) {
    class_num = 0;
  }
  if (object_num < 0) {
    object_num = 0;
  }

  sb_printf(&sb, "class_number: %d\n", (int)ctx->class_count);
  sb_printf(&sb, "object_number: %lld\n", (long long)ctx->object_number);

  sb_printf(&sb, "\n%-4s\t%-10s\t%s\n", "id", "#bytes", "class_name");
  sb_printf(&sb, "----------------------------------------------------\n");
  for (i = 0; i < ctx->top_object_count; i++) {
    jint idx = ctx->top_object_count - 1 - i;
    top_object_t *o = &ctx->top_objects[idx];
    sb_printf(&sb, "%-4d\t%-10lld\t%s\n", (int)(i + 1), (long long)o->size,
              class_name_by_id(ctx, o->class_id));
  }
  sb_printf(&sb, "\n");

  // 选出占用最大的 class_num 个类
  if (class_num > 0) {
    top_object_t *top_classes;
    jint top_count = 0;
    jint max = class_num;
    if (max > ctx->class_count) {
      max = ctx->class_count;
    }
    top_classes =
        (top_object_t *)calloc((size_t)max, sizeof(top_object_t)); // 复用字段
    if (top_classes) {
      jint cid;
      for (cid = 1; cid <= ctx->class_count; cid++) {
        jlong total = ctx->class_info[cid].total_size;
        // top_classes 按 total_size 升序存放
        if (top_count < max) {
          top_classes[top_count].size = total;
          top_classes[top_count].class_id = cid;
          top_count++;
          i = top_count - 1;
          while (i > 0 && top_classes[i].size < top_classes[i - 1].size) {
            top_objects_swap(&top_classes[i], &top_classes[i - 1]);
            i--;
          }
        } else if (total > top_classes[0].size) {
          top_classes[0].size = total;
          top_classes[0].class_id = cid;
          i = 0;
          while (i + 1 < top_count &&
                 top_classes[i].size > top_classes[i + 1].size) {
            top_objects_swap(&top_classes[i], &top_classes[i + 1]);
            i++;
          }
        }
      }

      sb_printf(&sb, "\n%-4s\t%-12s\t%-15s\t%s\n", "id", "#instances", "#bytes",
                "class_name");
      sb_printf(&sb, "----------------------------------------------------\n");
      for (i = 0; i < top_count; i++) {
        jint idx = top_count - 1 - i;
        jint cid = top_classes[idx].class_id;
        sb_printf(&sb, "%-4d\t%-12lld\t%-15lld\t%s\n", (int)(i + 1),
                  (long long)ctx->class_info[cid].instance_count,
                  (long long)ctx->class_info[cid].total_size,
                  class_name_by_id(ctx, cid));
      }
      sb_printf(&sb, "\n");
      free(top_classes);
    }
  }

  if (sb.oom) {
    sb_free(&sb);
    return NULL;
  }

  return sb.buf;
}

static int get_class_id_for_tag(heap_ctx_t *ctx, jlong tag, jint *out_class_id) {
  if (!ctx || !out_class_id) {
    return 0;
  }
  if (!is_our_tag(ctx, tag)) {
    return 0;
  }
  if (tag_type(tag) == HEAP_TAG_TYPE_OBJECT) {
    jint oid = tag_id(tag);
    if (oid <= 0 || oid > ctx->object_tag_count || !ctx->objects) {
      return 0;
    }
    *out_class_id = ctx->objects[oid].class_id;
    return 1;
  }
  if (tag_type(tag) == HEAP_TAG_TYPE_CLASS) {
    jint cid = tag_id(tag);
    if (cid <= 0 || cid > ctx->class_count || !ctx->class_object_class_id) {
      return 0;
    }
    *out_class_id = ctx->class_object_class_id[cid];
    return 1;
  }
  return 0;
}

static int get_referrer_for_tag(heap_ctx_t *ctx, jlong tag, jlong *out_ref) {
  if (!ctx || !out_ref) {
    return 0;
  }
  if (!is_our_tag(ctx, tag)) {
    return 0;
  }
  if (tag_type(tag) == HEAP_TAG_TYPE_OBJECT) {
    jint oid = tag_id(tag);
    if (oid <= 0 || oid > ctx->object_tag_count || !ctx->objects) {
      return 0;
    }
    *out_ref = ctx->objects[oid].referrer_tag;
    return 1;
  }
  if (tag_type(tag) == HEAP_TAG_TYPE_CLASS) {
    jint cid = tag_id(tag);
    if (cid <= 0 || cid > ctx->class_count || !ctx->class_object_referrer_set) {
      return 0;
    }
    if (!ctx->class_object_referrer_set[cid]) {
      return 0;
    }
    *out_ref = ctx->class_object_referrer_tag[cid];
    return 1;
  }
  return 0;
}

static int get_root_method_for_tag(heap_ctx_t *ctx, jlong tag,
                                   jmethodID *out_method) {
  if (!ctx || !out_method) {
    return 0;
  }
  if (!is_our_tag(ctx, tag)) {
    return 0;
  }
  if (tag_type(tag) == HEAP_TAG_TYPE_OBJECT) {
    jint oid = tag_id(tag);
    if (oid <= 0 || oid > ctx->object_tag_count || !ctx->objects) {
      return 0;
    }
    if (!ctx->objects[oid].has_root_method) {
      return 0;
    }
    *out_method = ctx->objects[oid].root_method;
    return 1;
  }
  if (tag_type(tag) == HEAP_TAG_TYPE_CLASS) {
    jint cid = tag_id(tag);
    if (cid <= 0 || cid > ctx->class_count || !ctx->class_object_has_root_method) {
      return 0;
    }
    if (!ctx->class_object_has_root_method[cid]) {
      return 0;
    }
    *out_method = ctx->class_object_root_method[cid];
    return 1;
  }
  return 0;
}

static void top_target_add(top_object_t *arr, jint max, jint *count, jlong size,
                           jint object_id) {
  jint i;
  top_object_t obj;
  if (max <= 0 || !arr || !count) {
    return;
  }

  obj.size = size;
  obj.class_id = object_id; // 复用 class_id 字段存 object_id
  obj.tag = 0;

  if (*count < max) {
    arr[*count] = obj;
    (*count)++;
    i = *count - 1;
    while (i > 0 && arr[i].size < arr[i - 1].size) {
      top_objects_swap(&arr[i], &arr[i - 1]);
      i--;
    }
    return;
  }

  if (*count <= 0) {
    return;
  }
  if (size <= arr[0].size) {
    return;
  }
  arr[0] = obj;
  i = 0;
  while (i + 1 < *count && arr[i].size > arr[i + 1].size) {
    top_objects_swap(&arr[i], &arr[i + 1]);
    i++;
  }
}

static char *build_reference_analyze_output(heap_ctx_t *ctx, jclass klass,
                                            jint object_num,
                                            jint backtrace_num) {
  sb_t sb;
  jint i;
  jlong klass_tag = 0;
  jint target_class_id = 0;
  top_object_t *top_target = NULL;
  jint top_count = 0;

  sb_init(&sb);

  if (object_num < 0) {
    object_num = 0;
  }

  if (JVMTI_CALL(ctx->jvmti, GetTag, klass, &klass_tag) != JVMTI_ERROR_NONE) {
    sb_append_cstr(&sb, "ERROR: JVMTI GetTag(klass) failed\n");
    return sb.buf;
  }
  target_class_id = decode_class_id(ctx, klass_tag);
  if (target_class_id <= 0) {
    sb_append_cstr(&sb, "ERROR: can not resolve class tag\n");
    return sb.buf;
  }

  if (object_num > 0) {
    top_target =
        (top_object_t *)calloc((size_t)object_num, sizeof(top_object_t));
  }

  if (top_target) {
    jint oid;
    for (oid = 1; oid <= ctx->object_tag_count; oid++) {
      if (ctx->objects[oid].class_id == target_class_id) {
        top_target_add(top_target, object_num, &top_count, ctx->objects[oid].size,
                       oid);
      }
    }
  }

  sb_printf(&sb, "\n%-4s\t%-10s\t%s\n", "id", "#bytes",
            backtrace_num ? "class_name & references" : "class_name");
  sb_printf(&sb, "----------------------------------------------------\n");

  for (i = 0; i < top_count; i++) {
    jint idx = top_count - 1 - i;
    jint oid = top_target[idx].class_id;
    jlong size = top_target[idx].size;
    jlong cur_tag = make_tag(ctx, HEAP_TAG_TYPE_OBJECT, oid);
    jlong next_tag = ctx->objects[oid].referrer_tag;

    sb_printf(&sb, "%-4d\t%-10lld\t%s", (int)(i + 1), (long long)size,
              class_name_by_id(ctx, ctx->objects[oid].class_id));

    if (backtrace_num != 0) {
      int steps = 0;
      int max_steps = (backtrace_num == -1) ? INT_MAX : (backtrace_num - 1);
      while (next_tag != 0 && steps < max_steps) {
        jint cid = 0;
        if (!get_class_id_for_tag(ctx, next_tag, &cid)) {
          next_tag = -1;
          break;
        }
        sb_printf(&sb, " <-- %s", class_name_by_id(ctx, cid));
        cur_tag = next_tag;
        if (!get_referrer_for_tag(ctx, cur_tag, &next_tag)) {
          next_tag = -1;
          break;
        }
        steps++;
      }

      if (next_tag == 0) {
        jmethodID method = NULL;
        sb_append_cstr(&sb, " <-- root");
        if (get_root_method_for_tag(ctx, cur_tag, &method) && method) {
          char *name = NULL;
          if (JVMTI_CALL(ctx->jvmti, GetMethodName, method, &name, NULL, NULL) ==
                  JVMTI_ERROR_NONE &&
              name) {
            sb_printf(&sb, "(local variable in method: %s)\n", name);
            JVMTI_CALL(ctx->jvmti, Deallocate, (unsigned char *)name);
          } else {
            sb_append_cstr(&sb, "\n");
          }
        } else {
          sb_append_cstr(&sb, "\n");
        }
      } else {
        sb_append_cstr(&sb, " <-- ...\n");
      }
    } else {
      sb_append_cstr(&sb, "\n");
    }
  }

  sb_append_cstr(&sb, "\n");

  if (top_target) {
    free(top_target);
  }

  if (sb.oom) {
    sb_free(&sb);
    return NULL;
  }

  return sb.buf;
}

char *arthas_vmtool_heap_analyze(jvmtiEnv *jvmti, jint class_num,
                                 jint object_num) {
  heap_ctx_t ctx;
  jvmtiHeapCallbacks callbacks;
  jvmtiError err;
  jlong run_id;
  char *result = NULL;

  memset(&ctx, 0, sizeof(ctx));
  ctx.jvmti = jvmti;

  run_id = (++g_heap_analyzer_run_counter) & 0xFFFF;
  if (run_id == 0) {
    run_id = 1;
  }
  ctx.tag_magic_bits = ((jlong)HEAP_TAG_MAGIC) << HEAP_TAG_MAGIC_SHIFT;
  ctx.tag_run_bits = ((jlong)run_id) << HEAP_TAG_RUN_SHIFT;

  if (object_num > 0) {
    ctx.top_object_max = object_num;
    ctx.top_objects =
        (top_object_t *)calloc((size_t)object_num, sizeof(top_object_t));
    if (!ctx.top_objects) {
      return NULL;
    }
  }

  if (!prepare_classes(&ctx)) {
    free_ctx(&ctx);
    return NULL;
  }

  memset(&callbacks, 0, sizeof(callbacks));
  callbacks.heap_reference_callback = heap_reference_callback;
  err = JVMTI_CALL(jvmti, FollowReferences, 0, NULL, NULL, &callbacks,
                   (void *)&ctx);
  (void)err;

  if (ctx.callback_error) {
    sb_t sb;
    sb_init(&sb);
    sb_append_cstr(&sb, "ERROR: heapAnalyze aborted (native OOM)\n");
    result = sb.buf;
  } else {
  result = build_heap_analyze_output(&ctx, class_num, object_num);
  }

  restore_tags(&ctx);
  free_ctx(&ctx);
  return result;
}

char *arthas_vmtool_reference_analyze(jvmtiEnv *jvmti, jclass klass,
                                      jint object_num, jint backtrace_num) {
  heap_ctx_t ctx;
  jvmtiHeapCallbacks callbacks;
  jvmtiError err;
  jlong run_id;
  char *result = NULL;

  memset(&ctx, 0, sizeof(ctx));
  ctx.jvmti = jvmti;

  run_id = (++g_heap_analyzer_run_counter) & 0xFFFF;
  if (run_id == 0) {
    run_id = 1;
  }
  ctx.tag_magic_bits = ((jlong)HEAP_TAG_MAGIC) << HEAP_TAG_MAGIC_SHIFT;
  ctx.tag_run_bits = ((jlong)run_id) << HEAP_TAG_RUN_SHIFT;

  if (!prepare_classes(&ctx)) {
    free_ctx(&ctx);
    return NULL;
  }

  memset(&callbacks, 0, sizeof(callbacks));
  callbacks.heap_reference_callback = heap_reference_callback;
  err = JVMTI_CALL(jvmti, FollowReferences, 0, NULL, NULL, &callbacks,
                   (void *)&ctx);
  (void)err;

  if (ctx.callback_error) {
    sb_t sb;
    sb_init(&sb);
    sb_append_cstr(&sb, "ERROR: referenceAnalyze aborted (native OOM)\n");
    result = sb.buf;
  } else {
    result =
        build_reference_analyze_output(&ctx, klass, object_num, backtrace_num);
  }

  restore_tags(&ctx);
  free_ctx(&ctx);
  return result;
}
