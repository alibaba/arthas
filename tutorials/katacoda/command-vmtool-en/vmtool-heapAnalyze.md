### Analyze heap usage

`vmtool -a heapAnalyze --classNum 5 --objectNum 3`{{execute T2}}

> Use the `--classNum` parameter to specify classes that will be shown, use the `--objectNum` parameter to specify objects that will be shown.

# Analyze reference

Use `referenceAnalyze` to show reference among objects to help locate them.

`vmtool -a referenceAnalyze --className ByteHolder --objectNum 2 --backtraceNum -1`{{execute T2}}

> Use the `--className` parameter to specify class name, use the `--objectNum` parameter to specify objects that will be shown, use the `--backtraceNum` parameter to specify how many times of backtrace by references among objects will be done, and set `--backtraceNum` as -1 to make backtrace do not finish until root is reached.

> `classLoaderClass` and `classloader` in `getInstances` is applicable here.

> If the root reference(the first reference from root, such as stack) of objects is stack of java threads, then the method name will be printed, otherwise only `root` will be printed.
