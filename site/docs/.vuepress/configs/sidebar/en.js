module.exports = {
  "/en/doc": [
    {
      text: "DOCS",
      children: [
        "/en/doc/README.md",
        "/en/doc/quick-start.md",
        "/en/doc/install-detail.md",
        "/en/doc/download.md",
        "/en/doc/advanced-use.md",
        {
          text: "Other features",
          collapsible: true,
          children: [
            "/en/doc/async.md",
            "/en/doc/save-log.md",
            "/en/doc/batch-support.md",
            {
              text: "How to use ognl",
              link: "",
              children: [
                {
                  text: "Basic ognl example",
                  link: "https://github.com/alibaba/arthas/issues/11",
                },
                {
                  text: "Ognl special uses",
                  link: "https://github.com/alibaba/arthas/issues/71",
                },
              ],
            },
          ],
        },
        "/en/doc/commands.md",
        "/en/doc/web-console.md",
        "/en/doc/tunnel.md",
        "/en/doc/http-api.md",
        "/en/doc/docker.md",
        "/en/doc/spring-boot-starter.md",
        "/en/doc/idea-plugin.md",
        "/en/doc/faq.md",
        {
          text: "User cases",
          link: "https://github.com/alibaba/arthas/issues?q=label%3Auser-case",
        },
        {
          text: "Start me at github",
          link: "https://github.com/alibaba/arthas",
        },
        {
          text: "Compile and debug/CONTRIBUTING",
          link: "https://github.com/alibaba/arthas/blob/master/CONTRIBUTING.md",
        },
        {
          text: "Release Notes",
          link: "https://github.com/alibaba/arthas/releases",
        },
        {
          text: "Contact us",
          link: "/en/doc/contact-us.md",
        },
      ],
    },
  ],
};
