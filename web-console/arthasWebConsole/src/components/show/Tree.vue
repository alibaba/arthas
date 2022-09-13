<script setup lang="ts">
// @ts-nocheck
// 忽略文件报错
import {
  Disclosure,
  DisclosurePanel,
  DisclosureButton
} from "@headlessui/vue"
const { root, classList = "",buttonClass='' } = defineProps<{
  root: TreeNode
  classList?: string[] | Record<string, boolean> | string,
  buttonClass?:string[]|string
}>()
</script>

<template>
  <div :class="classList" v-if="root">
    <Disclosure>
      <DisclosureButton :class="buttonClass">
        <slot name="meta" :data="root.meta" :active="root.children.length > 0"></slot>
      </DisclosureButton>
      <template v-if='root.children !== undefined && root.children.length > 0'>
        <DisclosurePanel class="pl-4 border-l border-black">
          <Tree v-for="(child, i) in root.children" :key="i" :root="child" :class-list="classList">
            <!-- meta = {data:child.meta} -->
            <!-- /**@ts-ignore */ -->
            <template #meta="{data, active}">
              <slot name="meta" :data="data" :active="active"></slot>
            </template>
          </Tree>
        </DisclosurePanel>
      </template>
    </Disclosure>
  </div>
</template>

<style scoped>

</style> 