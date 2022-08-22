<script setup lang="ts">
import { useRouter } from 'vue-router';

const routers: { cmd: string, url: string }[] = [
  {
    cmd: "thread",
    url: "thread"
  }, {
    cmd: "jvm",
    url: "jvm"
  }, {
    cmd: "memory",
    url: "memory"
  }, {
    cmd: "perfcounter",
    url: "perfcounter"
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
  },{
    cmd:"heapdump",
    url:"heapdump"
  }
]
const router = useRouter()
</script>

<template>
  <ul class=" w-40 border-l bg-gray-200 ">
    <li v-for="(v, i) in routers" :key="i">
      <button @click="() => router.push(v.url)"
        class=" transition w-full h-12 bg-gray-300 focus:bg-gray-500 hover:bg-slate-400">
        {{ v.cmd }}
      </button>
    </li>
  </ul>
</template>

<style scoped>
</style>