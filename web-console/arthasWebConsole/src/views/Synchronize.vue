<script setup lang="ts">
import SelectCmd from '@/components/routeTo/SelectCmd.vue';

</script>

<template>
  <div class="h-[90vh] overflow-auto flex">
    <SelectCmd></SelectCmd>
    <div class="p-2 overflow-y-scroll w-full flex flex-col flex-1">
      <RouterView></RouterView>
    </div>
  </div>
</template>

<style scoped>
</style>