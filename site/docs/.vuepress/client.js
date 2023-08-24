import oldContributorsData from "./oldContributorsData.json";

import { usePageData, defineClientConfig } from "@vuepress/client";
import CountTo from "vue-count-to/src/vue-countTo.vue";

const addOldDocsContributors = () => {
  const page = usePageData();
  if (!page.value.git) return;
  const filePath = page.value.filePathRelative;
  const contributors = page.value.git.contributors;
  const oldContributors = oldContributorsData[filePath];

  const haveSameContributor = (contributors, oldContributor) => {
    return contributors.find(
      (contributor) =>
        contributor.name === oldContributor.name &&
        contributor.email === oldContributor.email,
    );
  };

  if (oldContributors) {
    oldContributors.forEach((oldContributor) => {
      if (!haveSameContributor(contributors, oldContributor)) {
        contributors.push(oldContributor);
      } else {
        haveSameContributor(contributors, oldContributor).commits +=
          oldContributor.commits;
      }
    });
  }

  // sort contributors by commits
  contributors?.sort((a, b) => b.commits - a.commits);
};

export default defineClientConfig({
  enhance({ router, app }) {
    // register global components
    app.component("CountTo", CountTo);

    // add old docs contributors
    router.afterEach((to, from) => {
      if (to.path !== from.path) {
        addOldDocsContributors();
      }
    });

    // baidu analytics
    router.beforeEach((to, from, next) => {
      if (typeof _hmt != "undefined") {
        if (to.path && to.fullPath !== from.fullPath) {
          _hmt.push(["_trackPageview", to.fullPath]);
        }
      }

      next();
    });
  },
});
