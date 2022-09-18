<script setup lang="ts">
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { useMachine, useInterpret } from '@xstate/vue';
import { onBeforeMount, onBeforeUnmount, reactive } from 'vue';
import { waitFor } from 'xstate/lib/waitFor';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
const fetchM = useInterpret(permachine)
const pollingM = useMachine(machine)
const fetchS = fetchStore()
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
const pollResults = reactive([] as [string,Map<string, string[]>][])
const enhancer = reactive(new Map())
getCommonResEffect(pollingM, body => {
  if (body.results.length > 0) {
    body.results.forEach(result => {
      if (result.type === "stack") {
        const map = new Map()
        Object
          .keys(result)
          .filter((k) => !["jobId", "type","ts"].includes(k))
          .forEach(k => {
            let val:string[] = []
            if (k === "stackTrace") {
              let stackTrace = result[k]
              val = stackTrace.map((trace,i) => `${trace.className}::${trace.methodName}`)
            } else{
              val.push(result[k as Exclude<keyof typeof result, "jobId"|"type"|"stackTrace"|"ts">].toString())
            }
            map.set(k,val)
          })
        pollResults.unshift([result.ts,map])
      }

      if (result.type === "enhancer") {
        enhancer.clear()
        enhancer.set("success", [result.success])
        for (const k in result.effect) {
          enhancer.set(k, [result.effect[k as "cost"]])
        }
      }
    })
  }
})


onBeforeMount(() => {
  // fetchM.send("INIT")
  pollingM.send("INIT")
  // loop.open()
})
onBeforeUnmount(() => {
  loop.close()
})

const submit = async (classI: Item, methI: Item) => {
  pollResults.length = 0
  enhancer.clear()
  fetchM.start()
  fetchM.send("INIT")
  fetchM.send({
    type: "SUBMIT",
    value: {
      action: "async_exec",
      command: `stack ${classI.value} ${methI.value}`,
      sessionId: undefined
    }
  })
  const state = await waitFor(fetchM, state => state.hasTag("result"))
  
  console.log(state.value)
  if (state.matches("success")) {
    loop.open()
  }
  fetchM.stop()
}
</script>

<template>
  <MethodInput :submit-f="submit"></MethodInput>
  <template v-if="pollResults.length > 0 || enhancer.size > 0">
    <CmdResMenu title="enhancer" :map="enhancer" open></CmdResMenu>
    <ul class=" pointer-events-auto mt-10">
      <template v-for="(result, i) in pollResults" :key="i">
        <CmdResMenu :title="result[0]" :map="result[1]" open></CmdResMenu>
      </template>
    </ul>
  </template>
</template>