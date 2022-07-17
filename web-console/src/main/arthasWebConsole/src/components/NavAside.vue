<script setup lang="ts">

</script>

<template>
  <div class=" h-full w-max">
    <ul class=" flex flex-col justify-center">
      <li>会话</li>
      <li>功能选择</li>
      <li>项目配置</li>
      <li>log</li>
      <li>console</li>
    </ul>
    <ul class=" w-0 overflow-hidden">
      <slot name="detail">
      </slot>
    </ul>
  </div>
</template>

<style scoped>
</style>