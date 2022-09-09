<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, onUnmounted, reactive, watchEffect } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import { publicStore } from '@/stores/public';
import { fetchStore } from '@/stores/fetch';
// import ConfigMenu from '@/components/show/ConfigMenu.vue';
const fetchM = useMachine(machine)
const { getCommonResEffect } = publicStore()
const { getPollingLoop } = fetchStore()
const loop = getPollingLoop(()=>{
  fetchM.send({ type: "SUBMIT", value: { action: "exec", command: "perfcounter -d" } })
},{
  step:1000
})
onBeforeMount(() => {
  fetchM.send("INIT")

  // fetchM.send({
  //   type: "SUBMIT",
  //   value: {
  //     action: "exec",
  //     command: "perfcounter -d"
  //   }
  // })
  loop.open()
})

const map = reactive(new Map<string, string[]>())
// watchEffect(() => {
//   if (fetchM.state.value.context.response) {
//     const response = fetchM.state.value.context.response
//     if (Object.hasOwn(response, "body")) {
//       const result = (response as CommonRes).body.results[0]
//       if (result.type === "perfcounter") {
//         const perfcounters = result.perfCounters
//         map.clear()
//         perfcounters.forEach(v => {
//           map.set(v.name, Object.entries(v).filter(v => v[0] !== "name").map(([key, value]) => `${key} : ${value}`))
//         })
//       }
//     }
//   }
//   setTimeout(() => fetchM.send({ type: "SUBMIT", value: { action: "exec", command: "perfcounter -d" } }), 5000)
// })
onUnmounted(()=>loop.close())
getCommonResEffect(fetchM, body => {
  const result = body.results[0]
  if (result.type === "perfcounter") {
    const perfcounters = result.perfCounters
    map.clear()
    perfcounters.forEach(v => {
      map.set(v.name, Object.entries(v).filter(v => v[0] !== "name").map(([key, value]) => `${key} : ${value}`))
    })
  }
})
</script>

<template>
  <CmdResMenu title="perfcounter" :map="map" class="w-full" />
</template>

<style scoped>
</style>