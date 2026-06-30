<!--
Base From Vdoing Theme
SRC: https://github.com/xugaoyi/vuepress-theme-vdoing/blob/master/vdoing/components/RightMenu.vue 
-->

<template>
  <div class="right-menu-wrapper">
    <div class="right-menu-margin">
      <div class="right-menu-title">{{ menu }}</div>
      <div class="right-menu-content">
        <template v-for="(item, i) in headers" :key="i">
          <div
            :class="[
              'right-menu-item',
              'level' + item.level,
              { active: item.slug === hashText },
            ]"
          >
            <a
              :href="'#' + item.slug"
              v-if="item.title.replace(/[^\x00-\xff]/g, '01').length >= 36"
              :title="item.title"
              >{{ item.title }}</a
            >
            <a :href="'#' + item.slug" v-else>{{ item.title }}</a>
          </div>
          <div
            :class="[
              'right-menu-item',
              'level' + subItem.level,
              { active: subItem.slug === hashText },
            ]"
            v-for="(subItem, j) in item.children"
            :key="j"
          >
            <a
              :href="'#' + subItem.slug"
              v-if="subItem.title.replace(/[^\x00-\xff]/g, '01').length >= 35"
              :title="subItem.title"
              >{{ subItem.title }}</a
            >
            <a :href="'#' + subItem.slug" v-else>{{ subItem.title }}</a>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRoute } from "vue-router";
import { onMounted, watch, ref } from "vue";
import { usePageData } from "@vuepress/client";
import { useThemeLocaleData } from "@vuepress/plugin-theme-data/client";

const pages = usePageData();
const theme = useThemeLocaleData();

const menu = ref("");
const headers = ref([]);
let hashText = ref("");

menu.value = theme.value.rightMenuText ?? "Table of Contents";

function getHeadersData() {
  headers.value = pages.value.headers;
}

function getHashText() {
  hashText.value = decodeURIComponent(window.location.hash.slice(1));
}

onMounted(() => {
  getHeadersData();
  getHashText();
});

watch(useRoute(), () => {
  headers.value = pages.value.headers;
  getHashText();
});
</script>

<style lang="scss" scoped>
$rightMenuWidth: 280px;

.theme-default-content {
  .right-menu-wrapper {
    .right-menu-margin {
      border-left: 1px solid var(--c-border);
    }
  }
}
.right-menu-wrapper {
  width: $rightMenuWidth;
  float: right;
  margin-right: -($rightMenuWidth + 20px);

  position: sticky;
  top: 0;
  font-size: 0.8rem;
  .right-menu-margin {
    margin-top: calc(var(--navbar-height) + 1rem);
    border-radius: 3px;
    overflow: hidden;
  }
  .right-menu-title {
    padding: 10px 15px 0 15px;
    background: var(--mainBg);
    font-size: 1rem;
    &:after {
      content: "";
      display: block;
      width: 100%;
      height: 1px;
      background: var(--c-border);
      margin-top: 10px;
    }
  }
  .right-menu-content {
    max-height: 80vh;
    position: relative;
    overflow: hidden;
    background: var(--mainBg);
    padding: 4px 3px 4px 0;
    &::-webkit-scrollbar {
      width: 3px;
      height: 3px;
    }
    &::-webkit-scrollbar-track-piece {
      background: none;
    }
    &::-webkit-scrollbar-thumb:vertical {
      background-color: hsla(0, 0%, 49%, 0.3);
    }
    &:hover {
      overflow-y: auto;
      padding-right: 0;
    }
    .right-menu-item {
      padding: 4px 0px 0px 15px;

      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
      position: relative;
      &.level2 {
        font-size: 0.8rem;
      }
      &.level3 {
        padding-left: 27px;
      }
      &.level4 {
        padding-left: 37px;
      }
      &.level5 {
        padding-left: 47px;
      }
      &.level6 {
        padding-left: 57px;
      }
      &.active {
        &:before {
          content: "";
          position: absolute;
          top: 5px;
          left: 0;
          width: 3px;
          height: 14px;
          background: var(--c-text-accent);
          border-radius: 0 4px 4px 0;
        }
        a {
          color: var(--c-text-accent);
          opacity: 1;
        }
      }
      a {
        color: var(--textColor);
        opacity: 0.75;
        display: inline-block;
        width: 100%;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
        &:hover {
          opacity: 1;
        }
      }
    }
  }
}

@media (max-width: 1300px) {
  .theme-default-content .right-menu-wrapper {
    display: none;
  }
}
</style>
