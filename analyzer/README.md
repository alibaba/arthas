## Memory Analyzer
MemoryAnalyzer能够在不dump的前提下，获取：
 * 1.某个class在jvm中当前所有存活实例;
 * 2.某个class在jvm中当前所有存活实例的总占用内存;
 * 3.某个实例的占用内存;
 * 4.某个class在jvm中当前所有存活实例的总个数;
 * 5.所有已加载的类(不包括void、int、boolean、float等小类型);
 * 6.系统中占用内存最多的类及具体MB.
 
 具体使用可参考测试用例`com.vdian.vclub.MemoryAnalyzerTest`;
 
 如果您需要在`com.vdian.vclub.MemoryAnalyzer`添加新的native方法，请按照以下步骤：
 * 1.进入analyzer模块的main文件夹，执行命令javah com.vdian.vclub.MemoryAnalyzer;
 * 2.把生成的.h文件移动到cpp/head文件夹下;
 * 3.编写jni-library.cpp，实现您新加的native方法;
 * 4.进入cpp文件夹，执行命令gcc -I $JAVA_HOME/include -I $JAVA_HOME/include/darwin -I /Users/xxxx/Desktop/JNIDemo/src/main/cpp/head jni-library.cpp -fPIC -shared -o jni-lib.so把c++类库打包成.so文件;
 * 5.把.so文件copy到resource目录下，然后就可以使用了;
