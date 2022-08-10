<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, watchEffect } from 'vue';
// import { Disclosure, DisclosureButton, DisclosurePanel } from '@headlessui/vue';
// import ConfigMenu from '@/components/ConfigMenu.vue';
import CmdResMenu from '@/components/CmdResMenu.vue';
// import { kMaxLength } from 'buffer';
const fetchM = useMachine(machine)
const busyfetchM = useMachine(machine)
const allMap = new Map<string, string[]>()
const busyMap = new Map<string, string[]>()
let busyN = 3
onBeforeMount(() => {
  fetchM.send("INIT")
  fetchM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: "thread"
    }
  })
  busyfetchM.send("INIT")
  busyfetchM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: `thread -n ${busyN}`
    }
  })
})
const alllist = reactive([] as string[])
const busylist = reactive([] as string[])
const allEffect = watchEffect(() => {
  if (fetchM.state.value.context.response) {
    const response = fetchM.state.value.context.response
    if (Object.hasOwn(response, "body")) {
      const result = (response as CommonRes).body.results[0]
      if (result.type === "thread") {
        if (Object.hasOwn(result, "threadStats")) {
          const { threadStats } = <{ threadStats: ThreadStats[] }>result
          alllist.length = 0
          allMap.clear()
          threadStats.forEach((v) => {
            alllist.push(v.name)
            allMap.set(v.name, Object.entries(v).filter(([k, v]) => k !== "name").map(([k, v]) => `${k} : ${v}`))
          })
        } else {
          const { busyThreads } = <{ busyThreads: BusyThread[] }>result

          busylist.length = 0
          busyMap.clear()
          busyThreads.forEach(v => {
            busylist.push(v.name)
            busyMap.set(v.name, Object.entries(v).filter(([k, v]) => k !== "name").map(([k, v]) => `${k} : ${v}`))
          })
        }
      }
    }

  }
  setTimeout(() => {
    fetchM.send({
      type: "SUBMIT",
      value: {
        action: "exec",
        command: "thread"
      }
    })
  }, 10000)
})
const busyEffect = watchEffect(() => {
  if (busyfetchM.state.value.context.response) {
    const response = busyfetchM.state.value.context.response
    if (Object.hasOwn(response, "body")) {
      const result = (response as CommonRes).body.results[0]
      if (result.type === "thread") {
        if (Object.hasOwn(result, "busyThreads")) {
          const { busyThreads } = <{ busyThreads: BusyThread[] }>result
          busylist.length = 0
          busyMap.clear()
          busyThreads.forEach(v => {
            busylist.push(v.name)
            busyMap.set(v.name, Object.entries(v).filter(([k, v]) => k !== "name").map(([k, v]) => {
              if (["lockInfo", "lockedMonitors", "stackTrace", "lockedMonitors",
                "lockedSynchronizers"].includes(k)) {
                v = JSON.stringify(v)
              }
              return `${k} : ${v}`
            }))
          })
        }
      }
    }

  }

  setTimeout(() => busyfetchM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: `thread -n ${busyN}`
    }
  }), 10000)
})
</script>

<template>
  <div class="p-2 overflow-auto flex-1">
    <CmdResMenu title="all thread" :list="alllist" :map="allMap" class="w-full" />
    <CmdResMenu title="busy thread" :list="busylist" :map="busyMap" class="w-full" />
  </div>
</template>

<style scoped>
</style>