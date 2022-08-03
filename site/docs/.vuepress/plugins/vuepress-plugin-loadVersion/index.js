const fs = require("fs");
const fetch = require("node-fetch");
const convert = require("xml-js");

exports.loadVersionPlugin = () => {
  const data = fs.readFileSync("../pom.xml");
  const pom = convert.xml2js(data.toString(), { compact: true });

  const getVersionByMaven = async () => {
    return await fetch(
      "https://search.maven.org/solrsearch/select?q=arthas&rows=1&wt=json"
    )
      .then((res) => res.json())
      .then((res) => res.response.docs[0].latestVersion);
  };

  var version = pom.project.properties.revision._text;

  return {
    name: "vuepress-plugin-loadVersion",
    onInitialized: async (app) => {
      if (version.includes("SNAPSHOT")) {
        version = await getVersionByMaven();
      }

      app.pages.map((page) => (page.data.version = version));
    },
  };
};
