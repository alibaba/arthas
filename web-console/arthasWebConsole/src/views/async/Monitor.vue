<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import { waitFor } from 'xstate/lib/waitFor';
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { onBeforeMount, onBeforeUnmount, reactive, ref } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import Enhancer from '@/components/show/Enhancer.vue';
import { publicStore } from '@/stores/public';
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const { pullResultsLoop, getCommonResEffect } = fetchS
const fetchM = useInterpret(permachine)
const loop = pullResultsLoop(pollingM)
const pollResults = reactive([] as [string, Map<string, string[]>][])
const enhancer = ref(undefined as undefined | EnchanceResult)
const cycleV = ref(120)
const publicS = publicStore()
const keyList: string[] = [

]
const tableResults = reactive([] as Map<string, string[] | string>[])
/**
 * 打算引入动态的堆叠图，但是不知道timestamp还有cost 应该是rt，估计得找后端去补这个接口
 */
getCommonResEffect(pollingM, body => {
  if (body.results.length > 0) {
    body.results.forEach(result => {
      if (result.type === "monitor") {
        result.monitorDataList.forEach(data => {
          const map = new Map()
          Object
            .entries(data)
            .filter(([k, _]) => !["className", "methodName"].includes(k))
            .forEach(([k, v]) => {
              let val: string[] = []
              if (k === "cost") {
                val.push((v as number).toFixed(2) + 'ms')
              } else val.push(v.toString())
              map.set(k, val)
            })
          pollResults.unshift([pollResults.length.toString(), map])
        })
      }
      if (result.type === "enhancer") {
        console.log("asdfasdf")
        enhancer.value = result
      }
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
  // enhancer.value = undefined
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
      <button class="input-button-style">cycle time:{{cycleV}}</button>
    </template>
  </MethodInput>
  <template v-if="pollResults.length > 0 || enhancer">
    <Enhancer :result="enhancer" v-if="enhancer"></Enhancer>
    <!-- <ul class=" pointer-events-auto mt-10">
      <template v-for="(result, i) in pollResults" :key="i">
        <CmdResMenu :title="result[0]" :map="result[1]" open></CmdResMenu>
      </template>
    </ul> -->
    <div class="w-full flex justify-center items-center ">
      <table class="border-collapse border border-slate-400 table-fixed">
        <thead>
          <tr>
            <th class="border border-slate-300 p-1" v-for="(v,i) in keyList" :key="i">{{v}}</th>
          </tr>
        </thead>
        <tbody class="">
          <tr v-for="(map, i) in tableResults" :key="i">
            <td class="border border-slate-300 p-1" v-for="(key,j) in keyList" :key="j">
              <template v-if=" key !== 'params'">
                {{map.get(key)}}
              </template>

              <div class="flex flex-col" v-else>
                <div v-for="(row, k) in map.get(key)" :key="k">
                  {{row}}
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </template>
</template>