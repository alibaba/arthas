Check the current JVM Perf Counter information.

`perfcounter -h`{{execute T2}}

[perfcounter command Docs](https://arthas.aliyun.com/en/doc/perfcounter.html)

## Usage

`perfcounter`{{execute T2}}

Print more information with the `-d` option:

`perfcounter -d`{{execute T2}}

## PS: for JVM above JDK9

If the information is not printed, when the application starts, add the following parameters:

`--add-opens java.base/jdk.internal.perf=ALL-UNNAMED --add-exports java.base/jdk.internal.perf=ALL-UNNAMED --add-opens java.management/sun.management.counter.perf=ALL-UNNAMED --add-opens java.management/sun.management.counter=ALL-UNNAMED`
