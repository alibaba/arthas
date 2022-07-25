module.exports = [
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
];
