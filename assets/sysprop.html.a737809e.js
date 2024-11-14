import{_ as a,o as s,c as r,a as e,b as d,e as n,d as l,r as v}from"./app.1e03d722.js";const c={},t=e("h1",{id:"sysprop",tabindex:"-1"},[e("a",{class:"header-anchor",href:"#sysprop","aria-hidden":"true"},"#"),n(" sysprop")],-1),o={href:"https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-sysprop",target:"_blank",rel:"noopener noreferrer"},u=e("code",null,"sysprop",-1),m=n("\u5728\u7EBF\u6559\u7A0B"),b=l(`<div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>\u67E5\u770B\u5F53\u524D JVM \u7684\u7CFB\u7EDF\u5C5E\u6027(<code>System Property</code>)</p></div><h2 id="\u4F7F\u7528\u53C2\u8003" tabindex="-1"><a class="header-anchor" href="#\u4F7F\u7528\u53C2\u8003" aria-hidden="true">#</a> \u4F7F\u7528\u53C2\u8003</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code> USAGE:
   sysprop [-h] [property-name] [property-value]

 SUMMARY:
   Display, and change all the system properties.

 EXAMPLES:
 sysprop
 sysprop file.encoding
 sysprop production.mode true

 WIKI:
   https://arthas.aliyun.com/doc/sysprop

 OPTIONS:
 -h, --help                                  this help
 &lt;property-name&gt;                             property name
 &lt;property-value&gt;                            property value
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="\u67E5\u770B\u6240\u6709\u5C5E\u6027" tabindex="-1"><a class="header-anchor" href="#\u67E5\u770B\u6240\u6709\u5C5E\u6027" aria-hidden="true">#</a> \u67E5\u770B\u6240\u6709\u5C5E\u6027</h3><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ sysprop
 KEY                                                  VALUE
-------------------------------------------------------------------------------------------------------------------------------------
 java.runtime.name                                    Java(TM) SE Runtime Environment
 sun.boot.library.path                                /Library/Java/JavaVirtualMachines/jdk1.8.0_51.jdk/Contents/Home/jre/lib
 java.vm.version                                      25.51-b03
 user.country.format                                  CN
 gopherProxySet                                       false
 java.vm.vendor                                       Oracle Corporation
 java.vendor.url                                      http://java.oracle.com/
 path.separator                                       :
 java.vm.name                                         Java HotSpot(TM) 64-Bit Server VM
 file.encoding.pkg                                    sun.io
 user.country                                         US
 sun.java.launcher                                    SUN_STANDARD
 sun.os.patch.level                                   unknown
 java.vm.specification.name                           Java Virtual Machine Specification
 user.dir                                             /private/var/tmp
 java.runtime.version                                 1.8.0_51-b16
 java.awt.graphicsenv                                 sun.awt.CGraphicsEnvironment
 java.endorsed.dirs                                   /Library/Java/JavaVirtualMachines/jdk1.8.0_51.jdk/Contents/Home/jre/lib/endors
                                                      ed
 os.arch                                              x86_64
 java.io.tmpdir                                       /var/folders/2c/tbxwzs4s4sbcvh7frbcc7n000000gn/T/
 line.separator

 java.vm.specification.vendor                         Oracle Corporation
 os.name                                              Mac OS X
 sun.jnu.encoding                                     UTF-8
 java.library.path                                    /Users/wangtao/Library/Java/Extensions:/Library/Java/Extensions:/Network/Libra
                                                      ry/Java/Extensions:/System/Library/Java/Extensions:/usr/lib/java:.
 sun.nio.ch.bugLevel
 java.specification.name                              Java Platform API Specification
 java.class.version                                   52.0
 sun.management.compiler                              HotSpot 64-Bit Tiered Compilers
 os.version                                           10.12.6
 user.home                                            /Users/wangtao
 user.timezone                                        Asia/Shanghai
 java.awt.printerjob                                  sun.lwawt.macosx.CPrinterJob
 file.encoding                                        UTF-8
 java.specification.version                           1.8
 user.name                                            wangtao
 java.class.path                                      .
 java.vm.specification.version                        1.8
 sun.arch.data.model                                  64
 java.home                                            /Library/Java/JavaVirtualMachines/jdk1.8.0_51.jdk/Contents/Home/jre
 sun.java.command                                     Test
 java.specification.vendor                            Oracle Corporation
 user.language                                        en
 awt.toolkit                                          sun.lwawt.macosx.LWCToolkit
 java.vm.info                                         mixed mode
 java.version                                         1.8.0_51
 java.ext.dirs                                        /Users/wangtao/Library/Java/Extensions:/Library/Java/JavaVirtualMachines/jdk1.
                                                      8.0_51.jdk/Contents/Home/jre/lib/ext:/Library/Java/Extensions:/Network/Library
                                                      /Java/Extensions:/System/Library/Java/Extensions:/usr/lib/java
 sun.boot.class.path                                  /Library/Java/JavaVirtualMachines/jdk1.8.0_51.jdk/Contents/Home/jre/lib/resour
                                                      ces.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_51.jdk/Contents/Home/jre/li
                                                      b/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_51.jdk/Contents/Home/jre/l
                                                      ib/sunrsasign.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_51.jdk/Contents/H
                                                      ome/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_51.jdk/Content
                                                      s/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_51.jdk/Conte
                                                      nts/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_51.jd
                                                      k/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_51.
                                                      jdk/Contents/Home/jre/classes
 java.vendor                                          Oracle Corporation
 file.separator                                       /
 java.vendor.url.bug                                  http://bugreport.sun.com/bugreport/
 sun.cpu.endian                                       little
 sun.io.unicode.encoding                              UnicodeBig
 sun.cpu.isalist
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="\u67E5\u770B\u5355\u4E2A\u5C5E\u6027" tabindex="-1"><a class="header-anchor" href="#\u67E5\u770B\u5355\u4E2A\u5C5E\u6027" aria-hidden="true">#</a> \u67E5\u770B\u5355\u4E2A\u5C5E\u6027</h3><div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>\u652F\u6301\u901A\u8FC7<code>TAB</code>\u952E\u81EA\u52A8\u8865\u5168</p></div><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ sysprop java.version
java.version=1.8.0_51
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="\u4FEE\u6539\u5355\u4E2A\u5C5E\u6027" tabindex="-1"><a class="header-anchor" href="#\u4FEE\u6539\u5355\u4E2A\u5C5E\u6027" aria-hidden="true">#</a> \u4FEE\u6539\u5355\u4E2A\u5C5E\u6027</h3><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ sysprop user.country
user.country=US
$ sysprop user.country CN
Successfully changed the system property.
user.country=CN
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,10);function p(h,j){const i=v("ExternalLinkIcon");return s(),r("div",null,[t,e("p",null,[e("a",o,[u,m,d(i)])]),b])}const g=a(c,[["render",p],["__file","sysprop.html.vue"]]);export{g as default};
