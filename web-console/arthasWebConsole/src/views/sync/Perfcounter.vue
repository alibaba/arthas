<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, watchEffect } from 'vue';
import CmdResMenu from '@/components/CmdResMenu.vue';
import ConfigMenu from '@/components/ConfigMenu.vue';
const fetchM = useMachine(machine)

onBeforeMount(() => {
  fetchM.send("INIT")

  fetchM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: "perfcounter"
    }
  })
})

const list = reactive([] as string[])
const map = new Map<string, string[]>()
watchEffect(() => {
  if (fetchM.state.value.context.response) {
    const response = fetchM.state.value.context.response
    if (Object.hasOwn(response, "body")) {
      const result = (response as CommonRes).body.results[0]
      if (result.type === "perfcounter") {
        console.log(result.perfCounters)
        // const memoryInfo = result.memoryInfo
        const perfcounters = result.perfCounters
        list.length = 0
        map.clear()
        // Object.entries(memoryInfo).reduce((pre, cur) => {
        //   cur[1].forEach(v => pre.push(v))
        //   return pre
        // }, [] as any[]).forEach(v => {
        //   list.push(v.name)

        //   map.set(v.name,
        //     Object.entries(v).filter(([k, v]) => k !== "name").map((k) => {
        //       return `${k[0]} : ${k[1]}`
        //     })
        //   )
        // })
        perfcounters.forEach(v=>{
          list.push(v.name)
          map.set(v.name,[v.value.toString()])
        })
      }
    }
  }
  setTimeout(()=>fetchM.send({type:"SUBMIT",value:{action:"exec",command:"perfcounter"}}),5000)
})
</script>

<template>
  <div class="p-2 overflow-auto flex-1">
    <CmdResMenu title="perfcounter" :list="list" :map="map" class="w-full" />
  </div>
</template>

<style scoped>
</style>