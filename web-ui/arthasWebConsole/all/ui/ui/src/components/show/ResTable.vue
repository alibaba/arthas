<script setup lang="ts">
import { ref, toRefs } from 'vue';

const _props = defineProps<{
  maplist:Map<string, string>[]
}>()
const props = toRefs(_props)

const hlist= ref([] as string[])
if(props.maplist.value.length > 0) {
  console.dir(hlist)
  hlist.value = Object.keys(props.maplist.value[0])
}
</script>

<template>
<table class="border-collapse border border-slate-400 ...">
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