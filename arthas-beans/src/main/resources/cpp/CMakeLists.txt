cmake_minimum_required(VERSION 3.17)
project(cpp)

set(CMAKE_CXX_STANDARD 14)

add_library(jni-lib SHARED jni-library.cpp)

#使用环境变量来include，把不同系统的兼容问题交给jdk解决
include_directories("$ENV{JAVA_HOME}/include")

IF (WIN32)
    include_directories("$ENV{JAVA_HOME}/include/win32")
    include_directories("$ENV{JAVA_HOME}/include/win32/bridge")
ELSEIF (APPLE)
    include_directories("$ENV{JAVA_HOME}/include/darwin")
ELSEIF (LINUX)
    include_directories("$ENV{JAVA_HOME}/include/linux")
ENDIF ()