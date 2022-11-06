<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, onUnmounted, reactive, watchEffect } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import { publicStore } from '@/stores/public';
import { fetchStore } from '@/stores/fetch';
const fetchM = useMachine(machine)
const { getPollingLoop } = fetchStore()
const { getCommonResEffect } = publicStore()

const map = reactive(new Map<string, string[]>())
const loop = getPollingLoop(() => {
  fetchM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: "memory"
    }
  })
})

onBeforeMount(() => {
  fetchM.send("INIT")

  loop.open()
})
onUnmounted(()=>loop.close())
getCommonResEffect(fetchM, body => {
  const result = body.results[0]
  if (result.type === "memory") {
    const memoryInfo = result.memoryInfo
    map.clear()
    Object.entries(memoryInfo).reduce((pre, cur) => {
      cur[1].forEach(v => pre.push(v))
      return pre
    }, [] as any[]).forEach(v => {
      map.set(v.name,
        Object.entries(v).filter(([k, v]) => k !== "name").map((k) => {
          return `${k[0]} : ${typeof k[1] === "number" && k[1] > 0 ? 
        (Math.floor(k[1] / 1024 / 1024) + 'M'):k[1] }`
        })
      )
    })
  }
})
</script>

<template>
  <CmdResMenu title="memory" :map="map" class="w-full" />
</template>

<style scoped>
</style>