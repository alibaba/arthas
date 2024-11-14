import{_ as n,o as l,c as i,a as s,b as t,e,d,r as c}from"./app.1e03d722.js";const o={},r=s("h1",{id:"sc",tabindex:"-1"},[s("a",{class:"header-anchor",href:"#sc","aria-hidden":"true"},"#"),e(" sc")],-1),m={href:"https://arthas.aliyun.com/doc/arthas-tutorials?language=cn&id=command-sc",target:"_blank",rel:"noopener noreferrer"},v=s("code",null,"sc",-1),p=e("\u5728\u7EBF\u6559\u7A0B"),u=d(`<div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>\u67E5\u770B JVM \u5DF2\u52A0\u8F7D\u7684\u7C7B\u4FE1\u606F</p></div><p>\u201CSearch-Class\u201D \u7684\u7B80\u5199\uFF0C\u8FD9\u4E2A\u547D\u4EE4\u80FD\u641C\u7D22\u51FA\u6240\u6709\u5DF2\u7ECF\u52A0\u8F7D\u5230 JVM \u4E2D\u7684 Class \u4FE1\u606F\uFF0C\u8FD9\u4E2A\u547D\u4EE4\u652F\u6301\u7684\u53C2\u6570\u6709 <code>[d]</code>\u3001<code>[E]</code>\u3001<code>[f]</code> \u548C <code>[x:]</code>\u3002</p><h2 id="\u53C2\u6570\u8BF4\u660E" tabindex="-1"><a class="header-anchor" href="#\u53C2\u6570\u8BF4\u660E" aria-hidden="true">#</a> \u53C2\u6570\u8BF4\u660E</h2><table><thead><tr><th style="text-align:right;">\u53C2\u6570\u540D\u79F0</th><th style="text-align:left;">\u53C2\u6570\u8BF4\u660E</th></tr></thead><tbody><tr><td style="text-align:right;"><em>class-pattern</em></td><td style="text-align:left;">\u7C7B\u540D\u8868\u8FBE\u5F0F\u5339\u914D</td></tr><tr><td style="text-align:right;"><em>method-pattern</em></td><td style="text-align:left;">\u65B9\u6CD5\u540D\u8868\u8FBE\u5F0F\u5339\u914D</td></tr><tr><td style="text-align:right;">[d]</td><td style="text-align:left;">\u8F93\u51FA\u5F53\u524D\u7C7B\u7684\u8BE6\u7EC6\u4FE1\u606F\uFF0C\u5305\u62EC\u8FD9\u4E2A\u7C7B\u6240\u52A0\u8F7D\u7684\u539F\u59CB\u6587\u4EF6\u6765\u6E90\u3001\u7C7B\u7684\u58F0\u660E\u3001\u52A0\u8F7D\u7684 ClassLoader \u7B49\u8BE6\u7EC6\u4FE1\u606F\u3002<br>\u5982\u679C\u4E00\u4E2A\u7C7B\u88AB\u591A\u4E2A ClassLoader \u6240\u52A0\u8F7D\uFF0C\u5219\u4F1A\u51FA\u73B0\u591A\u6B21</td></tr><tr><td style="text-align:right;">[E]</td><td style="text-align:left;">\u5F00\u542F\u6B63\u5219\u8868\u8FBE\u5F0F\u5339\u914D\uFF0C\u9ED8\u8BA4\u4E3A\u901A\u914D\u7B26\u5339\u914D</td></tr><tr><td style="text-align:right;">[f]</td><td style="text-align:left;">\u8F93\u51FA\u5F53\u524D\u7C7B\u7684\u6210\u5458\u53D8\u91CF\u4FE1\u606F\uFF08\u9700\u8981\u914D\u5408\u53C2\u6570-d \u4E00\u8D77\u4F7F\u7528\uFF09</td></tr><tr><td style="text-align:right;">[x:]</td><td style="text-align:left;">\u6307\u5B9A\u8F93\u51FA\u9759\u6001\u53D8\u91CF\u65F6\u5C5E\u6027\u7684\u904D\u5386\u6DF1\u5EA6\uFF0C\u9ED8\u8BA4\u4E3A 0\uFF0C\u5373\u76F4\u63A5\u4F7F\u7528 <code>toString</code> \u8F93\u51FA</td></tr><tr><td style="text-align:right;"><code>[c:]</code></td><td style="text-align:left;">\u6307\u5B9A class \u7684 ClassLoader \u7684 hashcode</td></tr><tr><td style="text-align:right;"><code>[classLoaderClass:]</code></td><td style="text-align:left;">\u6307\u5B9A\u6267\u884C\u8868\u8FBE\u5F0F\u7684 ClassLoader \u7684 class name</td></tr><tr><td style="text-align:right;"><code>[n:]</code></td><td style="text-align:left;">\u5177\u6709\u8BE6\u7EC6\u4FE1\u606F\u7684\u5339\u914D\u7C7B\u7684\u6700\u5927\u6570\u91CF\uFF08\u9ED8\u8BA4\u4E3A 100\uFF09</td></tr><tr><td style="text-align:right;"><code>[cs &lt;arg&gt;]</code></td><td style="text-align:left;">\u6307\u5B9A class \u7684 ClassLoader#toString() \u8FD4\u56DE\u503C\u3002\u957F\u683C\u5F0F<code>[classLoaderStr &lt;arg&gt;]</code></td></tr></tbody></table><div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>class-pattern \u652F\u6301\u5168\u9650\u5B9A\u540D\uFF0C\u5982 com.taobao.test.AAA\uFF0C\u4E5F\u652F\u6301 com/taobao/test/AAA \u8FD9\u6837\u7684\u683C\u5F0F\uFF0C\u8FD9\u6837\uFF0C\u6211\u4EEC\u4ECE\u5F02\u5E38\u5806\u6808\u91CC\u9762\u628A\u7C7B\u540D\u62F7\u8D1D\u8FC7\u6765\u7684\u65F6\u5019\uFF0C\u4E0D\u9700\u8981\u5728\u624B\u52A8\u628A<code>/</code>\u66FF\u6362\u4E3A<code>.</code>\u5566\u3002</p></div><div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>sc \u9ED8\u8BA4\u5F00\u542F\u4E86\u5B50\u7C7B\u5339\u914D\u529F\u80FD\uFF0C\u4E5F\u5C31\u662F\u8BF4\u6240\u6709\u5F53\u524D\u7C7B\u7684\u5B50\u7C7B\u4E5F\u4F1A\u88AB\u641C\u7D22\u51FA\u6765\uFF0C\u60F3\u8981\u7CBE\u786E\u7684\u5339\u914D\uFF0C\u8BF7\u6253\u5F00<code>options disable-sub-class true</code>\u5F00\u5173</p></div><h2 id="\u4F7F\u7528\u53C2\u8003" tabindex="-1"><a class="header-anchor" href="#\u4F7F\u7528\u53C2\u8003" aria-hidden="true">#</a> \u4F7F\u7528\u53C2\u8003</h2><ul><li><p>\u6A21\u7CCA\u641C\u7D22</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ sc demo.*
demo.MathGame
Affect<span class="token punctuation">(</span>row-cnt:1<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">55</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div></li><li><p>\u6253\u5370\u7C7B\u7684\u8BE6\u7EC6\u4FE1\u606F</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ sc <span class="token parameter variable">-d</span> demo.MathGame
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
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div></li><li><p>\u6253\u5370\u51FA\u7C7B\u7684 Field \u4FE1\u606F</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ sc <span class="token parameter variable">-d</span> <span class="token parameter variable">-f</span> demo.MathGame
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
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div></li><li><p>\u901A\u8FC7 ClassLoader#toString \u67E5\u627E\u7C7B\uFF08\u524D\u63D0\uFF1A\u6709\u4E00\u4E2A toString()\u8FD4\u56DE\u503C\u662F<code>apo</code>\u7684\u7C7B\u52A0\u8F7D\u5668\uFF0C\u52A0\u8F7D\u7684\u7C7B\u4E2D\u5305\u542B<code>demo.MathGame</code>, <code>demo.MyBar</code>,<code> demo.MyFoo</code>3 \u4E2A\u7C7B\uFF09</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ sc <span class="token parameter variable">-cs</span> apo *demo*
demo.MathGame
demo.MyBar
demo.MyFoo
Affect<span class="token punctuation">(</span>row-cnt:3<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">56</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div></li></ul>`,8);function b(h,g){const a=c("ExternalLinkIcon");return l(),i("div",null,[r,s("p",null,[s("a",m,[v,p,t(a)])]),u])}const k=n(o,[["render",b],["__file","sc.html.vue"]]);export{k as default};
