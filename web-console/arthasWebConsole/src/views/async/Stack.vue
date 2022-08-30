<script setup lang="ts">
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
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
})
const pollResults = reactive([] as ArthasResResult[])
const enhancer = reactive(new Map())
getCommonResEffect(pollingM, body => {
  console.log(body)
  if (body.results.length > 0) {
    body.results.forEach(result => {
      if (result.type === "stack") {
        pollResults.push(result)
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
      command: `stack ${classI.value} ${methI.value}`
    } as AsyncReq
  })
  const state = await waitFor(fetchM, state => state.hasTag("result"))
  console.log(state.value)
  if (state.matches("success")) {
    loop.open()
    fetchS.openJobRun()
  }
  fetchM.stop()
}
</script>

<template>
  <MethodInput :submit-f="submit"></MethodInput>
  <template v-if="pollResults.length > 0 || enhancer.size > 0">
    <CmdResMenu title="enhancer" :map="enhancer" open></CmdResMenu>
    {{  pollResults  }}
  </template>
</template>