<script setup lang="ts">
import SelectCmd from '@/components/routeTo/SelectCmd.vue';

const routes: { cmd: string, url: string }[] = [
  { cmd: 'tt', url: 'tt' },
  {
    cmd: 'stack', url: "stack"
  }
]
</script>

<template>
  <div class="flex">
    <SelectCmd :routes="routes"></SelectCmd>
    <div class="p-2 h-[90vh] overflow-auto">
      <RouterView></RouterView>
    </div>
  </div>
</template>

<style scoped>
</style>