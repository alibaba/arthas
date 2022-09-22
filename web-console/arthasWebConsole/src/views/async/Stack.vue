<script setup lang="ts">
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { useMachine, useInterpret } from '@xstate/vue';
import { onBeforeMount, onBeforeUnmount, reactive, ref } from 'vue';
import Enhancer from '@/components/show/Enhancer.vue';
import { publicStore } from '@/stores/public';
const fetchM = useInterpret(permachine)
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const publicS = publicStore()
const { getCommonResEffect } = fetchS
// const {getCommonResEffect} = publicStore()

const loop = fetchS.pullResultsLoop(pollingM)
const tableResults = reactive([] as Map<string, string>[])
const keyList = [
  "ts",
  "cost",
  "daemon",
  "priority",
  "stackTrace",
  "classloader",
  "threadId",
  "threadName",]
// const enhancer = reactive(new Map())
const enhancer = ref(undefined as EnchanceResult | undefined)
getCommonResEffect(pollingM, body => {
  if (body.results.length > 0) {
    body.results.forEach(result => {
      if (result.type === "stack") {
        const map = new Map()
        Object
          .keys(result)
          .filter((k) => !["jobId", "type"].includes(k))
          .forEach(k => {
            let val: string | string[] = ""
            if (k === "stackTrace") {
              let stackTrace = result[k]
              val = stackTrace.map((trace, i) => `${trace.className}.${trace.methodName} (${trace.fileName}: ${trace.lineNumber})`)
            } else {
              val = result[k as Exclude<keyof typeof result, "jobId" | "type" | "stackTrace">].toString()
            }
            map.set(k, val)
          })
        // pollResults.unshift([result.ts, map])
        tableResults.unshift(map)
      }

      if (result.type === "enhancer") {
        enhancer.value = result
      }
    })
  }
})

onBeforeMount(() => {
  // fetchM.send("INIT")
  pollingM.send("INIT")
  fetchS.asyncInit()
  // loop.open()
})
onBeforeUnmount(() => {
  loop.close()
})

const submit = async (data: { classItem: Item, methodItem: Item, conditon: string, count: number }) => {

  let className = data.classItem.value
  let methodName = data.methodItem.value
  let condition = data.conditon.trim() == "" ? "" : `'${data.conditon.trim()}'`
  let n = data.count > 0 ? `-n ${data.count}` : ""

  // pollResults.length = 0
  enhancer.value = undefined
  // tableResults.length = 0
  fetchS.baseSubmit(fetchM, {
    action: "async_exec",
    command: `stack ${className} ${methodName} ${condition} ${n}`,
    sessionId: undefined
  }).then(res => {
    loop.open()
  })
}
</script>

<template>
  <MethodInput :submit-f="submit" ncondition ncount></MethodInput>
  <template v-if="tableResults.length > 0 || enhancer">
    <Enhancer :result="enhancer" v-if="enhancer"></Enhancer>
    <div class="w-full flex justify-center items-center mt-4">
      <table class="border-collapse border border-slate-400 ">
        <thead>
          <tr>
            <th class="border border-slate-300 p-2" v-for="(v,i) in keyList" :key="i">{{v}}</th>
          </tr>
        </thead>
        <tbody class="">
          <tr v-for="(map, i) in tableResults" :key="i">
            <td class="border border-slate-300 p-2" v-for="(key,j) in keyList" :key="j">
              <template v-if="key!== 'stackTrace'">
                {{map.get(key)}}
              </template>
              <div class="flex flex-col items-end" v-else>

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