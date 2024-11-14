import{_ as n,o as l,c as i,a,b as r,e as s,d,r as t}from"./app.1e03d722.js";const c={},o=a("h1",{id:"classloader",tabindex:"-1"},[a("a",{class:"header-anchor",href:"#classloader","aria-hidden":"true"},"#"),s(" classloader")],-1),u={href:"https://arthas.aliyun.com/doc/arthas-tutorials?language=cn&id=command-classloader",target:"_blank",rel:"noopener noreferrer"},p=a("code",null,"classloader",-1),v=s("\u5728\u7EBF\u6559\u7A0B"),m=d(`<div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>\u67E5\u770B classloader \u7684\u7EE7\u627F\u6811\uFF0Curls\uFF0C\u7C7B\u52A0\u8F7D\u4FE1\u606F</p></div><p><code>classloader</code> \u547D\u4EE4\u5C06 JVM \u4E2D\u6240\u6709\u7684 classloader \u7684\u4FE1\u606F\u7EDF\u8BA1\u51FA\u6765\uFF0C\u5E76\u53EF\u4EE5\u5C55\u793A\u7EE7\u627F\u6811\uFF0Curls \u7B49\u3002</p><p>\u53EF\u4EE5\u8BA9\u6307\u5B9A\u7684 classloader \u53BB getResources\uFF0C\u6253\u5370\u51FA\u6240\u6709\u67E5\u627E\u5230\u7684 resources \u7684 url\u3002\u5BF9\u4E8E<code>ResourceNotFoundException</code>\u6BD4\u8F83\u6709\u7528\u3002</p><h2 id="\u53C2\u6570\u8BF4\u660E" tabindex="-1"><a class="header-anchor" href="#\u53C2\u6570\u8BF4\u660E" aria-hidden="true">#</a> \u53C2\u6570\u8BF4\u660E</h2><table><thead><tr><th style="text-align:right;">\u53C2\u6570\u540D\u79F0</th><th style="text-align:left;">\u53C2\u6570\u8BF4\u660E</th></tr></thead><tbody><tr><td style="text-align:right;">[l]</td><td style="text-align:left;">\u6309\u7C7B\u52A0\u8F7D\u5B9E\u4F8B\u8FDB\u884C\u7EDF\u8BA1</td></tr><tr><td style="text-align:right;">[t]</td><td style="text-align:left;">\u6253\u5370\u6240\u6709 ClassLoader \u7684\u7EE7\u627F\u6811</td></tr><tr><td style="text-align:right;">[a]</td><td style="text-align:left;">\u5217\u51FA\u6240\u6709 ClassLoader \u52A0\u8F7D\u7684\u7C7B\uFF0C\u8BF7\u8C28\u614E\u4F7F\u7528</td></tr><tr><td style="text-align:right;"><code>[c:]</code></td><td style="text-align:left;">ClassLoader \u7684 hashcode</td></tr><tr><td style="text-align:right;"><code>[classLoaderClass:]</code></td><td style="text-align:left;">\u6307\u5B9A\u6267\u884C\u8868\u8FBE\u5F0F\u7684 ClassLoader \u7684 class name</td></tr><tr><td style="text-align:right;"><code>[c: r:]</code></td><td style="text-align:left;">\u7528 ClassLoader \u53BB\u67E5\u627E resource</td></tr><tr><td style="text-align:right;"><code>[c: load:]</code></td><td style="text-align:left;">\u7528 ClassLoader \u53BB\u52A0\u8F7D\u6307\u5B9A\u7684\u7C7B</td></tr></tbody></table><h2 id="\u4F7F\u7528\u53C2\u8003" tabindex="-1"><a class="header-anchor" href="#\u4F7F\u7528\u53C2\u8003" aria-hidden="true">#</a> \u4F7F\u7528\u53C2\u8003</h2><h3 id="\u6309\u7C7B\u52A0\u8F7D\u7C7B\u578B\u67E5\u770B\u7EDF\u8BA1\u4FE1\u606F" tabindex="-1"><a class="header-anchor" href="#\u6309\u7C7B\u52A0\u8F7D\u7C7B\u578B\u67E5\u770B\u7EDF\u8BA1\u4FE1\u606F" aria-hidden="true">#</a> \u6309\u7C7B\u52A0\u8F7D\u7C7B\u578B\u67E5\u770B\u7EDF\u8BA1\u4FE1\u606F</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader
 name                                       numberOfInstances  loadedCountTotal
 com.taobao.arthas.agent.ArthasClassloader  <span class="token number">1</span>                  <span class="token number">2115</span>
 BootstrapClassLoader                       <span class="token number">1</span>                  <span class="token number">1861</span>
 sun.reflect.DelegatingClassLoader          <span class="token number">5</span>                  <span class="token number">5</span>
 sun.misc.Launcher<span class="token variable">$AppClassLoader</span>           <span class="token number">1</span>                  <span class="token number">4</span>
 sun.misc.Launcher<span class="token variable">$ExtClassLoader</span>           <span class="token number">1</span>                  <span class="token number">1</span>
Affect<span class="token punctuation">(</span>row-cnt:5<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">3</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="\u6309\u7C7B\u52A0\u8F7D\u5B9E\u4F8B\u67E5\u770B\u7EDF\u8BA1\u4FE1\u606F" tabindex="-1"><a class="header-anchor" href="#\u6309\u7C7B\u52A0\u8F7D\u5B9E\u4F8B\u67E5\u770B\u7EDF\u8BA1\u4FE1\u606F" aria-hidden="true">#</a> \u6309\u7C7B\u52A0\u8F7D\u5B9E\u4F8B\u67E5\u770B\u7EDF\u8BA1\u4FE1\u606F</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader <span class="token parameter variable">-l</span>
 name                                                loadedCount  <span class="token builtin class-name">hash</span>      parent
 BootstrapClassLoader                                <span class="token number">1861</span>         null      null
 com.taobao.arthas.agent.ArthasClassloader@68b31f0a  <span class="token number">2115</span>         68b31f0a  sun.misc.Launcher<span class="token variable">$ExtClassLoader</span>@66350f69
 sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@3d4eac69           <span class="token number">4</span>            3d4eac69  sun.misc.Launcher<span class="token variable">$ExtClassLoader</span>@66350f69
 sun.misc.Launcher<span class="token variable">$ExtClassLoader</span>@66350f69           <span class="token number">1</span>            66350f69  null
Affect<span class="token punctuation">(</span>row-cnt:4<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">2</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="\u67E5\u770B-classloader-\u7684\u7EE7\u627F\u6811" tabindex="-1"><a class="header-anchor" href="#\u67E5\u770B-classloader-\u7684\u7EE7\u627F\u6811" aria-hidden="true">#</a> \u67E5\u770B ClassLoader \u7684\u7EE7\u627F\u6811</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader <span class="token parameter variable">-t</span>
+-BootstrapClassLoader
+-sun.misc.Launcher<span class="token variable">$ExtClassLoader</span>@66350f69
  +-com.taobao.arthas.agent.ArthasClassloader@68b31f0a
  +-sun.misc.Launcher<span class="token variable">$AppClassLoader</span>@3d4eac69
Affect<span class="token punctuation">(</span>row-cnt:4<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">3</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="\u67E5\u770B-urlclassloader-\u5B9E\u9645\u7684-urls" tabindex="-1"><a class="header-anchor" href="#\u67E5\u770B-urlclassloader-\u5B9E\u9645\u7684-urls" aria-hidden="true">#</a> \u67E5\u770B URLClassLoader \u5B9E\u9645\u7684 urls</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader <span class="token parameter variable">-c</span> 3d4eac69
file:/private/tmp/math-game.jar
file:/Users/hengyunabc/.arthas/lib/3.0.5/arthas/arthas-agent.jar

Affect<span class="token punctuation">(</span>row-cnt:9<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">3</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p><em>\u6CE8\u610F</em> hashcode \u662F\u53D8\u5316\u7684\uFF0C\u9700\u8981\u5148\u67E5\u770B\u5F53\u524D\u7684 ClassLoader \u4FE1\u606F\uFF0C\u63D0\u53D6\u5BF9\u5E94 ClassLoader \u7684 hashcode\u3002</p><p>\u5BF9\u4E8E\u53EA\u6709\u552F\u4E00\u5B9E\u4F8B\u7684 ClassLoader \u53EF\u4EE5\u901A\u8FC7 class name \u6307\u5B9A\uFF0C\u4F7F\u7528\u8D77\u6765\u66F4\u52A0\u65B9\u4FBF\uFF1A</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader <span class="token parameter variable">--classLoaderClass</span> sun.misc.Launcher<span class="token variable">$AppClassLoader</span>
file:/private/tmp/math-game.jar
file:/Users/hengyunabc/.arthas/lib/3.0.5/arthas/arthas-agent.jar

Affect<span class="token punctuation">(</span>row-cnt:9<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">3</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="\u4F7F\u7528-classloader-\u53BB\u67E5\u627E-resource" tabindex="-1"><a class="header-anchor" href="#\u4F7F\u7528-classloader-\u53BB\u67E5\u627E-resource" aria-hidden="true">#</a> \u4F7F\u7528 ClassLoader \u53BB\u67E5\u627E resource</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader <span class="token parameter variable">-c</span> 3d4eac69  <span class="token parameter variable">-r</span> META-INF/MANIFEST.MF
 jar:file:/System/Library/Java/Extensions/MRJToolkit.jar<span class="token operator">!</span>/META-INF/MANIFEST.MF
 jar:file:/private/tmp/math-game.jar<span class="token operator">!</span>/META-INF/MANIFEST.MF
 jar:file:/Users/hengyunabc/.arthas/lib/3.0.5/arthas/arthas-agent.jar<span class="token operator">!</span>/META-INF/MANIFEST.MF
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>\u4E5F\u53EF\u4EE5\u5C1D\u8BD5\u67E5\u627E\u7C7B\u7684 class \u6587\u4EF6\uFF1A</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader <span class="token parameter variable">-c</span> 1b6d3586 <span class="token parameter variable">-r</span> java/lang/String.class
 jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/rt.jar<span class="token operator">!</span>/java/lang/String.class
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="\u4F7F\u7528-classloader-\u53BB\u52A0\u8F7D\u7C7B" tabindex="-1"><a class="header-anchor" href="#\u4F7F\u7528-classloader-\u53BB\u52A0\u8F7D\u7C7B" aria-hidden="true">#</a> \u4F7F\u7528 ClassLoader \u53BB\u52A0\u8F7D\u7C7B</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ classloader <span class="token parameter variable">-c</span> 3d4eac69 <span class="token parameter variable">--load</span> demo.MathGame
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
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="\u7EDF\u8BA1-classloader-\u5B9E\u9645\u4F7F\u7528-url-\u548C\u672A\u4F7F\u7528\u7684-url" tabindex="-1"><a class="header-anchor" href="#\u7EDF\u8BA1-classloader-\u5B9E\u9645\u4F7F\u7528-url-\u548C\u672A\u4F7F\u7528\u7684-url" aria-hidden="true">#</a> \u7EDF\u8BA1 ClassLoader \u5B9E\u9645\u4F7F\u7528 URL \u548C\u672A\u4F7F\u7528\u7684 URL</h3><div class="custom-container warning"><p class="custom-container-title">\u6CE8\u610F</p><p>\u6CE8\u610F\uFF0C\u57FA\u4E8E JVM \u76EE\u524D\u5DF2\u52A0\u8F7D\u7684\u6240\u6709\u7C7B\u7EDF\u8BA1\uFF0C\u4E0D\u4EE3\u8868<code>Unused URLs</code>\u53EF\u4EE5\u4ECE\u5E94\u7528\u4E2D\u5220\u6389\u3002\u56E0\u4E3A\u53EF\u80FD\u5C06\u6765\u9700\u8981\u4ECE<code>Unused URLs</code>\u91CC\u52A0\u8F7D\u7C7B\uFF0C\u6216\u8005\u9700\u8981\u52A0\u8F7D<code>resources</code>\u3002</p></div><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ classloader --url-stat
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
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,26);function b(h,k){const e=t("ExternalLinkIcon");return l(),i("div",null,[o,a("p",null,[a("a",u,[p,v,r(e)])]),m])}const f=n(c,[["render",b],["__file","classloader.html.vue"]]);export{f as default};
