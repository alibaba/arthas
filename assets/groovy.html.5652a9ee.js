import{_ as e,o as p,c as o,a as s,b as i,w as c,d as a,e as n,r as l}from"./app.1e03d722.js";const u={},r=a(`<h1 id="groovy" tabindex="-1"><a class="header-anchor" href="#groovy" aria-hidden="true">#</a> groovy</h1><div class="custom-container tip"><p class="custom-container-title">TIP</p><p>Arthas support groovy scripting to allow user to use script like BTrace. It is possible to use if/for/switch/while in groovy scripting, but has more limitations compared to BTrace.</p></div><h3 id="limitations" tabindex="-1"><a class="header-anchor" href="#limitations" aria-hidden="true">#</a> Limitations</h3><ol><li>Prohibit from alternating the original logic. Like <code>watch</code> command, The major purpose of scripting is monitoring and observing.</li><li>Only allow to monitor at the stages of before/success/exception/finish on one method.</li></ol><h3 id="parameters" tabindex="-1"><a class="header-anchor" href="#parameters" aria-hidden="true">#</a> Parameters</h3><table><thead><tr><th style="text-align:right;">Parameter</th><th style="text-align:left;">Explanation</th></tr></thead><tbody><tr><td style="text-align:right;"><em>class-pattern</em></td><td style="text-align:left;">class name pattern</td></tr><tr><td style="text-align:right;"><em>method-pattern</em></td><td style="text-align:left;">method name pattern</td></tr><tr><td style="text-align:right;"><em>script-filepath</em></td><td style="text-align:left;">the absolute path of the groovy script</td></tr><tr><td style="text-align:right;">[S]</td><td style="text-align:left;">match all sub classes</td></tr><tr><td style="text-align:right;">[E]</td><td style="text-align:left;">enable regex match, the default is wildcard match</td></tr></tbody></table><p>Note: the third parameter <code>script-filepath</code> must be the absolute path of the groovy script, for example <code>/tmp/test.groovy</code>. It is not recommended to use relative path, e.g. <code>./test.groovy</code>.</p><h3 id="explanation-on-the-important-callbacks" tabindex="-1"><a class="header-anchor" href="#explanation-on-the-important-callbacks" aria-hidden="true">#</a> Explanation on the important callbacks</h3><div class="language-java ext-java line-numbers-mode"><pre class="language-java"><code><span class="token doc-comment comment">/**
 * Listeners for script to enhance the class
 */</span>
<span class="token keyword">interface</span> <span class="token class-name">ScriptListener</span> <span class="token punctuation">{</span>

    <span class="token doc-comment comment">/**
     * When the script is created
     *
     * <span class="token keyword">@param</span> <span class="token parameter">output</span> Output
     */</span>
    <span class="token keyword">void</span> <span class="token function">create</span><span class="token punctuation">(</span><span class="token class-name">Output</span> output<span class="token punctuation">)</span><span class="token punctuation">;</span>

    <span class="token doc-comment comment">/**
     * When the script is destroyed
     *
     * <span class="token keyword">@param</span> <span class="token parameter">output</span> Output
     */</span>
    <span class="token keyword">void</span> <span class="token function">destroy</span><span class="token punctuation">(</span><span class="token class-name">Output</span> output<span class="token punctuation">)</span><span class="token punctuation">;</span>

    <span class="token doc-comment comment">/**
     * Before the method executes
     *
     * <span class="token keyword">@param</span> <span class="token parameter">output</span> Output
     * <span class="token keyword">@param</span> <span class="token parameter">advice</span> Advice
     */</span>
    <span class="token keyword">void</span> <span class="token function">before</span><span class="token punctuation">(</span><span class="token class-name">Output</span> output<span class="token punctuation">,</span> <span class="token class-name">Advice</span> advice<span class="token punctuation">)</span><span class="token punctuation">;</span>

    <span class="token doc-comment comment">/**
     * After the method returns
     *
     * <span class="token keyword">@param</span> <span class="token parameter">output</span> Output
     * <span class="token keyword">@param</span> <span class="token parameter">advice</span> Advice
     */</span>
    <span class="token keyword">void</span> <span class="token function">afterReturning</span><span class="token punctuation">(</span><span class="token class-name">Output</span> output<span class="token punctuation">,</span> <span class="token class-name">Advice</span> advice<span class="token punctuation">)</span><span class="token punctuation">;</span>

    <span class="token doc-comment comment">/**
     * After the method throws exceptions
     *
     * <span class="token keyword">@param</span> <span class="token parameter">output</span> Output
     * <span class="token keyword">@param</span> <span class="token parameter">advice</span> Advice
     */</span>
    <span class="token keyword">void</span> <span class="token function">afterThrowing</span><span class="token punctuation">(</span><span class="token class-name">Output</span> output<span class="token punctuation">,</span> <span class="token class-name">Advice</span> advice<span class="token punctuation">)</span><span class="token punctuation">;</span>

<span class="token punctuation">}</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="advice-parameter" tabindex="-1"><a class="header-anchor" href="#advice-parameter" aria-hidden="true">#</a> <code>Advice</code> parameter</h3>`,10),d=s("code",null,"Advice",-1),k=n(" contains all information necessary for notification. Refer to "),v=n("expression core parameters"),m=n(" for more details."),b=a(`<h3 id="output-parameter" tabindex="-1"><a class="header-anchor" href="#output-parameter" aria-hidden="true">#</a> <code>Output</code> parameter</h3><p>There are three methods in <code>Output</code>, used for outputting the corresponding text.</p><div class="language-java ext-java line-numbers-mode"><pre class="language-java"><code><span class="token doc-comment comment">/**
 * Output
 */</span>
<span class="token keyword">interface</span> <span class="token class-name">Output</span> <span class="token punctuation">{</span>

    <span class="token doc-comment comment">/**
     * Output text without line break
     *
     * <span class="token keyword">@param</span> <span class="token parameter">string</span> Text to output
     * <span class="token keyword">@return</span> this
     */</span>
    <span class="token class-name">Output</span> <span class="token function">print</span><span class="token punctuation">(</span><span class="token class-name">String</span> string<span class="token punctuation">)</span><span class="token punctuation">;</span>

    <span class="token doc-comment comment">/**
     * Output text with line break
     *
     * <span class="token keyword">@param</span> <span class="token parameter">string</span> Text to output
     * <span class="token keyword">@return</span> this
     */</span>
    <span class="token class-name">Output</span> <span class="token function">println</span><span class="token punctuation">(</span><span class="token class-name">String</span> string<span class="token punctuation">)</span><span class="token punctuation">;</span>

    <span class="token doc-comment comment">/**
     * Finish outputting from the script
     *
     * <span class="token keyword">@return</span> this
     */</span>
    <span class="token class-name">Output</span> <span class="token function">finish</span><span class="token punctuation">(</span><span class="token punctuation">)</span><span class="token punctuation">;</span>

<span class="token punctuation">}</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="a-groovy-sample-script-to-output-logs" tabindex="-1"><a class="header-anchor" href="#a-groovy-sample-script-to-output-logs" aria-hidden="true">#</a> A groovy sample script to output logs</h3><div class="language-groovy ext-groovy line-numbers-mode"><pre class="language-groovy"><code><span class="token keyword">import</span> com<span class="token punctuation">.</span>taobao<span class="token punctuation">.</span>arthas<span class="token punctuation">.</span>core<span class="token punctuation">.</span>command<span class="token punctuation">.</span>ScriptSupportCommand
<span class="token keyword">import</span> com<span class="token punctuation">.</span>taobao<span class="token punctuation">.</span>arthas<span class="token punctuation">.</span>core<span class="token punctuation">.</span>util<span class="token punctuation">.</span>Advice

<span class="token keyword">import</span> <span class="token keyword">static</span> java<span class="token punctuation">.</span>lang<span class="token punctuation">.</span>String<span class="token punctuation">.</span>format

<span class="token comment">/**
 * Output method logs
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
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>Run the script like this:</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ groovy com.alibaba.sample.petstore.dal.dao.ProductDao getProductById /Users/zhuyong/middleware/arthas/scripts/Logger.groovy <span class="token parameter variable">-S</span>
script create.
Press Ctrl+C to abort.
Affect<span class="token punctuation">(</span>class-cnt:1 , method-cnt:1<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">102</span> ms.
before:class<span class="token operator">=</span>IbatisProductDao<span class="token punctuation">;</span><span class="token assign-left variable">method</span><span class="token operator">=</span>getProductById<span class="token punctuation">;</span><span class="token assign-left variable">paramslen</span><span class="token operator">=</span><span class="token number">1</span><span class="token punctuation">;</span><span class="token punctuation">[</span>Ljava.lang.Object<span class="token punctuation">;</span>@45df64fc<span class="token punctuation">;</span>
returning:class<span class="token operator">=</span>IbatisProductDao<span class="token punctuation">;</span><span class="token assign-left variable">method</span><span class="token operator">=</span>getProductById<span class="token punctuation">;</span>
before:class<span class="token operator">=</span>IbatisProductDao<span class="token punctuation">;</span><span class="token assign-left variable">method</span><span class="token operator">=</span>getProductById<span class="token punctuation">;</span><span class="token assign-left variable">paramslen</span><span class="token operator">=</span><span class="token number">1</span><span class="token punctuation">;</span><span class="token punctuation">[</span>Ljava.lang.Object<span class="token punctuation">;</span>@5b0e2d00<span class="token punctuation">;</span>
returning:class<span class="token operator">=</span>IbatisProductDao<span class="token punctuation">;</span><span class="token assign-left variable">method</span><span class="token operator">=</span>getProductById<span class="token punctuation">;</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,7);function h(g,f){const t=l("RouterLink");return p(),o("div",null,[r,s("p",null,[d,k,i(t,{to:"/en/doc/advice-class.html"},{default:c(()=>[v]),_:1}),m]),b])}const w=e(u,[["render",h],["__file","groovy.html.vue"]]);export{w as default};
