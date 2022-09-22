<script setup lang="ts">
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { useMachine, useInterpret } from '@xstate/vue';
import { onBeforeMount, onBeforeUnmount, reactive, ref } from 'vue';
import { waitFor } from 'xstate/lib/waitFor';
import Enhancer from '@/components/show/Enhancer.vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import { publicStore } from '@/stores/public';
const fetchM = useInterpret(permachine)
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const publicS = publicStore()
const { getPollingLoop, interruptJob, getCommonResEffect } = fetchS
// const {getCommonResEffect} = publicStore()

const loop = getPollingLoop(() => {
  pollingM.send({
    type: "SUBMIT",
    value: {
      action: "pull_results",
      sessionId: undefined,
      consumerId: undefined
    }
  })
}, {
  globalIntrupt: true
})

const pollResults = reactive([] as [string, Map<string, string[]>][])
// const enhancer = reactive(new Map())
const enhancer = ref(undefined as EnchanceResult | undefined)
getCommonResEffect(pollingM, body => {
  if (body.results.length > 0) {
    body.results.forEach(result => {
      if (result.type === "stack") {
        const map = new Map()
        Object
          .keys(result)
          .filter((k) => !["jobId", "type", "ts"].includes(k))
          .forEach(k => {
            let val: string[] = []
            if (k === "stackTrace") {
              let stackTrace = result[k]
              val = stackTrace.map((trace, i) => `${trace.className}::${trace.methodName}`)
            } else {
              val.push(result[k as Exclude<keyof typeof result, "jobId" | "type" | "stackTrace" | "ts">].toString())
            }
            map.set(k, val)
          })
        pollResults.unshift([result.ts, map])
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

  pollResults.length = 0
  enhancer.value = undefined

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
  <template v-if="pollResults.length > 0 || enhancer">
    <Enhancer :result="enhancer" v-if="enhancer"></Enhancer>
    <ul class=" pointer-events-auto mt-10">
      <template v-for="(result, i) in pollResults" :key="i">
        <CmdResMenu :title="result[0]" :map="result[1]" open></CmdResMenu>
      </template>
    </ul>
  </template>
</template>