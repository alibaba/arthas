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
        "/doc/commands.md",
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
