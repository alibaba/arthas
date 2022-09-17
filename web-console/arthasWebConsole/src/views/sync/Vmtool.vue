<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { useInterpret, useMachine } from '@xstate/vue';
import { onBeforeMount } from 'vue';

const gcMachine = useInterpret(machine)
const fetchS = fetchStore()
onBeforeMount(()=>{
  // gcMachine.send("INIT")
})
const forceGc = ()=>{
  fetchS.baseSubmit(gcMachine,{
      action:"exec",
      command:"vmtool --action forceGc"
    })
}
const getInstance = ()=>{
  fetchS.baseSubmit(gcMachine,{
    action:"exec",
    command:"vmtool --action getInstances --className demo.MathGame -x 4"
  })
}
</script>

<template>
<div>
  <button @click.prevent="forceGc" class="bg-blue-500 p-2 hover:bg-blue-300 transition rounded-md">forceGc</button>
</div>
</template>

<style scoped>

</style>