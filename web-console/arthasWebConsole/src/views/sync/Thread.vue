<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, ref, computed, onUnmounted } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import AutoComplete from "@/components/input/AutoComplete.vue";
import {
  Disclosure, DisclosureButton, DisclosurePanel
} from '@headlessui/vue';
import { waitFor } from 'xstate/lib/waitFor';
import ClassInput from '@/components/input/ClassInput.vue';
type OptionThread = {
  name: string,
  value: number
}
const fetchS = fetchStore()
const { getCommonResEffect } = publicStore()
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
const alllist = reactive([] as string[])
const busylist = reactive([] as string[])

const threadInfo = ref({} as ThreadInfo)
const optionThread = reactive([] as OptionThread[])
onBeforeMount(() => {
  fetchM.service.start()
  fetchM.send("INIT")
  allloop.open()
  busyfetchM.service.start()
  busyfetchM.send("INIT")
  busyloop.open()
  concretefetchM.send("INIT")
})
onUnmounted(() => {
  allloop.close()
  busyloop.close()
})
const allEffect = getCommonResEffect(fetchM, body => {
  const result = body.results[0]
  if (result.type === "thread") {
    if (Object.hasOwn(result, "threadStats")) {
      const { threadStats } = result
      alllist.length = 0
      allMap.clear()
      optionThread.length = 0
      threadStats.forEach((v) => {
        alllist.push(v.name)
        allMap.set(v.name, Object.entries(v).filter(([k, v]) => k !== "name").map(([k, v]) => `${k} : ${v}`))
        optionThread.push({ name: v.name, value: v.id })
      })
    }
  }
})
const busyEffect = getCommonResEffect(busyfetchM, body => {
  const result = body.results[0]
  if (result.type === "thread") {
    if (Object.hasOwn(result, "busyThreads")) {
      const { busyThreads } = result
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
})
getCommonResEffect(concretefetchM, body => {
  const res = body.results[0]
  if (res.type === "thread" && Object.hasOwn(res, "threadInfo")) {
    threadInfo.value = res.threadInfo
  }
})
const getConcrtetThread = (thread: OptionThread) => {
  concretefetchM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: `thread ${thread.value}`
    }
  })
}
const toggleAllLoop = (open: boolean) => {
  if (open) {
    busyloop.open()
    allloop.open()
  } else {
    busyloop.close()
    allloop.close()
  }
}
</script>

<template>
  <CmdResMenu title="all thread" :list="alllist" :map="allMap" class="w-full" />
  <CmdResMenu title="busy thread" :list="busylist" :map="busyMap" class="w-full" />
  <Disclosure v-slot="{ open }">
    <DisclosureButton class="py-2 w-80 rounded grid place-content-center bg-blue-300 " @click="toggleAllLoop(open)">
      getThreadInfo
    </DisclosureButton>
    <DisclosurePanel class="pt-4 border-t-2 mt-4">

      <AutoComplete label="ThreadName :" :submitfn="getConcrtetThread" :option-items="optionThread"></AutoComplete>
      <ul>
        <template v-if="Object.keys(threadInfo).length !== 0">
          <li class="grid place-content-center mb-4 text-3xl">stackTrace</li>
          <li v-for="stack in threadInfo.stackTrace" class="flex flex-col bg-blue-100 mb-3 pt-2 pl-2">
            <div v-for="kv in Object.entries(stack)" class=" mb-2 flex "><span
                class="w-1/6 bg-blue-300 grid place-content-center rounded mr-2">{{ kv[0] }}</span> <span>{{ kv[1]
                }}</span>
            </div>
          </li>
        </template>
      </ul>
    </DisclosurePanel>
  </Disclosure>
</template>

<style scoped>
</style>