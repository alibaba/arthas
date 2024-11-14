import{_ as e,o as p,c,a as s,b as i,w as o,d as a,e as n,r as l}from"./app.1e03d722.js";const u={},d=a(`<h1 id="groovy" tabindex="-1"><a class="header-anchor" href="#groovy" aria-hidden="true">#</a> groovy</h1><div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>Arthas \u652F\u6301 groovy \u811A\u672C\u589E\u5F3A\uFF0C\u5141\u8BB8\u50CF BTrace \u4E00\u6837\u7F16\u5199\u811A\u672C\u6765\u89E3\u51B3\u95EE\u9898\uFF0C\u53EF\u4EE5\u5728 groovy \u811A\u672C\u4E2D\u8FDB\u884C if/for/switch/while \u7B49\u63A7\u5236\u8BED\u53E5\uFF0C\u4E0D\u53D7\u9650\u5236\uFF0C\u4F46\u76F8\u6BD4 BTrace \u800C\u8A00\u62E5\u6709\u66F4\u591A\u7684\u9650\u5236\u8303\u56F4\u3002</p></div><h3 id="\u9650\u5236\u5185\u5BB9" tabindex="-1"><a class="header-anchor" href="#\u9650\u5236\u5185\u5BB9" aria-hidden="true">#</a> \u9650\u5236\u5185\u5BB9</h3><ol><li>\u7981\u6B62\u6539\u53D8\u539F\u6709\u903B\u8F91\uFF0C\u4E0E watch \u7B49\u547D\u4EE4\u4E00\u6837\uFF0C\u91CD\u70B9\u4FDD\u8BC1\u7684\u662F\u76D1\u542C\u548C\u89C2\u5BDF\u3002</li><li>\u53EA\u5141\u8BB8\u5728\u65B9\u6CD5\u7684 before/success/exception/finish \u56DB\u4E2A\u73AF\u8282\u8FDB\u884C\u76D1\u542C\u3002</li></ol><h3 id="\u53C2\u6570\u8BF4\u660E" tabindex="-1"><a class="header-anchor" href="#\u53C2\u6570\u8BF4\u660E" aria-hidden="true">#</a> \u53C2\u6570\u8BF4\u660E</h3><table><thead><tr><th style="text-align:right;">\u53C2\u6570\u540D\u79F0</th><th style="text-align:left;">\u53C2\u6570\u8BF4\u660E</th></tr></thead><tbody><tr><td style="text-align:right;"><em>class-pattern</em></td><td style="text-align:left;">\u7C7B\u540D\u8868\u8FBE\u5F0F\u5339\u914D</td></tr><tr><td style="text-align:right;"><em>method-pattern</em></td><td style="text-align:left;">\u65B9\u6CD5\u540D\u8868\u8FBE\u5F0F\u5339\u914D</td></tr><tr><td style="text-align:right;"><em>script-filepath</em></td><td style="text-align:left;">groovy \u811A\u672C\u7684\u7EDD\u5BF9\u8DEF\u5F84</td></tr><tr><td style="text-align:right;">[S]</td><td style="text-align:left;">\u5339\u914D\u6240\u6709\u7684\u5B50\u7C7B</td></tr><tr><td style="text-align:right;">[E]</td><td style="text-align:left;">\u5F00\u542F\u6B63\u5219\u8868\u8FBE\u5F0F\u5339\u914D\uFF0C\u9ED8\u8BA4\u4E3A\u901A\u914D\u7B26\u5339\u914D</td></tr></tbody></table><p>\u9700\u8981\u8BF4\u660E\u7684\u662F\uFF0C\u7B2C\u4E09\u4E2A\u8F93\u5165\u53C2\u6570\u662F\u811A\u672C\u7684\u7EDD\u5BF9\u8DEF\u5F84\uFF0C\u6BD4\u5982 <code>/tmp/test.groovy</code>\uFF0C\u4E0D\u5EFA\u8BAE\u8F93\u5165\u76F8\u5BF9\u8DEF\u5F84\uFF0C\u6BD4\u5982 <code>./test.groovy</code></p><h3 id="\u4E94\u4E2A\u5173\u952E\u51FD\u6570\u58F0\u660E" tabindex="-1"><a class="header-anchor" href="#\u4E94\u4E2A\u5173\u952E\u51FD\u6570\u58F0\u660E" aria-hidden="true">#</a> \u4E94\u4E2A\u5173\u952E\u51FD\u6570\u58F0\u660E</h3><div class="language-java ext-java line-numbers-mode"><pre class="language-java"><code><span class="token doc-comment comment">/**
 * \u589E\u5F3A\u811A\u672C\u76D1\u542C\u5668
 */</span>
<span class="token keyword">interface</span> <span class="token class-name">ScriptListener</span> <span class="token punctuation">{</span>

    <span class="token doc-comment comment">/**
     * \u811A\u672C\u521B\u5EFA
     *
     * <span class="token keyword">@param</span> <span class="token parameter">output</span> \u8F93\u51FA\u5668
     */</span>
    <span class="token keyword">void</span> <span class="token function">create</span><span class="token punctuation">(</span><span class="token class-name">Output</span> output<span class="token punctuation">)</span><span class="token punctuation">;</span>

    <span class="token doc-comment comment">/**
     * \u811A\u672C\u9500\u6BC1
     *
     * <span class="token keyword">@param</span> <span class="token parameter">output</span> \u8F93\u51FA\u5668
     */</span>
    <span class="token keyword">void</span> <span class="token function">destroy</span><span class="token punctuation">(</span><span class="token class-name">Output</span> output<span class="token punctuation">)</span><span class="token punctuation">;</span>

    <span class="token doc-comment comment">/**
     * \u65B9\u6CD5\u6267\u884C\u524D
     *
     * <span class="token keyword">@param</span> <span class="token parameter">output</span> \u8F93\u51FA\u5668
     * <span class="token keyword">@param</span> <span class="token parameter">advice</span> \u901A\u77E5\u70B9
     */</span>
    <span class="token keyword">void</span> <span class="token function">before</span><span class="token punctuation">(</span><span class="token class-name">Output</span> output<span class="token punctuation">,</span> <span class="token class-name">Advice</span> advice<span class="token punctuation">)</span><span class="token punctuation">;</span>

    <span class="token doc-comment comment">/**
     * \u65B9\u6CD5\u6B63\u5E38\u8FD4\u56DE
     *
     * <span class="token keyword">@param</span> <span class="token parameter">output</span> \u8F93\u51FA\u5668
     * <span class="token keyword">@param</span> <span class="token parameter">advice</span> \u901A\u77E5\u70B9
     */</span>
    <span class="token keyword">void</span> <span class="token function">afterReturning</span><span class="token punctuation">(</span><span class="token class-name">Output</span> output<span class="token punctuation">,</span> <span class="token class-name">Advice</span> advice<span class="token punctuation">)</span><span class="token punctuation">;</span>

    <span class="token doc-comment comment">/**
     * \u65B9\u6CD5\u5F02\u5E38\u8FD4\u56DE
     *
     * <span class="token keyword">@param</span> <span class="token parameter">output</span> \u8F93\u51FA\u5668
     * <span class="token keyword">@param</span> <span class="token parameter">advice</span> \u901A\u77E5\u70B9
     */</span>
    <span class="token keyword">void</span> <span class="token function">afterThrowing</span><span class="token punctuation">(</span><span class="token class-name">Output</span> output<span class="token punctuation">,</span> <span class="token class-name">Advice</span> advice<span class="token punctuation">)</span><span class="token punctuation">;</span>

<span class="token punctuation">}</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="\u53C2\u6570-advice-\u8BF4\u660E" tabindex="-1"><a class="header-anchor" href="#\u53C2\u6570-advice-\u8BF4\u660E" aria-hidden="true">#</a> \u53C2\u6570 <code>Advice</code> \u8BF4\u660E</h3>`,10),r=s("code",null,"Advice",-1),v=n(" \u53C2\u6570\u6700\u4E3B\u8981\u662F\u5C01\u88C5\u4E86\u901A\u77E5\u8282\u70B9\u7684\u6240\u6709\u4FE1\u606F\u3002\u53C2\u8003"),k=n("\u8868\u8FBE\u5F0F\u6838\u5FC3\u53D8\u91CF"),m=n("\u4E2D\u5173\u4E8E\u8BE5\u8282\u70B9\u7684\u63CF\u8FF0\u3002"),b=a(`<h3 id="\u53C2\u6570-output-\u8BF4\u660E" tabindex="-1"><a class="header-anchor" href="#\u53C2\u6570-output-\u8BF4\u660E" aria-hidden="true">#</a> \u53C2\u6570 <code>Output</code> \u8BF4\u660E</h3><p><code>Output</code> \u53C2\u6570\u53EA\u62E5\u6709\u4E09\u4E2A\u65B9\u6CD5\uFF0C\u4E3B\u8981\u7684\u5DE5\u4F5C\u8FD8\u662F\u8F93\u51FA\u5BF9\u5E94\u7684\u6587\u672C\u4FE1\u606F</p><div class="language-java ext-java line-numbers-mode"><pre class="language-java"><code><span class="token doc-comment comment">/**
 * \u8F93\u51FA\u5668
 */</span>
<span class="token keyword">interface</span> <span class="token class-name">Output</span> <span class="token punctuation">{</span>

    <span class="token doc-comment comment">/**
     * \u8F93\u51FA\u5B57\u7B26\u4E32(\u4E0D\u6362\u884C)
     *
     * <span class="token keyword">@param</span> <span class="token parameter">string</span> \u5F85\u8F93\u51FA\u5B57\u7B26\u4E32
     * <span class="token keyword">@return</span> this
     */</span>
    <span class="token class-name">Output</span> <span class="token function">print</span><span class="token punctuation">(</span><span class="token class-name">String</span> string<span class="token punctuation">)</span><span class="token punctuation">;</span>

    <span class="token doc-comment comment">/**
     * \u8F93\u51FA\u5B57\u7B26\u4E32(\u6362\u884C)
     *
     * <span class="token keyword">@param</span> <span class="token parameter">string</span> \u5F85\u8F93\u51FA\u5B57\u7B26\u4E32
     * <span class="token keyword">@return</span> this
     */</span>
    <span class="token class-name">Output</span> <span class="token function">println</span><span class="token punctuation">(</span><span class="token class-name">String</span> string<span class="token punctuation">)</span><span class="token punctuation">;</span>

    <span class="token doc-comment comment">/**
     * \u7ED3\u675F\u5F53\u524D\u811A\u672C
     *
     * <span class="token keyword">@return</span> this
     */</span>
    <span class="token class-name">Output</span> <span class="token function">finish</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">;</span>

<span class="token punctuation">}</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="\u4E00\u4E2A\u8F93\u51FA\u65E5\u5FD7\u7684-groovy-\u811A\u672C\u793A\u4F8B" tabindex="-1"><a class="header-anchor" href="#\u4E00\u4E2A\u8F93\u51FA\u65E5\u5FD7\u7684-groovy-\u811A\u672C\u793A\u4F8B" aria-hidden="true">#</a> \u4E00\u4E2A\u8F93\u51FA\u65E5\u5FD7\u7684 groovy \u811A\u672C\u793A\u4F8B</h3><div class="language-groovy ext-groovy line-numbers-mode"><pre class="language-groovy"><code><span class="token keyword">import</span> com<span class="token punctuation">.</span>taobao<span class="token punctuation">.</span>arthas<span class="token punctuation">.</span>core<span class="token punctuation">.</span>command<span class="token punctuation">.</span>ScriptSupportCommand
<span class="token keyword">import</span> com<span class="token punctuation">.</span>taobao<span class="token punctuation">.</span>arthas<span class="token punctuation">.</span>core<span class="token punctuation">.</span>util<span class="token punctuation">.</span>Advice

<span class="token keyword">import</span> <span class="token keyword">static</span> java<span class="token punctuation">.</span>lang<span class="token punctuation">.</span>String<span class="token punctuation">.</span>format

<span class="token comment">/**
 * \u8F93\u51FA\u65B9\u6CD5\u65E5\u5FD7
 */</span>
<span class="token keyword">public</span> <span class="token keyword">class</span> <span class="token class-name">Logger</span> <span class="token keyword">implements</span> <span class="token class-name">ScriptSupportCommand<span class="token punctuation">.</span>ScriptListener</span> <span class="token punctuation">{</span>

    <span class="token annotation punctuation">@Override</span>
    <span class="token keyword">void</span> <span class="token function">create</span><span class="token punctuation">(</span>ScriptSupportCommand<span class="token punctuation">.</span>Output output<span class="token punctuation">)</span> <span class="token punctuation">{</span>
        output<span class="token punctuation">.</span><span class="token function">println</span><span class="token punctuation">(</span><span class="token interpolation-string"><span class="token string">&quot;script create.&quot;</span></span><span class="token punctuation">)</span><span class="token punctuation">;</span>
    <span class="token punctuation">}</span>

    <span class="token annotation punctuation">@Override</span>
    <span class="token keyword">void</span> <span class="token function">destroy</span><span class="token punctuation">(</span>ScriptSupportCommand<span class="token punctuation">.</span>Output output<span class="token punctuation">)</span> <span class="token punctuation">{</span>
        output<span class="token punctuation">.</span><span class="token function">println</span><span class="token punctuation">(</span><span class="token interpolation-string"><span class="token string">&quot;script destroy.&quot;</span></span><span class="token punctuation">)</span><span class="token punctuation">;</span>
    <span class="token punctuation">}</span>

    <span class="token annotation punctuation">@Override</span>
    <span class="token keyword">void</span> <span class="token function">before</span><span class="token punctuation">(</span>ScriptSupportCommand<span class="token punctuation">.</span>Output output<span class="token punctuation">,</span> Advice advice<span class="token punctuation">)</span> <span class="token punctuation">{</span>
        output<span class="token punctuation">.</span><span class="token function">println</span><span class="token punctuation">(</span><span class="token function">format</span><span class="token punctuation">(</span><span class="token interpolation-string"><span class="token string">&quot;before:class=%s;method=%s;paramslen=%d;%s;&quot;</span></span><span class="token punctuation">,</span>
                advice<span class="token punctuation">.</span><span class="token function">getClazz</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">.</span><span class="token function">getSimpleName</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">,</span>
                advice<span class="token punctuation">.</span><span class="token function">getMethod</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">.</span><span class="token function">getName</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">,</span>
                advice<span class="token punctuation">.</span><span class="token function">getParams</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">.</span>length<span class="token punctuation">,</span> advice<span class="token punctuation">.</span><span class="token function">getParams</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">)</span><span class="token punctuation">)</span>
    <span class="token punctuation">}</span>

    <span class="token annotation punctuation">@Override</span>
    <span class="token keyword">void</span> <span class="token function">afterReturning</span><span class="token punctuation">(</span>ScriptSupportCommand<span class="token punctuation">.</span>Output output<span class="token punctuation">,</span> Advice advice<span class="token punctuation">)</span> <span class="token punctuation">{</span>
        output<span class="token punctuation">.</span><span class="token function">println</span><span class="token punctuation">(</span><span class="token function">format</span><span class="token punctuation">(</span><span class="token interpolation-string"><span class="token string">&quot;returning:class=%s;method=%s;&quot;</span></span><span class="token punctuation">,</span>
                advice<span class="token punctuation">.</span><span class="token function">getClazz</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">.</span><span class="token function">getSimpleName</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">,</span>
                advice<span class="token punctuation">.</span><span class="token function">getMethod</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">.</span><span class="token function">getName</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">)</span><span class="token punctuation">)</span>
    <span class="token punctuation">}</span>

    <span class="token annotation punctuation">@Override</span>
    <span class="token keyword">void</span> <span class="token function">afterThrowing</span><span class="token punctuation">(</span>ScriptSupportCommand<span class="token punctuation">.</span>Output output<span class="token punctuation">,</span> Advice advice<span class="token punctuation">)</span> <span class="token punctuation">{</span>
        output<span class="token punctuation">.</span><span class="token function">println</span><span class="token punctuation">(</span><span class="token function">format</span><span class="token punctuation">(</span><span class="token interpolation-string"><span class="token string">&quot;throwing:class=%s;method=%s;&quot;</span></span><span class="token punctuation">,</span>
                advice<span class="token punctuation">.</span><span class="token function">getClazz</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">.</span><span class="token function">getSimpleName</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">,</span>
                advice<span class="token punctuation">.</span><span class="token function">getMethod</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">.</span><span class="token function">getName</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">)</span><span class="token punctuation">)</span>
    <span class="token punctuation">}</span>
<span class="token punctuation">}</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>\u4F7F\u7528\u793A\u4F8B\uFF1A</p><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ groovy com.alibaba.sample.petstore.dal.dao.ProductDao getProductById /Users/zhuyong/middleware/arthas/scripts/Logger.groovy -S
script create.
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 102 ms.
before:class=IbatisProductDao;method=getProductById;paramslen=1;[Ljava.lang.Object;@45df64fc;
returning:class=IbatisProductDao;method=getProductById;
before:class=IbatisProductDao;method=getProductById;paramslen=1;[Ljava.lang.Object;@5b0e2d00;
returning:class=IbatisProductDao;method=getProductById;
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,7);function g(h,f){const t=l("RouterLink");return p(),c("div",null,[d,s("p",null,[r,v,i(t,{to:"/doc/advice-class.html"},{default:o(()=>[k]),_:1}),m]),b])}const w=e(u,[["render",g],["__file","groovy.html.vue"]]);export{w as default};
