<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { onBeforeMount, reactive } from 'vue';
import { interpret } from 'xstate';
const fetchS = fetchStore();
type ResultType = { "key": string, "value": string, rowspan: number };
const tableResultList: ResultType[] = reactive([])
const getSysenv = () => fetchS.baseSubmit(interpret(permachine), {
  action: "exec",
  command: "sysenv",
}).then(res => {
  tableResultList.length = 0
  let result = (res as CommonRes).body.results[0]
  if (result.type == "sysenv") {
    Object.entries(result.env).forEach(([key, value]) => {
      let rows: ResultType[] = []
        rows.push({
          key, value, rowspan: 1
        })
      tableResultList.push(...rows)
    })
    tableResultList.sort((a, b) => a.key > b.key ? 1 : -1)
  }
})
onBeforeMount(() => {
  getSysenv()
})
</script>

<template>
  <table class="table w-full table-compact">
    <thead>
      <tr>
        <th>key</th>
        <th>value</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(map, i) in tableResultList" :key="i" class="hover">
        <th v-if="map.rowspan > 0" :rowspan="map.rowspan">{{map.key}}</th>
        <td>{{map.value}}</td>
      </tr>
    </tbody>
    <tfoot>
      <tr>
        <th>key</th>
        <th>value</th>
      </tr>
    </tfoot>
  </table>
</template>