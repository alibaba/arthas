const fs = require("fs");
const fetch = require("node-fetch");
const convert = require("xml-js");

exports.loadVersionPlugin = () => {
  var version;

  const data = fs.readFileSync("../pom.xml");
  const pom = convert.xml2js(data.toString(), { compact: true });

  const getVersionByMaven = async () => {
    return await fetch(
      "https://search.maven.org/solrsearch/select?q=arthas&rows=1&wt=json"
    )
      .then((res) => res.json())
      .then((res) => res.response.docs[0].latestVersion);
  };

  version = pom.project.properties.revision._text;

  return {
    name: "vuepress-plugin-loadVersion",
    extendsPage: async (page) => {
      const injectPagePaths = ["/", "/en/"];
      if (!injectPagePaths.includes(page.data.path)) return;

      if (version.includes("SNAPSHOT")) {
        page.data.version = await getVersionByMaven();
      } else {
        page.data.version = version;
      }
    },
  };
};
