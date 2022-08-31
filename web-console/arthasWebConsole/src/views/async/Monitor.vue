<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import { waitFor } from 'xstate/lib/waitFor';
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { onBeforeMount, onBeforeUnmount } from 'vue';

const pollingM = useMachine(machine)
const fetchS = fetchStore()
const { getPollingLoop, interruptJob, getCommonResEffect } = fetchS
const fetchM = useInterpret(permachine)
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

getCommonResEffect(pollingM, body => {
  console.log(body.results)
})
onBeforeMount(()=>{
  pollingM.send("INIT")
})
onBeforeUnmount(()=>{
  loop.close()
})
const submit = async (classI: Item, methI: Item) => {
  fetchM.start()
  fetchM.send("INIT")
  fetchM.send({
    type: "SUBMIT",
    value: {
      action: "async_exec",
      command: `monitor -c 5 ${classI.value} ${methI.value}`
    } as AsyncReq
  })
  const state = await waitFor(fetchM, state => state.hasTag("result"))

  console.log(state.value)
  if (state.matches("success")) {
    console.log("????")
    loop.open()
  }
  fetchM.stop()
}
</script>

<template>
  <MethodInput :submit-f="submit"></MethodInput>
</template>