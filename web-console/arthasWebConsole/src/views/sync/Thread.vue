<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, ref, watchEffect } from 'vue';
import CmdResMenu from '@/components/CmdResMenu.vue';
import { fetchStore } from '@/stores/fetch';
import { Listbox, ListboxButton, ListboxLabel, ListboxOptions, ListboxOption, Disclosure, DisclosureButton, DisclosurePanel } from '@headlessui/vue';
import { waitFor } from 'xstate/lib/waitFor';
import { hasOwn } from '@vue/shared';
const fetchS = fetchStore()
const fetchM = useMachine(machine)
const busyfetchM = useMachine(machine)
const concretefetchM = useMachine(machine)
const allMap = new Map<string, string[]>()
const busyMap = new Map<string, string[]>()
let busyN = 3
const allloop = fetchS.getPollingLoop(() => {
  fetchM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: "thread"
    }
  })
})
const busyloop = fetchS.getPollingLoop(() => busyfetchM.send({
  type: "SUBMIT",
  value: {
    action: "exec",
    command: `thread -n ${busyN}`
  }
}))
onBeforeMount(() => {
  fetchM.send("INIT")
  allloop.open()
  busyfetchM.send("INIT")
  busyloop.open()
  concretefetchM.send("INIT")
})
const alllist = reactive([] as string[])
const busylist = reactive([] as string[])

const threadInfo = ref({} as ThreadInfo)
const optionThread = reactive([] as { name: string, id: number }[])
const selectedThread = ref({ name: "threadName", id: -1 })
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
          optionThread.length = 0
          threadStats.forEach((v) => {
            alllist.push(v.name)
            allMap.set(v.name, Object.entries(v).filter(([k, v]) => k !== "name").map(([k, v]) => `${k} : ${v}`))
            optionThread.push({ name: v.name, id: v.id })
          })
        }
      }
    }
  }
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

})
const getConcrtetThread = async () => {
  concretefetchM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: `thread ${selectedThread.value.id}`
    }
  })
  await waitFor(concretefetchM.service, state => state.matches("ready"))
  console.log(concretefetchM.state.value.context.response)
  const res = (concretefetchM.state.value.context.response as CommonRes).body.results[0]
  if (res.type === "thread" && Object.hasOwn(res, "threadInfo")) {
    threadInfo.value = (<{ threadInfo: ThreadInfo }>res).threadInfo
  }

}
const toggleAllLoop = (open: boolean) => {
  console.log("open??", open)
  if (open) {
    allloop.open()
  } else {
    allloop.close()
  }
}
</script>

<template>
  <div class="p-2 overflow-auto flex-1">
    <CmdResMenu title="all thread" :list="alllist" :map="allMap" class="w-full" />
    <CmdResMenu title="busy thread" :list="busylist" :map="busyMap" class="w-full" />
    <Disclosure v-slot="{ open }">
      <DisclosureButton class="py-2 w-80 rounded grid place-content-center bg-blue-300 " @click="toggleAllLoop(open)">
        getThreadInfo
      </DisclosureButton>
      <DisclosurePanel class=" pt-2">
        <div class="flex">
          <div class="p-2">selectThread:</div>
          <Listbox v-model="selectedThread" as="div" class=" relative">
            <ListboxButton class="border bg-blue-200 p-2 rounded">{{ selectedThread.name }}</ListboxButton>
            <ListboxOptions class=" absolute z-10 h-40 bg-white border rounded overflow-y-auto w-80"
              v-slot="{ active, selected }">
              <ListboxOption v-for="thread in optionThread" :key="thread.id" :value="thread"
                :class="{ 'bg-gray-400': active, 'bg-gray-500': selected }" class="border-t">
                {{ thread.name }}
              </ListboxOption>
            </ListboxOptions>
          </Listbox>
        </div>
        <button @click="getConcrtetThread" class="border bg-blue-400 p-2 rounded mb-4">getInfo</button>
        <ul>
          <li class="grid place-content-center mb-4 text-3xl">stackTrace</li>
          <li v-for="stack in threadInfo.stackTrace" class="flex flex-col bg-blue-100 mb-3 pt-2 pl-2">
            <div v-for="kv in Object.entries(stack)" class=" mb-2 flex "><span
                class="w-1/6 bg-blue-300 grid place-content-center rounded mr-2">{{ kv[0] }}</span> <span>{{ kv[1] }}</span>
            </div>
          </li>
        </ul>
      </DisclosurePanel>
    </Disclosure>
  </div>
</template>

<style scoped>
</style>