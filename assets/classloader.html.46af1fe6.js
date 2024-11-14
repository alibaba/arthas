import{_ as n,o as l,c as i,a,b as t,e as s,d as r,r as d}from"./app.1e03d722.js";const c={},o=a("h1",{id:"classloader",tabindex:"-1"},[a("a",{class:"header-anchor",href:"#classloader","aria-hidden":"true"},"#"),s(" classloader")],-1),u={href:"https://arthas.aliyun.com/doc/arthas-tutorials?language=en&id=command-classloader",target:"_blank",rel:"noopener noreferrer"},p=a("code",null,"classloader",-1),m=s(" online tutorial"),v=r(`<div class="custom-container tip"><p class="custom-container-title">TIP</p><p>View hierarchy, urls and classes-loading info for the class-loaders.</p></div><p><code>classloader</code> can search and print out the URLs for a specified resource from one particular classloader. It is quite handy when analyzing <code>ResourceNotFoundException</code>.</p><h2 id="options" tabindex="-1"><a class="header-anchor" href="#options" aria-hidden="true">#</a> Options</h2><table><thead><tr><th style="text-align:right;">Name</th><th style="text-align:left;">Specification</th></tr></thead><tbody><tr><td style="text-align:right;">[l]</td><td style="text-align:left;">list all classloader instances</td></tr><tr><td style="text-align:right;">[t]</td><td style="text-align:left;">print classloader&#39;s hierarchy</td></tr><tr><td style="text-align:right;">[a]</td><td style="text-align:left;">list all the classes loaded by all the classloaders (use it with great caution since the output can be huge)</td></tr><tr><td style="text-align:right;">[c:]</td><td style="text-align:left;">print classloader&#39;s hashcode</td></tr><tr><td style="text-align:right;"><code>[classLoaderClass:]</code></td><td style="text-align:left;">The class name of the ClassLoader that executes the expression.</td></tr><tr><td style="text-align:right;"><code>[c: r:]</code></td><td style="text-align:left;">using ClassLoader to search resource</td></tr><tr><td style="text-align:right;"><code>[c: load:]</code></td><td style="text-align:left;">using ClassLoader to load class</td></tr></tbody></table><h2 id="usage" tabindex="-1"><a class="header-anchor" href="#usage" aria-hidden="true">#</a> Usage</h2><h3 id="view-statistics-categorized-by-class-type" tabindex="-1"><a class="header-anchor" href="#view-statistics-categorized-by-class-type" aria-hidden="true">#</a> View statistics categorized by class type</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader
 name                                       numberOfInstances  loadedCountTotal
 com.taobao.arthas.agent.ArthasClassloader  <span class="token number">1</span>                  <span class="token number">2115</span>
 BootstrapClassLoader                       <span class="token number">1</span>                  <span class="token number">1861</span>
 sun.reflect.DelegatingClassLoader          <span class="token number">5</span>                  <span class="token number">5</span>
 sun.misc.Launcher<span class="token variable">$AppClassLoader</span>           <span class="token number">1</span>                  <span class="token number">4</span>
 sun.misc.Launcher<span class="token variable">$ExtClassLoader</span>           <span class="token number">1</span>                  <span class="token number">1</span>
Affect<span class="token punctuation">(</span>row-cnt:5<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">3</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="view-statistics-categorized-by-loaded-classes-number" tabindex="-1"><a class="header-anchor" href="#view-statistics-categorized-by-loaded-classes-number" aria-hidden="true">#</a> View statistics categorized by loaded classes number</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader <span class="token parameter variable">-l</span>
 name                                                loadedCount  <span class="token builtin class-name">hash</span>      parent
 BootstrapClassLoader                                <span class="token number">1861</span>         null      null
 com.taobao.arthas.agent.ArthasClassloader@68b31f0a  <span class="token number">2115</span>         68b31f0a  sun.misc.Launcher<span class="token variable">$ExtClassLoader</span>@66350f69
 sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@3d4eac69           <span class="token number">4</span>            3d4eac69  sun.misc.Launcher<span class="token variable">$ExtClassLoader</span>@66350f69
 sun.misc.Launcher<span class="token variable">$ExtClassLoader</span>@66350f69           <span class="token number">1</span>            66350f69  null
Affect<span class="token punctuation">(</span>row-cnt:4<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">2</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="view-class-loaders-hierarchy" tabindex="-1"><a class="header-anchor" href="#view-class-loaders-hierarchy" aria-hidden="true">#</a> View class-loaders hierarchy</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader <span class="token parameter variable">-t</span>
+-BootstrapClassLoader
+-sun.misc.Launcher<span class="token variable">$ExtClassLoader</span>@66350f69
  +-com.taobao.arthas.agent.ArthasClassloader@68b31f0a
  +-sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@3d4eac69
Affect<span class="token punctuation">(</span>row-cnt:4<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">3</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="show-the-urls-of-the-urlclassloader" tabindex="-1"><a class="header-anchor" href="#show-the-urls-of-the-urlclassloader" aria-hidden="true">#</a> Show the URLs of the URLClassLoader</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader <span class="token parameter variable">-c</span> 3d4eac69
file:/private/tmp/math-game.jar
file:/Users/hengyunabc/.arthas/lib/3.0.5/arthas/arthas-agent.jar

Affect<span class="token punctuation">(</span>row-cnt:9<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">3</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>Note that the hashcode changes, you need to check the current ClassLoader information first, and extract the hashcode corresponding to the ClassLoader.</p><p>For ClassLoader with only unique instance, it can be specified by class name, which is more convenient to use:</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader <span class="token parameter variable">--classLoaderClass</span> sun.misc.Launcher<span class="token variable">$AppClassLoader</span>
file:/private/tmp/math-game.jar
file:/Users/hengyunabc/.arthas/lib/3.0.5/arthas/arthas-agent.jar

Affect<span class="token punctuation">(</span>row-cnt:9<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">3</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="use-the-classloader-to-load-resource" tabindex="-1"><a class="header-anchor" href="#use-the-classloader-to-load-resource" aria-hidden="true">#</a> Use the classloader to load resource</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader <span class="token parameter variable">-c</span> 3d4eac69  <span class="token parameter variable">-r</span> META-INF/MANIFEST.MF
 jar:file:/System/Library/Java/Extensions/MRJToolkit.jar<span class="token operator">!</span>/META-INF/MANIFEST.MF
 jar:file:/private/tmp/math-game.jar<span class="token operator">!</span>/META-INF/MANIFEST.MF
 jar:file:/Users/hengyunabc/.arthas/lib/3.0.5/arthas/arthas-agent.jar<span class="token operator">!</span>/META-INF/MANIFEST.MF
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>Use the classloader to load <code>.class</code> resource</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader <span class="token parameter variable">-c</span> 1b6d3586 <span class="token parameter variable">-r</span> java/lang/String.class
 jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/rt.jar<span class="token operator">!</span>/java/lang/String.class
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="use-the-classloader-to-load-class" tabindex="-1"><a class="header-anchor" href="#use-the-classloader-to-load-class" aria-hidden="true">#</a> Use the classloader to load class</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader <span class="token parameter variable">-c</span> 3d4eac69 <span class="token parameter variable">--load</span> demo.MathGame
load class success.
 class-info        demo.MathGame
 code-source       /private/tmp/math-game.jar
 name              demo.MathGame
 isInterface       <span class="token boolean">false</span>
 isAnnotation      <span class="token boolean">false</span>
 isEnum            <span class="token boolean">false</span>
 isAnonymousClass  <span class="token boolean">false</span>
 isArray           <span class="token boolean">false</span>
 isLocalClass      <span class="token boolean">false</span>
 isMemberClass     <span class="token boolean">false</span>
 isPrimitive       <span class="token boolean">false</span>
 isSynthetic       <span class="token boolean">false</span>
 simple-name       MathGame
 modifier          public
 annotation
 interfaces
 super-class       +-java.lang.Object
 class-loader      +-sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@3d4eac69
                     +-sun.misc.Launcher<span class="token variable">$ExtClassLoader</span>@66350f69
 classLoaderHash   3d4eac69
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="statistics-classloader-actually-used-urls-and-unused-urls" tabindex="-1"><a class="header-anchor" href="#statistics-classloader-actually-used-urls-and-unused-urls" aria-hidden="true">#</a> Statistics ClassLoader actually used URLs and unused URLs</h3><div class="custom-container warning"><p class="custom-container-title">WARNING</p><p>Note that statistics are based on all classes currently loaded by the JVM. Does not mean that <code>Unused URLs</code> can be removed from the application. Because it may be necessary to load classes from <code>Unused URLs</code> in the future, or to load <code>resources</code>.</p></div><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ classloader --url-stat
 com.taobao.arthas.agent.ArthasClassloader@3c41660, hash:3c41660
 Used URLs:
 file:/Users/admin/.arthas/lib/3.5.6/arthas/arthas-core.jar
 Unused URLs:

 sun.misc.Launcher$AppClassLoader@75b84c92, hash:75b84c92
 Used URLs:
 file:/Users/admin/code/java/arthas/math-game/target/math-game.jar
 file:/Users/admin/.arthas/lib/3.5.6/arthas/arthas-agent.jar
 Unused URLs:

 sun.misc.Launcher$ExtClassLoader@7f31245a, hash:7f31245a
 Used URLs:
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/sunec.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/sunjce_provider.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/localedata.jar
 Unused URLs:
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/nashorn.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/cldrdata.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/legacy8ujsse.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/jfxrt.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/dnsns.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/openjsse.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/sunpkcs11.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/jaccess.jar
 file:/tmp/jdk1.8/Contents/Home/jre/lib/ext/zipfs.jar
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,25);function h(b,g){const e=d("ExternalLinkIcon");return l(),i("div",null,[o,a("p",null,[a("a",u,[p,m,t(e)])]),v])}const k=n(c,[["render",h],["__file","classloader.html.vue"]]);export{k as default};
