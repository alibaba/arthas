## 如果JvmUtils不支持您的系统或者在使用时遇到`java.lang.UnsatisfiedLinkError`

1. 在您的系统上安装合适版本的g++编译器

2. 进入机器上tomcat对应的WEB/lib/cpp

3. 使用g++命令生成linux对应的so文件

   g++ -I $JAVA_HOME/include -I $JAVA_HOME/include/linux jni-library.cpp -fPIC -shared -o jni-lib-linux.so

4. 使用`java.lang.System#load`重新加载第3步生成的so文件
