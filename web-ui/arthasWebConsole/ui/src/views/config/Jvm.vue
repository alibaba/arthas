<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { onBeforeMount, reactive } from 'vue';
import { interpret } from 'xstate';
const fetchS = fetchStore();
type ResultType = { "key": string, "value": string};
const tableResultList: ResultType[] = reactive([])
// 尝试转化为tree，不过分表可能会合理一些
const transformToString: <T=Record<string,unknown>>(obj:T)=>T = (obj)=>{
  type Out = typeof obj
  let output = {} as Out
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      const element = obj[key];
      if( typeof element === "object") {
        output[key] = transformToString(element as Record<string,unknown>) as Out[Extract<keyof Out, string>]
      } else {
        output[key] = (element as (string|number|boolean)).toString() as Out[Extract<keyof Out, string>]
      }
    }
  }
  return output
}
const getJvm = () => fetchS.baseSubmit(interpret(permachine), {
  action: "exec",
  command: "jvm"
}).then(res => {
  const result = (res as CommonRes).body.results[0]
  if (result.type === "jvm") {
    tableResultList.length = 0
    Object.entries(result.jvmInfo).forEach(([key, value]) => {
      let row = {
        key,
        value:JSON.stringify(value)
      }
      tableResultList.push(row)
    })
  }
})
onBeforeMount(() => {
  getJvm()
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
        <th>{{map.key}}</th>
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