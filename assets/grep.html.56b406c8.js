import{_ as s,o as t,c as a,a as e,b as l,e as n,d as r,r as d}from"./app.1e03d722.js";const c={},v=e("h1",{id:"grep",tabindex:"-1"},[e("a",{class:"header-anchor",href:"#grep","aria-hidden":"true"},"#"),n(" grep")],-1),o={href:"https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-grep",target:"_blank",rel:"noopener noreferrer"},u=e("code",null,"grep",-1),m=n("\u5728\u7EBF\u6559\u7A0B"),p=r(`<div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>\u7C7B\u4F3C\u4F20\u7EDF\u7684<code>grep</code>\u547D\u4EE4\u3002</p></div><h2 id="\u4F7F\u7528\u53C2\u8003" tabindex="-1"><a class="header-anchor" href="#\u4F7F\u7528\u53C2\u8003" aria-hidden="true">#</a> \u4F7F\u7528\u53C2\u8003</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code> USAGE:
   grep [-A &lt;value&gt;] [-B &lt;value&gt;] [-C &lt;value&gt;] [-h] [-i] [-v] [-n] [-m &lt;value&gt;] [-e] [--trim-end] pattern

 SUMMARY:
   grep command for pipes.

 EXAMPLES:
  sysprop | grep java
  sysprop | grep java -n
  sysenv | grep -v JAVA
  sysenv | grep -e &quot;(?i)(JAVA|sun)&quot; -m 3  -C 2
  sysenv | grep JAVA -A2 -B3
  thread | grep -m 10 -e  &quot;TIMED_WAITING|WAITING&quot;

 WIKI:
   https://arthas.aliyun.com/doc/grep

 OPTIONS:
 -A, --after-context &lt;value&gt;                                                    Print NUM lines of trailing context)
 -B, --before-context &lt;value&gt;                                                   Print NUM lines of leading context)
 -C, --context &lt;value&gt;                                                          Print NUM lines of output context)
 -h, --help                                                                     this help
 -i, --ignore-case                                                              Perform case insensitive matching.  By default, grep is case sensitive.
 -v, --invert-match                                                             Select non-matching lines
 -n, --line-number                                                              Print line number with output lines
 -m, --max-count &lt;value&gt;                                                        stop after NUM selected lines)
 -e, --regex                                                                    Enable regular expression to match
     --trim-end                                                                 Remove whitespaces at the end of the line
 &lt;pattern&gt;                                                                      Pattern
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,3);function g(h,b){const i=d("ExternalLinkIcon");return t(),a("div",null,[v,e("p",null,[e("a",o,[u,m,l(i)])]),p])}const f=s(c,[["render",g],["__file","grep.html.vue"]]);export{f as default};
