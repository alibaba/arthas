<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { onBeforeMount, reactive, ref, Ref } from 'vue';
import { interpret } from 'xstate';
const fetchS = fetchStore();
const publicS = publicStore()
type ResultType = MergeObj<Omit<GlobalOptions, "value">, {
  value: Ref<string>, changeValue: null | (() => void)
}>

const tableResultList: ResultType[] = reactive([])
const keyList: (keyof GlobalOptions)[] = [
  "level",
  "name",
  "type",
  "value",
  "summary",
  "description",
]
const setVmoption = (key: string, value: string) => fetchS.baseSubmit(interpret(permachine), {
  action: "exec",
  command: `options ${key} ${value}`
})

const getVmoption = () => fetchS.baseSubmit(interpret(permachine), {
  action: "exec",
  command: "options",
}).then(res => {
  let result = (res as CommonRes).body.results[0]
  if (result.type == "options") {
    tableResultList.length = 0;
    result.options.forEach((option) => {
      let rows: ResultType[] = []
      // 用来绑定读写

      let _raw = ref(option.value)
      let changeValue = null
      changeValue = publicS.inputDialogFactory(_raw,
        (raw) => {
          setVmoption(option.name, raw.trim())
            .then(res =>{
              let result = (res as CommonRes).body.results[0]
              if(result.type === "options") _raw.value = (result.changeResult.afterValue as string).toString()
            })
            .catch(err => {
              // 失败就回退
              _raw.value = option.value
            })
          return raw.trim()
        },
        (input) => input.value)
      rows.push({
        ...option,
        value: _raw,
        changeValue,
      })
      tableResultList.push(...rows)
    })
    tableResultList.sort((a, b) => {
      if(a.level != b.level) return a.level - b.level
      else {
        return a.name > b.name ? 1 :-1
      }
    })
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
        <th v-for="(k, i) in keyList" :key="i">{{ k }}</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(map, i) in tableResultList" :key="i" class="hover">
        <th>
          <button class="btn btn-outline btn-xs btn-primary" @click.prevent="map.changeValue"
            v-if="map.changeValue !== null">edit</button>
        </th>
        <template v-for="(k, j) in keyList" :key="j">
          <th v-if="k === 'name' || k === 'level'">{{ map[k] }}</th>
          <td v-else class="break-words"> {{ map[k] }}</td>
        </template>
      </tr>
    </tbody>
    <tfoot>
      <tr>
        <th></th>
        <th v-for="(k, i) in keyList" :key="i">{{ k }}</th>
      </tr>
    </tfoot>
  </table>
</template>