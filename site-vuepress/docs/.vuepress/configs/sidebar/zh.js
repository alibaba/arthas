module.exports = {
  "/doc/": [
    {
      text: "文档",
      children: [
        "/doc/README.md",
        "/doc/quick-start.md",
        {
          text: "在线教程(阿里云)",
          link: "https://start.aliyun.com/handson-lab?category=arthas",
        },
        "/doc/install-detail.md",
        "/doc/download.md",
        "/doc/advanced-use.md",
        {
          text: "其他特性",
          collapsible: true,
          children: [
            "/doc/async.md",
            "/doc/save-log.md",
            "/doc/batch-support.md",
            {
              text: "ognl 表达式用法",
              link: "",
              children: [
                {
                  text: "活用ognl表达式",
                  link: "https://github.com/alibaba/arthas/issues/11",
                },
                {
                  text: "一些ognl特殊用法",
                  link: "https://github.com/alibaba/arthas/issues/71",
                },
              ],
            },
          ],
        },
        {
          text: "命令列表",
          collapsible: true,
          link: "/doc/commands.md",
          children: [
            {
              text: "jvm相关",
              collapsible: false,
              children: [
                "/doc/dashboard.md",
                "/doc/thread.md",
                "/doc/jvm.md",
                "/doc/memory.md",
                "/doc/sysprop.md",
                "/doc/sysenv.md",
                "/doc/vmoption.md",
                "/doc/perfcounter.md",
                "/doc/logger.md",
                "/doc/mbean.md",
                "/doc/getstatic.md",
                "/doc/ognl.md",
                "/doc/heapdump.md",
                "/doc/vmtool.md",
              ],
            },
            {
              text: "class/classloader相关",
              collapsible: false,
              children: [
                "/doc/sc.md",
                "/doc/sm.md",
                "/doc/jad.md",
                "/doc/classloader.md",
                "/doc/mc.md",
                "/doc/dump.md",
                "/doc/retransform.md",
                "/doc/redefine.md",
              ],
            },
            {
              text: "monitor/watch/trace相关",
              collapsible: false,
              children: [
                "/doc/monitor.md",
                "/doc/watch.md",
                "/doc/trace.md",
                "/doc/stack.md",
                "/doc/tt.md",
              ],
            },
            {
              text: "其他",
              collapsible: false,
              children: [
                "/doc/profiler.md",
                "/doc/cat.md",
                "/doc/echo.md",
                "/doc/grep.md",
                "/doc/base64.md",
                "/doc/tee.md",
                "/doc/pwd.md",
                "/doc/auth.md",
                "/doc/options.md",
              ],
            },
            {
              text: "基础命令",
              collapsible: false,
              children: [
                "/doc/help.md",
                "/doc/cls.md",
                "/doc/session.md",
                "/doc/reset.md",
                "/doc/history.md",
                "/doc/quit.md",
                "/doc/stop.md",
                {
                  text: "keymap",
                  link: "/doc/keymap.md",
                },
              ],
            },
          ],
        },
        "/doc/web-console.md",
        "/doc/tunnel.md",
        "/doc/http-api.md",
        "/doc/docker.md",
        "/doc/spring-boot-starter.md",
        "/doc/idea-plugin.md",
        "/doc/faq.md",
        {
          text: "用户案列",
          link: "https://github.com/alibaba/arthas/issues?q=label%3Auser-case",
        },
        {
          text: "Start me at github",
          link: "https://github.com/alibaba/arthas",
        },
        {
          text: "编译调试/参与贡献",
          link: "https://github.com/alibaba/arthas/blob/master/CONTRIBUTING.md",
        },
        {
          text: "Release Notes",
          link: "https://github.com/alibaba/arthas/releases",
        },
        {
          text: "QQ群/钉钉群",
          link: "/doc/contact-us.md",
        },
      ],
    },
  ],
};
