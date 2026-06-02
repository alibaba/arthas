<script setup>
import SidebarItem from "@theme/SidebarItem.vue";

import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useSidebarItems } from "@vuepress/theme-default/client";

const route = useRoute();
const sidebarItems = useSidebarItems();
const searchQuery = ref("");

const isEnglish = computed(() => route.path.startsWith("/en/"));
const normalizedQuery = computed(() => normalizeText(searchQuery.value.trim()));
const isSearchActive = computed(() => normalizedQuery.value.length > 0);
const searchPlaceholder = computed(() => (isEnglish.value ? "Search" : "搜索"));
const clearSearchLabel = computed(() =>
  isEnglish.value ? "Clear search" : "清除搜索",
);
const noResultsText = computed(() =>
  isEnglish.value ? "No matches" : "没有匹配结果",
);

function normalizeText(value) {
  return String(value ?? "").toLocaleLowerCase();
}

function sidebarItemMatches(item, query) {
  return [item.text, item.link].some((value) =>
    normalizeText(value).includes(query),
  );
}

function filterSidebarItem(item, query) {
  const children = (item.children ?? [])
    .map((child) => filterSidebarItem(child, query))
    .filter(Boolean);

  if (!sidebarItemMatches(item, query) && children.length === 0) {
    return null;
  }

  return {
    ...item,
    children,
  };
}

const filteredSidebarItems = computed(() => {
  if (!isSearchActive.value) {
    return sidebarItems.value;
  }

  return sidebarItems.value
    .map((item) => filterSidebarItem(item, normalizedQuery.value))
    .filter(Boolean);
});

function clearSearch() {
  searchQuery.value = "";
}

onMounted(() => {
  watch(
    () => route.hash,
    (hash) => {
      const sidebar = document.querySelector(".sidebar");
      if (!sidebar) return;

      const activeSidebarItem = document.querySelector(
        `.sidebar a.sidebar-item[href="${route.path}${hash}"]`,
      );
      if (!activeSidebarItem) return;

      const { top: sidebarTop, height: sidebarHeight } =
        sidebar.getBoundingClientRect();
      const { top: activeSidebarItemTop, height: activeSidebarItemHeight } =
        activeSidebarItem.getBoundingClientRect();

      if (activeSidebarItemTop < sidebarTop) {
        activeSidebarItem.scrollIntoView(true);
      } else if (
        activeSidebarItemTop + activeSidebarItemHeight >
        sidebarTop + sidebarHeight
      ) {
        activeSidebarItem.scrollIntoView(false);
      }
    },
  );
});
</script>

<template>
  <div v-if="sidebarItems.length" class="sidebar-search-wrapper">
    <div class="sidebar-search" role="search">
      <span class="sidebar-search-icon" aria-hidden="true" />
      <input
        v-model="searchQuery"
        class="sidebar-search-input"
        type="search"
        :placeholder="searchPlaceholder"
        :aria-label="searchPlaceholder"
      />
      <button
        v-if="searchQuery"
        type="button"
        class="sidebar-search-clear"
        :aria-label="clearSearchLabel"
        @click="clearSearch"
      >
        ×
      </button>
    </div>
  </div>

  <ul v-if="filteredSidebarItems.length" class="sidebar-items">
    <SidebarItem
      v-for="item in filteredSidebarItems"
      :key="`${item.text}${item.link}`"
      :item="item"
      :force-open="isSearchActive"
    />
  </ul>

  <p v-else class="sidebar-search-empty">{{ noResultsText }}</p>
</template>

<style lang="scss" scoped>
.sidebar-search-wrapper {
  padding: 1rem 1rem 0;
}

.sidebar-search {
  display: flex;
  align-items: center;
  height: 2.5rem;
  border-radius: 8px;
  background: var(--c-bg-light);
  color: var(--c-text-light);
  transition: background-color var(--t-color);

  &:focus-within {
    background: var(--c-bg-lighter);
  }
}

.sidebar-search-icon {
  position: relative;
  flex: 0 0 1rem;
  width: 1rem;
  height: 1rem;
  margin-left: 0.85rem;
  border: 2px solid var(--c-text-lightest);
  border-radius: 50%;

  &::after {
    content: "";
    position: absolute;
    right: -0.35rem;
    bottom: -0.25rem;
    width: 0.45rem;
    height: 2px;
    border-radius: 999px;
    background: var(--c-text-lightest);
    transform: rotate(45deg);
  }
}

.sidebar-search-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  padding: 0 0.65rem;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--c-text);
  font: inherit;

  &::placeholder {
    color: var(--c-text-lightest);
  }

  &::-webkit-search-cancel-button {
    display: none;
  }
}

.sidebar-search-clear {
  flex: 0 0 2rem;
  width: 2rem;
  height: 2rem;
  margin-right: 0.25rem;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--c-text-light);
  cursor: pointer;
  font-size: 1.2rem;
  line-height: 1;

  &:hover,
  &:focus-visible {
    color: var(--c-text-accent);
  }

  &:focus-visible {
    outline: 0;
  }
}

.sidebar-items {
  padding-top: 1rem;
}

.sidebar-search-empty {
  margin: 1rem 1.5rem;
  color: var(--c-text-light);
  font-size: 0.95rem;
}
</style>
