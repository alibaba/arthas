<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { onBeforeMount, reactive } from 'vue';
import { interpret } from 'xstate';
const fetchS = fetchStore();
type ResultType = MergeObj<Perfcounter,{rowspan:number}>
const perfcounterList: ResultType[] = reactive([])
let keyList: (keyof Perfcounter)[] = [
  "name",
  "units",
  "variability",
  "value",
]
const getPerCounter = () => fetchS.baseSubmit(interpret(permachine), { action: "exec", command: "perfcounter -d" }).then(res => {
  const result = (res as CommonRes).body.results[0]
  if (result.type === "perfcounter") {
    const perfcounters = result.perfCounters
    perfcounterList.length = 0;
    perfcounters.forEach(perfcouter => {
      let result = {...perfcouter,rowspan:1}
      result.value = result.value.toString()
      perfcounterList.push(result)
    })
    perfcounterList.sort((a, b) => a.name > b.name ? 1 : -1)
  }
})

onBeforeMount(() => {
  getPerCounter()
})
</script>

<template>
  <table class="table w-full table-compact">
    <thead>
      <tr>
        <th v-for="(v,i) in keyList" :key="i">{{v}}</th>
      </tr>
    </thead>
    <tbody class="">
      <tr v-for="(map, i) in perfcounterList" :key="i" class="hover">
        <template v-for="(key,j) in keyList" :key="j">
          <!-- 展示合并的单元格且定住name -->
          <th v-if="key == 'name' && map.rowspan > 0" :rowspan="map.rowspan">
            {{map[key]}}
          </th>
          <!-- 只有value才会裂变 -->
          <td v-else-if="key === 'value'" >
            <div class="break-all">{{map[key]}}</div>
          </td>
          <!-- 展示合并的单元格 -->
          <td v-else-if="map.rowspan > 0" :rowspan="map.rowspan">
            {{map[key]}}
          </td>
        </template>
      </tr>
    </tbody>
    <tfoot>
      <tr>
        <th v-for="(v,i) in keyList" :key="i" class="break-words">{{v}}</th>
      </tr>
    </tfoot>
  </table>
</template>

<style scoped>

</style>