import{_ as n,o as t,c as i,a as e,b as l,e as s,d,r as c}from"./app.bd2aade7.js";const o={},r=e("h1",{id:"sc",tabindex:"-1"},[e("a",{class:"header-anchor",href:"#sc","aria-hidden":"true"},"#"),s(" sc")],-1),m={href:"https://arthas.aliyun.com/doc/arthas-tutorials?language=en&id=command-sc",target:"_blank",rel:"noopener noreferrer"},u=e("code",null,"sc",-1),p=s(" online tutorial"),v=d(`<div class="custom-container tip"><p class="custom-container-title">TIP</p><p>Search classes loaded by JVM.</p></div><p><code>sc</code> stands for search class. This command can search all possible classes loaded by JVM and show their information. The supported options are: <code>[d]</code>\u3001<code>[E]</code>\u3001<code>[f]</code> and <code>[x:]</code>.</p><h3 id="supported-options" tabindex="-1"><a class="header-anchor" href="#supported-options" aria-hidden="true">#</a> Supported Options</h3><table><thead><tr><th style="text-align:right;">Name</th><th style="text-align:left;">Specification</th></tr></thead><tbody><tr><td style="text-align:right;"><em>class-pattern</em></td><td style="text-align:left;">pattern for the class name</td></tr><tr><td style="text-align:right;"><em>method-pattern</em></td><td style="text-align:left;">pattern for the method name</td></tr><tr><td style="text-align:right;"><code>[d]</code></td><td style="text-align:left;">print the details of the current class, including its code source, class specification, its class loader and so on.<br>If a class is loaded by more than one class loader, then the class details will be printed several times</td></tr><tr><td style="text-align:right;"><code>[E]</code></td><td style="text-align:left;">turn on regex match, the default behavior is wildcards match</td></tr><tr><td style="text-align:right;"><code>[f]</code></td><td style="text-align:left;">print the fields info of the current class, MUST be used with <code>-d</code> together</td></tr><tr><td style="text-align:right;"><code>[x:]</code></td><td style="text-align:left;">specify the depth of recursive traverse the static fields, the default value is &#39;0&#39; - equivalent to use <code>toString</code> to output</td></tr><tr><td style="text-align:right;"><code>[c:]</code></td><td style="text-align:left;">The hash code of the special class&#39;s classLoader</td></tr><tr><td style="text-align:right;"><code>[classLoaderClass:]</code></td><td style="text-align:left;">The class name of the ClassLoader that executes the expression.</td></tr><tr><td style="text-align:right;"><code>[n:]</code></td><td style="text-align:left;">Maximum number of matching classes with details (100 by default)</td></tr></tbody></table><div class="custom-container tip"><p class="custom-container-title">TIP</p><p><em>class-patten</em> supports full qualified class name, e.g. com.taobao.test.AAA and com/taobao/test/AAA. It also supports the format of &#39;com/taobao/test/AAA&#39;, so that it is convenient to directly copy class name from the exception stack trace without replacing &#39;/&#39; to &#39;.&#39;.</p></div><div class="custom-container tip"><p class="custom-container-title">TIP</p><p><code>sc</code> turns on matching sub-class match by default, that is, <code>sc</code> will also search the sub classes of the target class too. If exact-match is desired, pls. use <code>options disable-sub-class true</code>.</p></div><h3 id="usage" tabindex="-1"><a class="header-anchor" href="#usage" aria-hidden="true">#</a> Usage</h3><ul><li><p>Wildcards match search</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ sc demo.*
demo.MathGame
Affect<span class="token punctuation">(</span>row-cnt:1<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">55</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div></li><li><p>View class details</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ sc -d demo.MathGame
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

Affect<span class="token punctuation">(</span>row-cnt:1<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">875</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div></li><li><p>View class fields</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ sc -d -f demo.MathGame
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
fields            modifierprivate,static
                  <span class="token builtin class-name">type</span>    java.util.Random
                  name    random
                  value   java.util.Random@522b4
                          08a

                  modifierprivate
                  <span class="token builtin class-name">type</span>    int
                  name    illegalArgumentCount


Affect<span class="token punctuation">(</span>row-cnt:1<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">19</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div></li></ul>`,8);function b(h,f){const a=c("ExternalLinkIcon");return t(),i("div",null,[r,e("p",null,[e("a",m,[u,p,l(a)])]),v])}var k=n(o,[["render",b],["__file","sc.html.vue"]]);export{k as default};
