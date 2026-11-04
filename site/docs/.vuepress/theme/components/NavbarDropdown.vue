<script setup>
import Translate from "./icons/Translate.vue";

import AutoLink from "@theme/AutoLink.vue";
import DropdownTransition from "@theme/DropdownTransition.vue";
import { computed, ref, toRefs, watch } from "vue";
import { useRoute } from "vue-router";

const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
});

const { item } = toRefs(props);

const dropdownAriaLabel = computed(
  () => item.value.ariaLabel || item.value.text,
);

const open = ref(false);
const route = useRoute();
watch(
  () => route.path,
  () => {
    open.value = false;
  },
);

/**
 * Open the dropdown when user tab and click from keyboard.
 *
 * Use event.detail to detect tab and click from keyboard.
 * The Tab + Click is UIEvent > KeyboardEvent, so the detail is 0.
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/API/UIEvent/detail
 */
const handleDropdown = (e) => {
  const isTriggerByTab = e.detail === 0;
  if (isTriggerByTab) {
    open.value = !open.value;
  } else {
    open.value = false;
  }
};

const isLastItemOfArray = (item, arr) => arr[arr.length - 1] === item;
</script>

<template>
  <div class="navbar-dropdown-wrapper" :class="{ open }">
    <button
      class="navbar-dropdown-title"
      type="button"
      :aria-label="dropdownAriaLabel"
      @click="handleDropdown"
    >
      <Translate v-if="item.text === 'Languages'" />
      <span class="title" v-else>{{ item.text }}</span>
      <span class="arrow down" />
    </button>

    <button
      class="navbar-dropdown-title-mobile"
      type="button"
      :aria-label="dropdownAriaLabel"
      @click="open = !open"
    >
      <Translate v-if="item.text === 'Languages'" />
      <span class="title" v-else>{{ item.text }}</span>
      <span class="arrow" :class="open ? 'down' : 'right'" />
    </button>

    <DropdownTransition>
      <ul v-show="open" class="navbar-dropdown">
        <li
          v-for="child in item.children"
          :key="child.text"
          class="navbar-dropdown-item"
        >
          <template v-if="child.children">
            <h4 class="navbar-dropdown-subtitle">
              <AutoLink
                v-if="child.link"
                :item="child"
                @focusout="
                  isLastItemOfArray(child, item.children) &&
                  child.children.length === 0 &&
                  (open = false)
                "
              />

              <span v-else>{{ child.text }}</span>
            </h4>

            <ul class="navbar-dropdown-subitem-wrapper">
              <li
                v-for="grandchild in child.children"
                :key="grandchild.link"
                class="navbar-dropdown-subitem"
              >
                <AutoLink
                  :item="grandchild"
                  @focusout="
                    isLastItemOfArray(grandchild, child.children) &&
                    isLastItemOfArray(child, item.children) &&
                    (open = false)
                  "
                />
              </li>
            </ul>
          </template>

          <template v-else>
            <AutoLink
              :item="child"
              @focusout="
                isLastItemOfArray(child, item.children) && (open = false)
              "
            />
          </template>
        </li>
      </ul>
    </DropdownTransition>
  </div>
</template>
