<script setup lang="ts">
import SelectCmd from '@/components/SelectCmd.vue';

</script>

<template>
  <div class="h-[90vh] overflow-auto flex">
    <SelectCmd></SelectCmd>
    <RouterView></RouterView>
  </div>
</template>

<style scoped>
</style>