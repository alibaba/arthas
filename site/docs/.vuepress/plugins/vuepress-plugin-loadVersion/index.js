import { readFileSync } from "fs";
import fetch from "node-fetch";
import { xml2js } from "xml-js";

export function loadVersionPlugin() {
  const data = readFileSync("../pom.xml");
  const pom = xml2js(data.toString(), { compact: true });

  const getVersionByMaven = async () => {
    return await fetch(
      "https://search.maven.org/solrsearch/select?q=arthas-site&rows=1&wt=json",
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
}
