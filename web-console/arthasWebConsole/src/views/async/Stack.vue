<script setup lang="ts">
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, onBeforeUnmount } from 'vue';
import { waitFor } from 'xstate/lib/waitFor';
const fetchM = useMachine(machine)
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const {getPollingLoop, interruptJob, getCommonResEffect} = fetchS
// const {getCommonResEffect} = publicStore()

const isReady = async ()=>waitFor(fetchM.service, state=>state.matches("ready"))
const loop = getPollingLoop(()=>{
  fetchM.send({
    type:"SUBMIT",
    value:{
      action:"pull_results"
    } as PullResults
  })
})
getCommonResEffect(pollingM,body=>{
  // body.results.forEach(result=>{
  //   if(result.type === "stack") {
  //     console.log(result)
  //   }
  // })
  console.log(body)
})
onBeforeMount(()=>{
  fetchM.send("INIT")
  pollingM.send("INIT")
  // loop.open()
})
onBeforeUnmount(()=>{
  // fetchM.send({
  //   type:"SUBMIT",
  //   value:{
  //     action:"interrupt_job"
  //   } as AsyncReq
  // })
  loop.close()
  interruptJob(fetchM)
})

const submit=async (classI: Item,methI: Item)=>{
  // interruptJob(fetchM)
  // await isReady()
  fetchM.send({
    type:"SUBMIT",
    value: {
      action:"async_exec",
      command:`stack ${classI.value} ${methI.value}`
    } as AsyncReq
  })
  loop.open()
  // await waitFor(fetchM.service, state=>state.matches("success"))
  // await isReady()
}
</script>

<template>
<MethodInput :submit-f="submit"></MethodInput>
</template>

<style scoped>

</style>