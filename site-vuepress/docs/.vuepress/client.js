import { defineClientConfig } from "@vuepress/client";
import { usePageData } from "@vuepress/client";
import oldContributorsData from "./oldContributorsData.json";

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
        contributor.email === oldContributor.email
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
  enhance({ router }) {
    router.afterEach((to, from) => {
      if (to.fullPath !== from.fullPath) {
        addOldDocsContributors();
      }
    });
  },
});
