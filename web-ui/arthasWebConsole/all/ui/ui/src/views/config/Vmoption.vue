<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { onBeforeMount, reactive, ref, Ref } from 'vue';
import { interpret } from 'xstate';
const fetchS = fetchStore();
const publicS = publicStore()
type ResultType = { name: string, origin: string, value: Ref<string>, writeable: string, changeValue: null|(() => void)
};
const tableResultList: ResultType[] = reactive([])
const keyList: (keyof VmOption)[] = [
  "name",
  "origin",
  "writeable",
  "value",
]
const setVmoption = (key: string, value: string) => fetchS.baseSubmit(interpret(permachine), {
  action: "exec",
  command: `vmoption ${key} ${value}`
})

const getVmoption = () => fetchS.baseSubmit(interpret(permachine), {
  action: "exec",
  command: "vmoption",
}).then(res => {
  let result = (res as CommonRes).body.results[0]
  if (result.type == "vmoption") {
    tableResultList.length = 0;
    result.vmOptions.forEach((vmoption) => {
      let rows: ResultType[] = []
      // 用来绑定读写
      
      let _raw = ref(vmoption.value)
      let changeValue = null
      if(vmoption.writeable===true) {
        changeValue = publicS.inputDialogFactory(_raw,
        (raw) => {
          setVmoption(vmoption.name, raw.trim()).then(res => {
          }, err => {
            // 失败就回退
            _raw.value = vmoption.value
          })
          return raw.trim()
        },
        (input) => input.value)
      }
      rows.push({
        name: vmoption.name,
        value: _raw,
        changeValue,
        writeable: vmoption.writeable.toString(),
        origin: vmoption.origin
      })
      tableResultList.push(...rows)
    })
    tableResultList.sort((a, b) => a.name > b.name ? 1 : -1)
  }
})
onBeforeMount(() => {
  getVmoption()
})
</script>

<template>
  <table class="table w-full table-compact">
    <thead>
      <tr>

        <th></th>
        <th v-for="(k, i) in keyList" :key="i">{{k}}</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(map, i) in tableResultList" :key="i" class="hover">
        <th >
          <button class="btn btn-outline btn-xs btn-primary" @click.prevent="map.changeValue" v-if="map.changeValue !== null">edit</button>
        </th>
        <template v-for="(k,j) in keyList" :key="j">
          <th v-if="k==='name'">{{map[k]}}</th>
          <td v-else class="break-words"> {{map[k]}}</td>
        </template>
      </tr>
    </tbody>
    <tfoot>
      <tr>
        <th></th>
        <th v-for="(k, i) in keyList" :key="i">{{k}}</th>
      </tr>
    </tfoot>
  </table>
</template>