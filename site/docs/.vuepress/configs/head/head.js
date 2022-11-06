export const head = [
  ["link", { rel: "icon", href: "/images/favicon.ico" }],
  [
    "meta",
    { name: "viewport", content: "width=device-width, initial-scale=1.0" },
  ],
  ["meta", { property: "og:title", content: "Arthas" }],
  [
    "meta",
    {
      property: "og:image:alt",
      content:
        "Alibaba Java Diagnostic Tool Arthas/Alibaba Java诊断利器Arthas - alibaba/arthas: Alibaba Java Diagnostic Tool Arthas/Alibaba Java诊断利器Arthas",
    },
  ],
  ["meta", { property: "og:image", content: "/images/arthas_mate_image.png" }],
  [
    "meta",
    {
      property: "og:description",
      content:
        "Alibaba Java Diagnostic Tool Arthas/Alibaba Java诊断利器Arthas - alibaba/arthas: Alibaba Java Diagnostic Tool Arthas/Alibaba Java诊断利器Arthas",
    },
  ],
  ["meta", { property: "og:image:width", content: "1200" }],
  ["meta", { property: "og:image:height", content: "600" }],
  [
    "meta",
    { property: "twitter:image:src", content: "/images/arthas_mate_image.png" },
  ],
  [
    "meta",
    {
      property: "twitter:image:alt",
      content:
        "Alibaba Java Diagnostic Tool Arthas/Alibaba Java诊断利器Arthas - alibaba/arthas: Alibaba Java Diagnostic Tool Arthas/Alibaba Java诊断利器Arthas",
    },
  ],
  // QQ meta
  [
    "meta",
    {
      itemprop: "name",
      content: "Arthas",
    },
  ],
  [
    "meta",
    {
      itemprop: "image",
      content: "/images/arthas_mate_image.png",
    },
  ],
  [
    "meta",
    {
      itemprop: "description",
      content:
        "Alibaba Java Diagnostic Tool Arthas/Alibaba Java诊断利器Arthas - alibaba/arthas: Alibaba Java Diagnostic Tool Arthas/Alibaba Java诊断利器Arthas",
    },
  ],
  // baidu analytics
  [
    "script",
    {},
    `
    var _hmt = _hmt || [];
    (function() {
      var hm = document.createElement("script");
      hm.src = "https://hm.baidu.com/hm.js?d5c5e25b100f0eb51a4c35c8a86ea9b4";
      var s = document.getElementsByTagName("script")[0]; 
      s.parentNode.insertBefore(hm, s);
    })();
    `,
  ],
  // aplus
  [
    "meta",
    {
      name: "aes-config",
      content: "pid=xux-opensource&user_type=101&uid=&username=&dim10=arthas",
    },
  ],
  [
    "script",
    {
      src: "//g.alicdn.com/alilog/mlog/aplus_v2.js",
      id: "beacon-aplus",
      exparams: "clog=o&aplus&sidx=aplusSidx&ckx=aplusCkx",
    },
  ],
  [
    "script",
    {
      src: "//g.alicdn.com/aes/??tracker/1.0.34/index.js,tracker-plugin-pv/2.4.5/index.js,tracker-plugin-event/1.2.5/index.js,tracker-plugin-jserror/1.0.13/index.js,tracker-plugin-api/1.1.14/index.js,tracker-plugin-perf/1.1.8/index.js,tracker-plugin-eventTiming/1.0.4/index.js",
    },
  ],
];
