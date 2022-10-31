<script setup lang="ts">
import SelectCmd from '@/components/routeTo/SelectCmd.vue';
// 初始化
const routes: { cmd: string, url: string }[] = [
  {
    cmd: "JVM",
    url: "jvm"
  },{
    cmd:"system property",
    url:"sysprop"
  },{
    cmd:"system environment variables",
    url:"sysenv"
  },{
    cmd:"JVM Perf Counter information",
    url:"PerCounter"
  },{
    cmd:"JVM diagnostic options",
    url:"vmoption"
  },{
    cmd:"global options",
    url:"options"
  }
]
</script>

<template>
  <div class="h-[90vh] overflow-auto flex">
  <SelectCmd :routes="routes"></SelectCmd>
    <div class="overflow-auto py-2 w-full flex flex-col flex-1 pointer-events-auto">
      <RouterView></RouterView>
    </div>
  </div>
</template>

<style scoped>

</style>