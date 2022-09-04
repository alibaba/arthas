<script setup lang="ts">
import { HtmlAttributes } from 'csstype';


const { root, classList = "" } = defineProps<{
  root: TreeNode
  classList?: string[] | Record<string, boolean> | string
}>()
</script>

<template>
  <div :class="classList" v-if="root">
    <slot name="meta" :data="root.meta"></slot>
    <template v-if='Object.hasOwn(root, "children") && root.children !== undefined && root.children.length > 0'>
      <Tree v-for="(child, i) in root.children" :key="i" :root="child" :class-list="classList">
        <!-- meta = {data:child.meta} -->
        <template #meta="{data}">
          <slot name="meta" :data="data"></slot>
        </template>
      </Tree>
    </template>
  </div>
</template>

<style scoped>
</style> 