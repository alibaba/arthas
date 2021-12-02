### 分析占用最大堆内存的对象和类

`vmtool -a heapAnalyze --classNum 5 --objectNum 3`{{execute T2}}

> 通过 `--classNum` 参数指定输出的类数量，通过 `--objectNum` 参数指定输出的对象数量。

### 分析对象间引用关系

`vmtool -a referenceAnalyze --className java.lang.String --objectNum 2 --backtraceNum -1`{{execute T2}}

> 通过 `--className` 参数指定类名，通过 `--objectNum` 参数指定输出的对象数量，通过 `--backtraceNum` 参数指定回溯对象引用关系的层级，如果 `--backtraceNum` 被设置为-1，则表示不断回溯，直到找到根引用。

> `getInstances` 中的 `classLoaderClass` 和 `classloader` 参数在此也适用。

> 如果对象的根引用是线程栈，那么在输出的中会显式输出该引用所处的栈帧方法名，而当对象的根引用来自其他位置，例如JNI栈帧时，无法获得其方法名，只会输出 `root` 。
