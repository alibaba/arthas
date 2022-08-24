<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useMachine } from '@xstate/vue';
import { onBeforeMount } from 'vue';

const gcMachine = useMachine(machine)
onBeforeMount(()=>{
  gcMachine.send("INIT")
})
const forceGc = ()=>{
  gcMachine.send({
    type:"SUBMIT",
    value:{
      action:"exec",
      command:"vmtool --action forceGc"
    }
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