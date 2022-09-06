<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import { waitFor } from 'xstate/lib/waitFor';
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { onBeforeMount, onBeforeUnmount, reactive } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const { getPollingLoop, pullResultsLoop, interruptJob, getCommonResEffect,getPullResultsEffect } = fetchS
const fetchM = useInterpret(permachine)
const loop = pullResultsLoop(pollingM)
const pollResults = reactive([] as [string, Map<string, string[]>][])
const enhancer = reactive(new Map())


getPullResultsEffect(
  pollingM,
  enhancer, result=>{
    if(result.type === "tt"){
      result.timeFragmentList
    }
})
onBeforeMount(() => {
  pollingM.send("INIT")
})
onBeforeUnmount(() => {
  loop.close()
})
const submit = async (classI: Item, methI: Item) => {
  fetchM.start()
  fetchM.send("INIT")
  fetchM.send({
    type: "SUBMIT",
    value: {
      action: "async_exec",
      command: `tt -t ${classI.value} ${methI.value}`
    } as AsyncReq
  })
  const state = await waitFor(fetchM, state => state.hasTag("result"))

  if (state.matches("success")) {
    pollResults.length = 0
    enhancer.clear()
    loop.open()
  }
  fetchM.stop()
}
</script>

<template>
  <MethodInput :submit-f="submit"></MethodInput>
</template>

<style scoped>
</style>