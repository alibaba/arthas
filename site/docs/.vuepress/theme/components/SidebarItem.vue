<script setup>
import AutoLink from "@theme/AutoLink.vue";
import DropdownTransition from "@theme/DropdownTransition.vue";

import { computed, nextTick, onBeforeUnmount, ref, toRefs, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
  depth: {
    type: Number,
    required: false,
    default: 0,
  },
  forceOpen: {
    type: Boolean,
    required: false,
    default: false,
  },
});

const { item, depth, forceOpen } = toRefs(props);
const route = useRoute();
const router = useRouter();

function normalizePath(path) {
  return decodeURI(path)
    .replace(/#.*$/, "")
    .replace(/(index)?\.(md|html)$/, "");
}

function isActiveLink(link, currentRoute) {
  if (currentRoute.hash === link) {
    return true;
  }

  return normalizePath(currentRoute.path) === normalizePath(link);
}

function isActiveSidebarItem(sidebarItem, currentRoute) {
  if (sidebarItem.link && isActiveLink(sidebarItem.link, currentRoute)) {
    return true;
  }

  if (sidebarItem.children) {
    return sidebarItem.children.some((child) =>
      isActiveSidebarItem(child, currentRoute),
    );
  }

  return false;
}

const hasChildren = computed(() => Boolean(item.value.children?.length));
const usesTreeRow = computed(() => Boolean(item.value.link) && depth.value > 0);
const isActive = computed(() => isActiveSidebarItem(item.value, route));
const isCollapsible = computed(
  () => Boolean(item.value.collapsible) && hasChildren.value,
);
const isOpenDefault = computed(() =>
  isCollapsible.value ? forceOpen.value || isActive.value : true,
);
const isOpen = ref(isOpenDefault.value);

const itemClass = computed(() => ({
  "sidebar-item": true,
  "sidebar-heading": depth.value === 0,
  active: isActive.value,
  collapsible: isCollapsible.value,
}));

const rowClass = computed(() => ({
  "sidebar-tree-row": true,
  "sidebar-tree-heading-row": depth.value === 0,
  active: isActive.value,
  collapsible: isCollapsible.value,
}));

const toggleLabel = computed(() => {
  const isEnglish = route.path.startsWith("/en/");

  if (isOpen.value) {
    return `${isEnglish ? "Collapse" : "收起"} ${item.value.text}`;
  }

  return `${isEnglish ? "Expand" : "展开"} ${item.value.text}`;
});

function toggleIsOpen() {
  if (isCollapsible.value) {
    isOpen.value = !isOpen.value;
  }
}

function onHeadingClick(event) {
  if (!isCollapsible.value) {
    return;
  }

  event.preventDefault();
  toggleIsOpen();
}

const unregisterRouterHook = router.afterEach(() => {
  nextTick(() => {
    isOpen.value = isOpenDefault.value;
  });
});

watch(isOpenDefault, (value) => {
  isOpen.value = value;
});

onBeforeUnmount(() => {
  unregisterRouterHook();
});
</script>

<template>
  <li>
    <div v-if="usesTreeRow" :class="rowClass">
      <button
        v-if="isCollapsible"
        type="button"
        class="sidebar-tree-toggle"
        :aria-expanded="isOpen"
        :aria-label="toggleLabel"
        @click="toggleIsOpen"
      >
        <span
          class="sidebar-tree-chevron"
          :class="{ open: isOpen }"
          aria-hidden="true"
        />
      </button>
      <span v-else class="sidebar-tree-toggle-placeholder" />

      <AutoLink :class="itemClass" :item="item" />
    </div>

    <AutoLink v-else-if="item.link" :class="itemClass" :item="item" />

    <p
      v-else
      tabindex="0"
      :class="itemClass"
      @click="onHeadingClick"
      @keydown.enter="onHeadingClick"
    >
      {{ item.text }}
      <span
        v-if="isCollapsible"
        class="sidebar-tree-chevron"
        :class="{ open: isOpen }"
        aria-hidden="true"
      />
    </p>

    <DropdownTransition v-if="hasChildren">
      <ul v-show="isOpen" class="sidebar-item-children">
        <SidebarItem
          v-for="child in item.children"
          :key="`${depth}${child.text}${child.link}`"
          :item="child"
          :depth="depth + 1"
          :force-open="forceOpen"
        />
      </ul>
    </DropdownTransition>
  </li>
</template>

<style lang="scss" scoped>
.sidebar-tree-row {
  display: flex;
  align-items: center;
  min-height: 2rem;
  margin: 0.125rem 0.75rem;
  border-left: 0.25rem solid transparent;
  border-radius: 6px;
  transition:
    background-color var(--t-color),
    border-color var(--t-color);

  &.active,
  &:hover {
    background: var(--c-bg-light);
  }

  &.active {
    border-left-color: var(--c-text-accent);
  }

  :deep(.sidebar-item) {
    flex: 1;
    min-width: 0;
    border-left: 0;
    padding-left: 0.35rem;
  }

  :deep(.sidebar-item.active:not(p.sidebar-heading)) {
    border-left-color: transparent;
  }
}

.sidebar-tree-heading-row {
  margin-right: 1rem;
  margin-left: 1rem;

  :deep(.sidebar-heading) {
    padding-left: 0.35rem;
  }
}

.sidebar-tree-toggle,
.sidebar-tree-toggle-placeholder {
  flex: 0 0 1.25rem;
  width: 1.25rem;
  height: 1.75rem;
  margin-left: 0.1rem;
}

.sidebar-tree-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  appearance: none;
  -webkit-appearance: none;
  color: inherit;
  font: inherit;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;

  &:focus-visible {
    outline: 0;

    .sidebar-tree-chevron {
      border-color: var(--c-text-accent);
    }
  }
}

.sidebar-tree-toggle-placeholder {
  display: inline-block;
}

.sidebar-tree-chevron {
  width: 0.4rem;
  height: 0.4rem;
  border-right: 2px solid var(--c-text-light);
  border-bottom: 2px solid var(--c-text-light);
  transform: rotate(-45deg);
  transition: transform var(--t-transform);

  &.open {
    transform: rotate(45deg);
  }
}
</style>
