<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import {
  ListboxOption,
  Listbox,
  ListboxButton,
  ListboxOptions
} from "@headlessui/vue"
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { onBeforeMount, onBeforeUnmount, reactive, ref } from 'vue';
import Enhancer from '@/components/show/Enhancer.vue';
import { publicStore } from '@/stores/public';
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const { pullResultsLoop, getCommonResEffect } = fetchS
const fetchM = useInterpret(permachine)
const loop = pullResultsLoop(pollingM)
const enhancer = ref(undefined as undefined | EnchanceResult)
const cycleV = ref(120)
const publicS = publicStore()
type KMD = | keyof MonitorData
const keyList: string[] = [
  "className",
  "methodName",
  "cost",
  "success",
  "failed",
  "fail-rate",
  "total",
]
const modelist: { name: string, value: string }[] = [
  { name: "调用开始之前", value: "-b" },
  { name: "调用结束之后", value: "" }
]
const mode = ref(modelist[1])

const tableResults = reactive([] as Map<string, string[] | string>[])
/**
 * 打算引入动态的堆叠图，但是不知道timestamp还有cost 应该是rt，估计得找后端去补这个接口
 */
const transform = (result: ArthasResResult) => {
  if (result.type === "monitor") {
    result.monitorDataList.forEach(data => {
      const map = new Map<string, string>()
      for (const key in data) {
        let val = ""
        if (key === "cost") val = data.cost.toFixed(6).toString() + "ms"
        else val += data[key as KMD]
        map.set(key as KMD, val)
      }
      map.set("fail-rate", (data.failed * 100 / data.total).toFixed(2) + "%")
      tableResults.unshift(map)
    })
  }
  if (result.type === "enhancer") {
    enhancer.value = result
  }
}
getCommonResEffect(pollingM, body => {
  if (body.results.length > 0) {
    body.results.forEach(result => {
      transform(result)
    })
  }
})

const changeCycle = publicS.inputDialogFactory(
  cycleV,
  (raw) => {
    let valRaw = parseInt(raw)
    return Number.isNaN(valRaw) ? 120 : valRaw
  },
  (input) => input.value.toString()
)
onBeforeMount(() => {
  fetchS.asyncInit()
  pollingM.send("INIT")
})
onBeforeUnmount(() => {
  loop.close()
})
const submit = async (data: { classItem: Item, methodItem: Item, conditon: string }) => {
  enhancer.value = undefined
  tableResults.length = 0
  let condition = data.conditon.trim() == "" ? "" : `'${data.conditon.trim()}'`
  let cycle = `-c ${cycleV.value}`
  fetchS.baseSubmit(fetchM, {
    action: "async_exec",
    command: `monitor -c 5 ${data.classItem.value} ${data.methodItem.value} ${condition}`,
    sessionId: undefined
  }).then(
    res => loop.open()
  )
}
</script>

<template>
  <MethodInput :submit-f="submit" class="mb-4" ncondition>
    <template #others>
      <Listbox v-model="mode">
        <div class=" relative mx-2 ">
          <ListboxButton class="input-btn-style w-40">{{ mode.name }}</ListboxButton>
          <ListboxOptions class="absolute w-40 mt-2 border overflow-hidden rounded-md hover:shadow-xl transition bg-white">
            <ListboxOption v-for="(am,i) in modelist" :key="i" :value="am" v-slot="{active, selected}">
              <div class=" p-2 transition " :class="{
              'bg-blue-300 text-white': active,
              'bg-blue-500 text-white': selected,
              'text-gray-900': !active && !selected
              }">
                {{ am.name }}
              </div>
            </ListboxOption>
          </ListboxOptions>
        </div>
      </Listbox>
      <button class="input-btn-style ml-2" @click="changeCycle">cycle time:{{cycleV}}</button>
    </template>
  </MethodInput>
  <template v-if="tableResults.length > 0 || enhancer">
    <Enhancer :result="enhancer" v-if="enhancer"></Enhancer>
    <div class="w-full flex justify-center items-center mt-4">
      <table class="border-collapse border border-slate-400 table-fixed">
        <thead>
          <tr>
            <th class="border border-slate-300 p-2" v-for="(v,i) in keyList" :key="i">{{v}}</th>
          </tr>
        </thead>
        <tbody class="">
          <tr v-for="(map, i) in tableResults" :key="i">
            <td class="border border-slate-300 p-2" v-for="(key,j) in keyList" :key="j">
              {{map.get(key)}}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </template>
</template>