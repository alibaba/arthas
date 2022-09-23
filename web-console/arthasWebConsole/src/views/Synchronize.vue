<script setup lang="ts">
import SelectCmd from '@/components/routeTo/SelectCmd.vue';
const routes: { cmd: string, url: string }[] = [
  {
    cmd: "thread",
    url: "thread"
  // }, {
  //   cmd: "memory",
  //   url: "memory"
  // }, {
  //   cmd: "perfcounter",
  //   url: "perfcounter"
  }, {
    cmd: "classLoader",
    url: "classLoader"
  }, {
    cmd: "jad",
    url: "jad"
  }, {
    cmd: "retransform",
    url: "retransform"
  }, {
    cmd: "mbean",
    url: "mbean"
  }, {
    cmd: "heapdump",
    url: "heapdump"
  }, {
    cmd: "vmtool",
    url: "vmtool"
  }, {
    cmd: "reset",
    url: "reset"
  },{
    cmd: "ognl",
    url: "ognl"
  }
]
</script>

<template>
  <div class="h-[90vh] overflow-auto flex">
    <SelectCmd :routes="routes"></SelectCmd>
    <div class="p-2 overflow-y-scroll w-full flex flex-col flex-1">
      <RouterView></RouterView>
    </div>
  </div>
</template>

<style scoped>
</style>