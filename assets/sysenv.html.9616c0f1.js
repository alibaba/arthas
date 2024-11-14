import{_ as s,o as a,c as d,a as e,b as l,e as n,d as r,r as v}from"./app.1e03d722.js";const c={},t=e("h1",{id:"sysenv",tabindex:"-1"},[e("a",{class:"header-anchor",href:"#sysenv","aria-hidden":"true"},"#"),n(" sysenv")],-1),m={href:"https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-sysenv",target:"_blank",rel:"noopener noreferrer"},u=e("code",null,"sysenv",-1),o=n("\u5728\u7EBF\u6559\u7A0B"),b=r(`<div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>\u67E5\u770B\u5F53\u524D JVM \u7684\u73AF\u5883\u5C5E\u6027(<code>System Environment Variables</code>)</p></div><h2 id="\u4F7F\u7528\u53C2\u8003" tabindex="-1"><a class="header-anchor" href="#\u4F7F\u7528\u53C2\u8003" aria-hidden="true">#</a> \u4F7F\u7528\u53C2\u8003</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code> USAGE:
   sysenv [-h] [env-name]

 SUMMARY:
   Display the system env.

 EXAMPLES:
   sysenv
   sysenv USER

 WIKI:
   https://arthas.aliyun.com/doc/sysenv

 OPTIONS:
 -h, --help                                                 this help
 &lt;env-name&gt;                                                 env name
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="\u67E5\u770B\u6240\u6709\u73AF\u5883\u53D8\u91CF" tabindex="-1"><a class="header-anchor" href="#\u67E5\u770B\u6240\u6709\u73AF\u5883\u53D8\u91CF" aria-hidden="true">#</a> \u67E5\u770B\u6240\u6709\u73AF\u5883\u53D8\u91CF</h3><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ sysenv
 KEY                      VALUE
----------------------------------------------------------------------------------------------------------------------------
 PATH                     /Users/admin/.sdkman/candidates/visualvm/current/bin:/Users/admin/.sdkman/candidates/ja
                          va/current/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/Applications/Wireshark.app/Contents/
                          MacOS
 SDKMAN_VERSION           5.7.3+337
 JAVA_HOME                /Users/admin/.sdkman/candidates/java/current
 JAVA_MAIN_CLASS_65244    demo.MathGame
 TERM                     xterm-256color
 LANG                     zh_CN.UTF-8
 AUTOJUMP_SOURCED         1
 COLORTERM                truecolor
 LOGNAME                  admin
 XPC_SERVICE_NAME         0
 PWD                      /Users/admin/code/ali/arthas/demo
 TERM_PROGRAM_VERSION     3.2.5
 _                        /Users/admin/.sdkman/candidates/java/current/bin/java
 SHELL                    /bin/bash
 TERM_PROGRAM             iTerm.app
 SDKMAN_PLATFORM          Darwin
 USER                     admin
 ITERM_PROFILE            Default
 TMPDIR                   /var/folders/0r/k561bkk917gg972stqclbz9h0000gn/T/
 XPC_FLAGS                0x0
 TERM_SESSION_ID          w0t4p0:60BC264D-9649-42AC-A7E4-AF85B69F93F8
 __CF_USER_TEXT_ENCODING  0x1F5:0x19:0x34
 Apple_PubSub_Socket_Ren  /private/tmp/com.apple.launchd.DwmmjSQsll/Render
 der
 COLORFGBG                7;0
 HOME                     /Users/admin
 SHLVL                    1
 AUTOJUMP_ERROR_PATH      /Users/admin/Library/autojump/errors.log
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="\u67E5\u770B\u5355\u4E2A\u73AF\u5883\u53D8\u91CF" tabindex="-1"><a class="header-anchor" href="#\u67E5\u770B\u5355\u4E2A\u73AF\u5883\u53D8\u91CF" aria-hidden="true">#</a> \u67E5\u770B\u5355\u4E2A\u73AF\u5883\u53D8\u91CF</h3><div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>\u652F\u6301\u901A\u8FC7<code>TAB</code>\u952E\u81EA\u52A8\u8865\u5168</p></div><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ sysenv USER
USER=admin
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div></div></div>`,8);function h(_,p){const i=v("ExternalLinkIcon");return a(),d("div",null,[t,e("p",null,[e("a",m,[u,o,l(i)])]),b])}const R=s(c,[["render",h],["__file","sysenv.html.vue"]]);export{R as default};
