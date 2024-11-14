import{_ as e,o as l,c as t,a,b as c,e as s,d as i,r as o}from"./app.1e03d722.js";const p={},r=a("h1",{id:"logger",tabindex:"-1"},[a("a",{class:"header-anchor",href:"#logger","aria-hidden":"true"},"#"),s(" logger")],-1),d={href:"https://arthas.aliyun.com/doc/arthas-tutorials?language=en&id=command-logger",target:"_blank",rel:"noopener noreferrer"},u=a("code",null,"logger",-1),v=s(" online tutorial"),g=i(`<div class="custom-container tip"><p class="custom-container-title">TIP</p><p>Print the logger information, update the logger level</p></div><h2 id="usage" tabindex="-1"><a class="header-anchor" href="#usage" aria-hidden="true">#</a> Usage</h2><h3 id="print-the-logger-information" tabindex="-1"><a class="header-anchor" href="#print-the-logger-information" aria-hidden="true">#</a> Print the logger information</h3><p>Take the following <code>logback.xml</code> as an example:</p><div class="language-xml ext-xml line-numbers-mode"><pre class="language-xml"><code><span class="token prolog">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;</span>
<span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>configuration</span><span class="token punctuation">&gt;</span></span>
    <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>appender</span> <span class="token attr-name">name</span><span class="token attr-value"><span class="token punctuation attr-equals">=</span><span class="token punctuation">&quot;</span>APPLICATION<span class="token punctuation">&quot;</span></span> <span class="token attr-name">class</span><span class="token attr-value"><span class="token punctuation attr-equals">=</span><span class="token punctuation">&quot;</span>ch.qos.logback.core.rolling.RollingFileAppender<span class="token punctuation">&quot;</span></span><span class="token punctuation">&gt;</span></span>
        <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>file</span><span class="token punctuation">&gt;</span></span>app.log<span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>file</span><span class="token punctuation">&gt;</span></span>
        <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>rollingPolicy</span> <span class="token attr-name">class</span><span class="token attr-value"><span class="token punctuation attr-equals">=</span><span class="token punctuation">&quot;</span>ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy<span class="token punctuation">&quot;</span></span><span class="token punctuation">&gt;</span></span>
            <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>fileNamePattern</span><span class="token punctuation">&gt;</span></span>mylog-%d{yyyy-MM-dd}.%i.txt<span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>fileNamePattern</span><span class="token punctuation">&gt;</span></span>
            <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>maxFileSize</span><span class="token punctuation">&gt;</span></span>100MB<span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>maxFileSize</span><span class="token punctuation">&gt;</span></span>
            <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>maxHistory</span><span class="token punctuation">&gt;</span></span>60<span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>maxHistory</span><span class="token punctuation">&gt;</span></span>
            <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>totalSizeCap</span><span class="token punctuation">&gt;</span></span>2GB<span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>totalSizeCap</span><span class="token punctuation">&gt;</span></span>
        <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>rollingPolicy</span><span class="token punctuation">&gt;</span></span>
        <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>encoder</span><span class="token punctuation">&gt;</span></span>
            <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>pattern</span><span class="token punctuation">&gt;</span></span>%logger{35} - %msg%n<span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>pattern</span><span class="token punctuation">&gt;</span></span>
        <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>encoder</span><span class="token punctuation">&gt;</span></span>
    <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>appender</span><span class="token punctuation">&gt;</span></span>

    <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>appender</span> <span class="token attr-name">name</span><span class="token attr-value"><span class="token punctuation attr-equals">=</span><span class="token punctuation">&quot;</span>ASYNC<span class="token punctuation">&quot;</span></span> <span class="token attr-name">class</span><span class="token attr-value"><span class="token punctuation attr-equals">=</span><span class="token punctuation">&quot;</span>ch.qos.logback.classic.AsyncAppender<span class="token punctuation">&quot;</span></span><span class="token punctuation">&gt;</span></span>
        <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>appender-ref</span> <span class="token attr-name">ref</span><span class="token attr-value"><span class="token punctuation attr-equals">=</span><span class="token punctuation">&quot;</span>APPLICATION<span class="token punctuation">&quot;</span></span> <span class="token punctuation">/&gt;</span></span>
    <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>appender</span><span class="token punctuation">&gt;</span></span>

    <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>appender</span> <span class="token attr-name">name</span><span class="token attr-value"><span class="token punctuation attr-equals">=</span><span class="token punctuation">&quot;</span>CONSOLE<span class="token punctuation">&quot;</span></span> <span class="token attr-name">class</span><span class="token attr-value"><span class="token punctuation attr-equals">=</span><span class="token punctuation">&quot;</span>ch.qos.logback.core.ConsoleAppender<span class="token punctuation">&quot;</span></span><span class="token punctuation">&gt;</span></span>
        <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>encoder</span><span class="token punctuation">&gt;</span></span>
            <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>pattern</span><span class="token punctuation">&gt;</span></span>%-4relative [%thread] %-5level %logger{35} - %msg %n
            <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>pattern</span><span class="token punctuation">&gt;</span></span>
            <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>charset</span><span class="token punctuation">&gt;</span></span>utf8<span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>charset</span><span class="token punctuation">&gt;</span></span>
        <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>encoder</span><span class="token punctuation">&gt;</span></span>
    <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>appender</span><span class="token punctuation">&gt;</span></span>

    <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>root</span> <span class="token attr-name">level</span><span class="token attr-value"><span class="token punctuation attr-equals">=</span><span class="token punctuation">&quot;</span>INFO<span class="token punctuation">&quot;</span></span><span class="token punctuation">&gt;</span></span>
        <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>appender-ref</span> <span class="token attr-name">ref</span><span class="token attr-value"><span class="token punctuation attr-equals">=</span><span class="token punctuation">&quot;</span>CONSOLE<span class="token punctuation">&quot;</span></span> <span class="token punctuation">/&gt;</span></span>
        <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;</span>appender-ref</span> <span class="token attr-name">ref</span><span class="token attr-value"><span class="token punctuation attr-equals">=</span><span class="token punctuation">&quot;</span>ASYNC<span class="token punctuation">&quot;</span></span> <span class="token punctuation">/&gt;</span></span>
    <span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>root</span><span class="token punctuation">&gt;</span></span>
<span class="token tag"><span class="token tag"><span class="token punctuation">&lt;/</span>configuration</span><span class="token punctuation">&gt;</span></span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>The result of the <code>logger</code> command:</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code><span class="token punctuation">[</span>arthas@2062<span class="token punctuation">]</span>$ logger
 name                                   ROOT
 class                                  ch.qos.logback.classic.Logger
 classLoader                            sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@2a139a55
 classLoaderHash                        2a139a55
 level                                  INFO
 effectiveLevel                         INFO
 additivity                             <span class="token boolean">true</span>
 codeSource                             file:/Users/hengyunabc/.m2/repository/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar
 appenders                              name            CONSOLE
                                        class           ch.qos.logback.core.ConsoleAppender
                                        classLoader     sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@2a139a55
                                        classLoaderHash 2a139a55
                                        target          System.out
                                        name            APPLICATION
                                        class           ch.qos.logback.core.rolling.RollingFileAppender
                                        classLoader     sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@2a139a55
                                        classLoaderHash 2a139a55
                                        <span class="token function">file</span>            app.log
                                        name            ASYNC
                                        class           ch.qos.logback.classic.AsyncAppender
                                        classLoader     sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@2a139a55
                                        classLoaderHash 2a139a55
                                        appenderRef     <span class="token punctuation">[</span>APPLICATION<span class="token punctuation">]</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>In the <code>appenders</code> section:</p><ul><li>The target of <code>CONSOLE</code> logger is <code>System.out</code></li><li><code>APPLICATION</code> logger is <code>RollingFileAppender</code>, the file is <code>app.log</code></li><li><code>ASYNC</code> its <code>appenderRef</code> is <code>APPLICATION</code>, which means asynchronous output to the file</li></ul><h3 id="view-logger-information-for-the-special-name" tabindex="-1"><a class="header-anchor" href="#view-logger-information-for-the-special-name" aria-hidden="true">#</a> View logger information for the special name</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code><span class="token punctuation">[</span>arthas@2062<span class="token punctuation">]</span>$ logger <span class="token parameter variable">-n</span> org.springframework.web
 name                                   org.springframework.web
 class                                  ch.qos.logback.classic.Logger
 classLoader                            sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@2a139a55
 classLoaderHash                        2a139a55
 level                                  null
 effectiveLevel                         INFO
 additivity                             <span class="token boolean">true</span>
 codeSource                             file:/Users/hengyunabc/.m2/repository/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="view-logger-information-for-the-special-classloader" tabindex="-1"><a class="header-anchor" href="#view-logger-information-for-the-special-classloader" aria-hidden="true">#</a> View logger information for the special classloader</h3><p>Note that the hashcode changes, you need to check the current ClassLoader information first, and extract the hashcode corresponding to the ClassLoader.</p><p>if you use<code>-c</code>, you have to manually type hashcode by <code>-c &lt;hashcode&gt;</code>.</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code><span class="token punctuation">[</span>arthas@2062<span class="token punctuation">]</span>$ logger <span class="token parameter variable">-c</span> 2a139a55
 name                                   ROOT
 class                                  ch.qos.logback.classic.Logger
 classLoader                            sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@2a139a55
 classLoaderHash                        2a139a55
 level                                  DEBUG
 effectiveLevel                         DEBUG
 additivity                             <span class="token boolean">true</span>
 codeSource                             file:/Users/hengyunabc/.m2/repository/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar
 appenders                              name            CONSOLE
                                        class           ch.qos.logback.core.ConsoleAppender
                                        classLoader     sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@2a139a55
                                        classLoaderHash 2a139a55
                                        target          System.out
                                        name            APPLICATION
                                        class           ch.qos.logback.core.rolling.RollingFileAppender
                                        classLoader     sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@2a139a55
                                        classLoaderHash 2a139a55
                                        <span class="token function">file</span>            app.log
                                        name            ASYNC
                                        class           ch.qos.logback.classic.AsyncAppender
                                        classLoader     sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@2a139a55
                                        classLoaderHash 2a139a55
                                        appenderRef     <span class="token punctuation">[</span>APPLICATION<span class="token punctuation">]</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>For classloader with only one instance, it can be specified by <code>--classLoaderClass</code> using class name, which is more convenient to use.</p><p><code>logger --classLoaderClass sun.misc.Launcher$AppClassLoader</code></p><ul><li>PS: Here the classLoaderClass in java 8 is sun.misc.Launcher$AppClassLoader, while in java 11 it&#39;s jdk.internal.loader.ClassLoaders$AppClassLoader.</li></ul><p>The value of <code>--classloaderclass</code> is the class name of classloader. It can only work when it matches a unique classloader instance. The purpose is to facilitate the input of general commands. However, <code>-c &lt;hashcode&gt;</code> is dynamic.</p><h3 id="update-logger-level" tabindex="-1"><a class="header-anchor" href="#update-logger-level" aria-hidden="true">#</a> Update logger level</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code><span class="token punctuation">[</span>arthas@2062<span class="token punctuation">]</span>$ logger <span class="token parameter variable">--name</span> ROOT <span class="token parameter variable">--level</span> debug
update logger level success.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="speecify-classloader-to-update-logger-level" tabindex="-1"><a class="header-anchor" href="#speecify-classloader-to-update-logger-level" aria-hidden="true">#</a> Speecify classloader to update logger level</h3><p>By default\uFF0Clogger command will be executed under SystemClassloader, if the application is a traditional <code>war</code>, or using spring boot fat jar, then it needs to specify classloader\u3002</p><p>You can first use <code>sc -d yourClassName</code> to check specified classloader hashcode\uFF0Cthen specify classloader when updating logger level:</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code><span class="token punctuation">[</span>arthas@2062<span class="token punctuation">]</span>$ logger <span class="token parameter variable">-c</span> 2a139a55 <span class="token parameter variable">--name</span> ROOT <span class="token parameter variable">--level</span> debug
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div></div></div><h3 id="view-the-logger-information-without-appenders" tabindex="-1"><a class="header-anchor" href="#view-the-logger-information-without-appenders" aria-hidden="true">#</a> View the logger information without appenders</h3><p>By default, the <code>logger</code> command only prints information about the logger with appenders. If you want to see information about loggers without <code>appender</code>, you can use the parameter <code>--include-no-appender</code>.</p><p>Note that the output will usually be very long.</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code><span class="token punctuation">[</span>arthas@2062<span class="token punctuation">]</span>$ logger --include-no-appender
 name                                   ROOT
 class                                  ch.qos.logback.classic.Logger
 classLoader                            sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@2a139a55
 classLoaderHash                        2a139a55
 level                                  DEBUG
 effectiveLevel                         DEBUG
 additivity                             <span class="token boolean">true</span>
 codeSource                             file:/Users/hengyunabc/.m2/repository/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar
 appenders                              name            CONSOLE
                                        class           ch.qos.logback.core.ConsoleAppender
                                        classLoader     sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@2a139a55
                                        classLoaderHash 2a139a55
                                        target          System.out
                                        name            APPLICATION
                                        class           ch.qos.logback.core.rolling.RollingFileAppender
                                        classLoader     sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@2a139a55
                                        classLoaderHash 2a139a55
                                        <span class="token function">file</span>            app.log
                                        name            ASYNC
                                        class           ch.qos.logback.classic.AsyncAppender
                                        classLoader     sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@2a139a55
                                        classLoaderHash 2a139a55
                                        appenderRef     <span class="token punctuation">[</span>APPLICATION<span class="token punctuation">]</span>

 name                                   com
 class                                  ch.qos.logback.classic.Logger
 classLoader                            sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@2a139a55
 classLoaderHash                        2a139a55
 level                                  null
 effectiveLevel                         DEBUG
 additivity                             <span class="token boolean">true</span>
 codeSource                             file:/Users/hengyunabc/.m2/repository/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar

 name                                   com.alibaba
 class                                  ch.qos.logback.classic.Logger
 classLoader                            sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@2a139a55
 classLoaderHash                        2a139a55
 level                                  null
 effectiveLevel                         DEBUG
 additivity                             <span class="token boolean">true</span>
 codeSource                             file:/Users/hengyunabc/.m2/repository/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar
<span class="token punctuation">..</span>.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,29);function k(m,b){const n=o("ExternalLinkIcon");return l(),t("div",null,[r,a("p",null,[a("a",d,[u,v,c(n)])]),g])}const f=e(p,[["render",k],["__file","logger.html.vue"]]);export{f as default};
