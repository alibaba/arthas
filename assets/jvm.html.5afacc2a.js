import{_ as l,o as s,c as d,a as i,b as a,e as n,d as v,r}from"./app.1e03d722.js";const c={},m=i("h1",{id:"jvm",tabindex:"-1"},[i("a",{class:"header-anchor",href:"#jvm","aria-hidden":"true"},"#"),n(" jvm")],-1),u={href:"https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-jvm",target:"_blank",rel:"noopener noreferrer"},b=i("code",null,"jvm",-1),t=n("\u5728\u7EBF\u6559\u7A0B"),o=v(`<div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>\u67E5\u770B\u5F53\u524D JVM \u4FE1\u606F</p></div><h2 id="\u4F7F\u7528\u53C2\u8003" tabindex="-1"><a class="header-anchor" href="#\u4F7F\u7528\u53C2\u8003" aria-hidden="true">#</a> \u4F7F\u7528\u53C2\u8003</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ jvm
RUNTIME
--------------------------------------------------------------------------------------------------------------
 MACHINE-NAME                   37@ff267334bb65
 JVM-START-TIME                 2020-07-23 07:50:36
 MANAGEMENT-SPEC-VERSION        1.2
 SPEC-NAME                      Java Virtual Machine Specification
 SPEC-VENDOR                    Oracle Corporation
 SPEC-VERSION                   1.8
 VM-NAME                        Java HotSpot(TM) 64-Bit Server VM
 VM-VENDOR                      Oracle Corporation
 VM-VERSION                     25.201-b09
 INPUT-ARGUMENTS                []
 CLASS-PATH                     demo-arthas-spring-boot.jar
 BOOT-CLASS-PATH                /usr/lib/jvm/java-8-oracle/jre/lib/resources.jar:/usr/lib/jvm/java-8-oracle/j
                                re/lib/rt.jar:/usr/lib/jvm/java-8-oracle/jre/lib/sunrsasign.jar:/usr/lib/jvm/
                                java-8-oracle/jre/lib/jsse.jar:/usr/lib/jvm/java-8-oracle/jre/lib/jce.jar:/us
                                r/lib/jvm/java-8-oracle/jre/lib/charsets.jar:/usr/lib/jvm/java-8-oracle/jre/l
                                ib/jfr.jar:/usr/lib/jvm/java-8-oracle/jre/classes
 LIBRARY-PATH                   /usr/java/packages/lib/amd64:/usr/lib64:/lib64:/lib:/usr/lib

--------------------------------------------------------------------------------------------------------------
 CLASS-LOADING
--------------------------------------------------------------------------------------------------------------
 LOADED-CLASS-COUNT             7529
 TOTAL-LOADED-CLASS-COUNT       7529
 UNLOADED-CLASS-COUNT           0
 IS-VERBOSE                     false

--------------------------------------------------------------------------------------------------------------
 COMPILATION
--------------------------------------------------------------------------------------------------------------
 NAME                           HotSpot 64-Bit Tiered Compilers
 TOTAL-COMPILE-TIME             14921(ms)

--------------------------------------------------------------------------------------------------------------
 GARBAGE-COLLECTORS
--------------------------------------------------------------------------------------------------------------
 PS Scavenge                            name : PS Scavenge
 [count/time (ms)]                      collectionCount : 7
                                        collectionTime : 68

 PS MarkSweep                           name : PS MarkSweep
 [count/time (ms)]                      collectionCount : 1
                                        collectionTime : 47

--------------------------------------------------------------------------------------------------------------
 MEMORY-MANAGERS
--------------------------------------------------------------------------------------------------------------
 CodeCacheManager               Code Cache

 Metaspace Manager              Metaspace
                                Compressed Class Space

 Copy                           Eden Space
                                Survivor Space

 MarkSweepCompact               Eden Space
                                Survivor Space
                                Tenured Gen


--------------------------------------------------------------------------------------------------------------
 MEMORY
--------------------------------------------------------------------------------------------------------------
 HEAP-MEMORY-USAGE                      init : 268435456(256.0 MiB)
 [memory in bytes]                      used : 18039504(17.2 MiB)
                                        committed : 181403648(173.0 MiB)
                                        max : 3817865216(3.6 GiB)

 NO-HEAP-MEMORY-USAGE                   init : 2555904(2.4 MiB)
 [memory in bytes]                      used : 33926216(32.4 MiB)
                                        committed : 35176448(33.5 MiB)
                                        max : -1(-1 B)

--------------------------------------------------------------------------------------------------------------
 OPERATING-SYSTEM
--------------------------------------------------------------------------------------------------------------
 OS                             Linux
 ARCH                           amd64
 PROCESSORS-COUNT               3
 LOAD-AVERAGE                   29.53
 VERSION                        4.15.0-52-generic

--------------------------------------------------------------------------------------------------------------
 THREAD
--------------------------------------------------------------------------------------------------------------
 COUNT                          30
 DAEMON-COUNT                   24
 PEAK-COUNT                     31
 STARTED-COUNT                  36
 DEADLOCK-COUNT                 0

--------------------------------------------------------------------------------------------------------------
 FILE-DESCRIPTOR
--------------------------------------------------------------------------------------------------------------
 MAX-FILE-DESCRIPTOR-COUNT      1048576
 OPEN-FILE-DESCRIPTOR-COUNT     100
Affect(row-cnt:0) cost in 88 ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h2 id="thread-\u76F8\u5173" tabindex="-1"><a class="header-anchor" href="#thread-\u76F8\u5173" aria-hidden="true">#</a> THREAD \u76F8\u5173</h2><ul><li>COUNT: JVM \u5F53\u524D\u6D3B\u8DC3\u7684\u7EBF\u7A0B\u6570</li><li>DAEMON-COUNT: JVM \u5F53\u524D\u6D3B\u8DC3\u7684\u5B88\u62A4\u7EBF\u7A0B\u6570</li><li>PEAK-COUNT: \u4ECE JVM \u542F\u52A8\u5F00\u59CB\u66FE\u7ECF\u6D3B\u7740\u7684\u6700\u5927\u7EBF\u7A0B\u6570</li><li>STARTED-COUNT: \u4ECE JVM \u542F\u52A8\u5F00\u59CB\u603B\u5171\u542F\u52A8\u8FC7\u7684\u7EBF\u7A0B\u6B21\u6570</li><li>DEADLOCK-COUNT: JVM \u5F53\u524D\u6B7B\u9501\u7684\u7EBF\u7A0B\u6570</li></ul><h2 id="\u6587\u4EF6\u63CF\u8FF0\u7B26\u76F8\u5173" tabindex="-1"><a class="header-anchor" href="#\u6587\u4EF6\u63CF\u8FF0\u7B26\u76F8\u5173" aria-hidden="true">#</a> \u6587\u4EF6\u63CF\u8FF0\u7B26\u76F8\u5173</h2><ul><li>MAX-FILE-DESCRIPTOR-COUNT\uFF1AJVM \u8FDB\u7A0B\u6700\u5927\u53EF\u4EE5\u6253\u5F00\u7684\u6587\u4EF6\u63CF\u8FF0\u7B26\u6570</li><li>OPEN-FILE-DESCRIPTOR-COUNT\uFF1AJVM \u5F53\u524D\u6253\u5F00\u7684\u6587\u4EF6\u63CF\u8FF0\u7B26\u6570</li></ul>`,7);function E(S,O){const e=r("ExternalLinkIcon");return s(),d("div",null,[m,i("p",null,[i("a",u,[b,t,a(e)])]),o])}const M=l(c,[["render",E],["__file","jvm.html.vue"]]);export{M as default};
