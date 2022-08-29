<script setup lang="ts">
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, onBeforeUnmount } from 'vue';
import { waitFor } from 'xstate/lib/waitFor';
const fetchM = useMachine(machine)
const {getPollingLoop, interruptJob, getCommonResEffect} = fetchStore()
// const {getCommonResEffect} = publicStore()

const isReady = async ()=>waitFor(fetchM.service, state=>state.matches("ready"))

getCommonResEffect(fetchM,body=>{
  // body.results.forEach(result=>{
  //   if(result.type === "stack") {
  //     console.log(result)
  //   }
  // })
  console.log(body)
})
onBeforeMount(()=>{
  fetchM.send("INIT")
})
onBeforeUnmount(async ()=>{
  loop.close()
  interruptJob(fetchM)
  await isReady()
  fetchM.send({
    type:"SUBMIT",
    value:{
      action:"interrupt_job"
    } as AsyncReq
  })
  await isReady()
  fetchM.send({
    type:"SUBMIT",
    value:{
      action:"close_session"
    } as SessionReq
  })
})
const loop = getPollingLoop(()=>{
  fetchM.send({
    type:"SUBMIT",
    value:{
      action:"pull_results"
    } as AsyncReq
  })
})
const submit=async (classI: Item,methI: Item)=>{
  loop.close()
  interruptJob(fetchM)
  await isReady()
  fetchM.send({
    type:"SUBMIT",
    value: {
      action:"async_exec",
      command:`stack ${classI.value} ${methI.value}`
    } as AsyncReq
  })
  // await waitFor(fetchM.service, state=>state.matches("success"))
  await isReady()
  loop.open()
}
</script>

<template>
<MethodInput :submit-f="submit"></MethodInput>
</template>

<style scoped>

</style>