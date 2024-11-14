import{_ as s,o as t,c as a,a as e,b as l,e as n,d as r,r as d}from"./app.1e03d722.js";const c={},o=e("h1",{id:"grep",tabindex:"-1"},[e("a",{class:"header-anchor",href:"#grep","aria-hidden":"true"},"#"),n(" grep")],-1),v={href:"https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-grep",target:"_blank",rel:"noopener noreferrer"},u=e("code",null,"grep",-1),m=n(" online tutorial"),p=r(`<div class="custom-container tip"><p class="custom-container-title">TIP</p><p>Similar to the traditional <code>grep</code> command.</p></div><h2 id="usage" tabindex="-1"><a class="header-anchor" href="#usage" aria-hidden="true">#</a> Usage</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code> USAGE:
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
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,3);function g(h,b){const i=d("ExternalLinkIcon");return t(),a("div",null,[o,e("p",null,[e("a",v,[u,m,l(i)])]),p])}const f=s(c,[["render",g],["__file","grep.html.vue"]]);export{f as default};
