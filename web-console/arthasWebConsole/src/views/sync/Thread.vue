<script setup lang="ts">
import { CheckIcon, SelectorIcon } from "@heroicons/vue/outline"
import machine from '@/machines/consoleMachine';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, ref, computed } from 'vue';
import CmdResMenu from '@/components/CmdResMenu.vue';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import {
  Listbox, ListboxButton, ListboxOptions, ListboxOption,
  Disclosure, DisclosureButton, DisclosurePanel,
  Combobox, ComboboxButton, ComboboxInput, ComboboxOptions, ComboboxOption, ComboboxLabel
} from '@headlessui/vue';
import { waitFor } from 'xstate/lib/waitFor';
type OptionThread = {
  name: string,
  id: number
}
type Item = OptionThread
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
const selectedThread = ref({ name: "", id: -1 } as OptionThread)
const queryThread = ref('')
const filterThreads = computed(() => queryThread.value === '' ? optionThread : optionThread.filter(thread => thread.name.toLocaleLowerCase().includes(queryThread.value.toLocaleLowerCase())))

onBeforeMount(() => {
  fetchM.service.start()
  fetchM.send("INIT")
  allloop.open()
  busyfetchM.service.start()
  busyfetchM.send("INIT")
  busyloop.open()
  concretefetchM.send("INIT")
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
        optionThread.push({ name: v.name, id: v.id })
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
const getConcrtetThread = async () => {
  concretefetchM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: `thread ${selectedThread.value.id}`
    }
  })
  await waitFor(concretefetchM.service, state => state.matches("ready"))
  const res = (concretefetchM.state.value.context.response as CommonRes).body.results[0]
  if (res.type === "thread" && Object.hasOwn(res, "threadInfo")) {
    threadInfo.value = res.threadInfo
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
      <DisclosurePanel class="pt-4 border-t-2 mt-4">
        <div class="flex items-center">
          <Combobox v-model="selectedThread">
            <ComboboxLabel class="p-2">selectThread:</ComboboxLabel>
            <div class="relative flex-1">
              <div
                class="relative w-full cursor-default overflow-hidden rounded-lg bg-white text-left border focus:outline-none hover:shadow-md transition">
                <ComboboxInput class="w-full border-none py-2 pl-3 pr-10 leading-5 text-gray-900 "
                  :displayValue="(item) => (item as OptionThread).name" @change="queryThread = $event.target.value" />
                <ComboboxButton class="absolute inset-y-0 right-0 flex items-center pr-2">
                  <SelectorIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
                </ComboboxButton>
              </div>
              <ComboboxOptions
                class="absolute mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none">
                <div v-if="filterThreads.length === 0 && queryThread !== ''"
                  class="relative cursor-default select-none py-2 px-4 text-gray-700">
                  Nothing found.
                </div>

                <ComboboxOption v-for="thread in filterThreads" as="template" :key="thread.id" :value="thread"
                  v-slot="{ selected, active }">
                  <li class="relative cursor-default select-none p-2" :class="{
                    'bg-blue-400 text-white': active,
                    'bg-blue-600 text-white': selected,
                    'text-gray-900': !active && !selected,
                  }">
                    <span class="block truncate"
                      :class="{ 'font-medium': selected, 'font-normal': !selected, 'text-white': active, 'text-teal-600': !active && !selected }">
                      {{ thread.name }}
                    </span>
                  </li>
                </ComboboxOption>
              </ComboboxOptions>
            </div>
          </Combobox>
          <button @click="getConcrtetThread" class="border bg-blue-400 p-2 rounded-md mx-2 ">getInfo</button>
        </div>
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
  </div>
</template>

<style scoped>
</style>