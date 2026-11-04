<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { onBeforeMount, reactive, ref, Ref } from 'vue';
import { interpret } from 'xstate';
const fetchS = fetchStore();
const publicS = publicStore()
type ResultType = { "key": string, "value": Ref<string>, rowspan: number, changeValue:()=>void};
const tableResultList: ResultType[] = reactive([])

const setSysprop = (key:string,value:string) => fetchS.baseSubmit(interpret(permachine),{
  action: "exec",
  command:`sysprop ${key} ${value}`
})

const getSysprop = () => fetchS.baseSubmit(interpret(permachine), {
  action: "exec",
  command: "sysprop",
}).then(res => {
  let result = (res as CommonRes).body.results[0]
  if (result.type == "sysprop") {
    tableResultList.length = 0;
    Object.entries(result.props).forEach(([key, value]) => {
      let rows: ResultType[] = []
      // 用来绑定读写
      let _raw = ref(value)
      const changeValue = publicS.inputDialogFactory(_raw,
        (raw) => {
          setSysprop(key, raw.trim()).then(res=>{
          },err=>{
            // 失败就回退
            _raw.value = value
          })
          return raw.trim()
        },
        (input) => input.value)
        rows.push({
          key, value:_raw, rowspan: 1, changeValue
        })
      tableResultList.push(...rows)
    })
    tableResultList.sort((a, b) => a.key > b.key ? 1 : -1)
  }
})
onBeforeMount(() => {
  getSysprop()
})
</script>

<template>
  <table class="table w-full table-compact">
    <thead>
      <tr>
        
        <th></th>
        <th>key</th>
        <th>value</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(map, i) in tableResultList" :key="i" class="hover">
        <th v-if="map.rowspan > 0" :rowspan="map.rowspan">
          <button class="btn btn-outline btn-xs btn-primary" @click.prevent="map.changeValue">edit</button>
        </th>
        <th v-if="map.rowspan > 0" :rowspan="map.rowspan">{{map.key}}</th>
        <td>{{map.value}}</td>
      </tr>
    </tbody>
    <tfoot>
      <tr>
        <th></th>
        <th>key</th>
        <th>value</th>
      </tr>
    </tfoot>
  </table>
</template>