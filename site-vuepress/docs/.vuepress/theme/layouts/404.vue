<script setup>
import { useRouteLocale } from "@vuepress/client";
import { useThemeLocaleData } from "@vuepress/plugin-theme-data/lib/client";

const routeLocale = useRouteLocale();
const themeLocale = useThemeLocaleData();

const isRouteToHome =
  typeof window !== "undefined"
    ? window.location.pathname in ["/zh-cn", "/en-us"]
    : false;

const messages = themeLocale.value.notFound ?? ["Not Found"];
const getMsg = () => messages[Math.floor(Math.random() * messages.length)];

const homeLink = themeLocale.value.home ?? routeLocale.value;
const homeText = themeLocale.value.backToHome ?? "Back to home";
</script>

<template>
  <div
    :class="[isRouteToHome ? 'theme-container no-sidebar' : 'theme-container']"
  >
    <main class="page">
      <div class="theme-default-content">
        <h1>404</h1>

        <blockquote>{{ getMsg() }}</blockquote>

        <a :href="homeLink">{{ homeText }}</a>
      </div>
    </main>
  </div>
</template>
