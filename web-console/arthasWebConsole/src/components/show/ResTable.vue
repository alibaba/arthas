<script setup lang="ts">
import { reactive, ref } from 'vue';

const {maplist} = defineProps<{
  maplist:Map<string, string>[]
}>()

const hlist= ref([] as string[])
if(maplist.length > 0) {
  hlist.value = Object.keys(maplist[0])
}
</script>

<template>
<table class="border-collapse border border-slate-400 ..." v-if="hlist.length > 0">
  <thead>
    <tr>
      <th class="border border-slate-300 ..." v-for="(v,i) in hlist" :key="i"></th>
    </tr>
  </thead>
  <tbody>
    <tr v-for="(map, i) in maplist" :key="i">
      <td class="border border-slate-300 ..." v-for="(key,j) in hlist" :key="j">{{map.get(key)}}</td>
    </tr>
  </tbody>
</table>
</template>

<style scoped>

</style>