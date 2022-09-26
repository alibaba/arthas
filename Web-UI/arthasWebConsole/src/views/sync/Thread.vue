<script setup lang="ts">
// import machine from '@/machines/consoleMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, ref, onUnmounted, watchEffect } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import TodoList from '@/components/input/TodoList.vue';
// import AutoComplete from "@/components/input/AutoComplete.vue";
import {
  Switch, SwitchLabel, SwitchGroup,
  Listbox, ListboxButton, ListboxLabel, ListboxOptions, ListboxOption
} from '@headlessui/vue';
import { transfromStore } from "@/stores/resTransform"
import permachine from '@/machines/perRequestMachine';
import { computed } from '@vue/reactivity';

const fetchS = fetchStore()
const FetchService = useInterpret(permachine)
const stackTrace = reactive([] as string[])
const transformS = transfromStore()
const count = ref(0)
const leastTime = ref(200)
const isBlock = ref(false)
const publiC = publicStore()
type thkey = keyof BusyThread
const keyList: thkey[] = [
  "id",
  "name",
  "cpu",
  "daemon",
  "deltaTime",
  "group",
  "interrupted",
  "priority",
  "state",
  "time",

  "inNative",
  "suspended",
  "waitedCount",
  "waitedTime",
  "lockOwnerId",
  "lockedMonitors",
  "lockedSynchronizers",
  "blockedCount",
  "blockedTime",
]
const statsList: (keyof ThreadStats)[] = ["id",
  "name",
  "cpu",
  "daemon",
  "deltaTime",
  "group",
  "interrupted",
  "priority",
  "state",
  "time",]
const infoCount = ref({
  NEW: 0,
  RUNNABLE: 0,
  BLOCKED: 0,
  WAITING: 0,
  TIMED_WAITING: 0,
  TERMINATED: 0
} as ThreadStateCount)
const tableResults = reactive([] as Map<string, string>[])
const tableFilter = computed(() => {
  let res = tableResults
  if (includesVal.size === 0) return res;
  includesVal.forEach((v1) => {
    let [key, vals] = v1.split(":")
    //@ts-ignore
    if (keyList.includes(key)) {
      const raw = vals.split("")
      let incudes: string[] = []
      if (raw[0] === "[" && raw[raw.length - 1] === "]") {
        raw.pop()
        raw.shift()
        incudes = raw.join("").split(',')
      } else {
        incudes = raw.join("").split(',')
      }
      res = res.filter((map) => incudes.includes(map.get(key.trim())!))
    }

  })
  return res
})
const statelist: { name: string, value: ThreadState | "" }[] = [
  { name: "WAITING", value: "WAITING" },
  { name: "RUNNABLE", value: "RUNNABLE" },
  { name: "TIMED_WAITING", value: "TIMED_WAITING" },
  { name: "BLOCKED", value: "BLOCKED" },
  { name: "all", value: "" }
]
const threadState = ref(statelist[4])
const includesVal = reactive(new Set<string>())
onBeforeMount(() => {
})
onUnmounted(() => {
  // allloop.close()
  // busyloop.close()
})
const getThreads = () => {
  let i = leastTime.value > 0 ? "-i " + leastTime.value : ""
  let n = count.value > 0 ? "-n " + count.value : ""
  const b = isBlock.value ? "-b" : ""
  let state = threadState.value.value === "" ? "" : `--state ${threadState.value.value}`
  tableResults.length = 0
  for (const key in infoCount.value) {
    //@ts-ignore
    infoCount.value[key] = 0
  }
  // thread [--all] [-b] [--lockedMonitors] [--lockedSynchronizers] [-i <value>] [--state <value>] [-n <value>] [id]
  fetchS.baseSubmit(FetchService, {
    action: "exec",
    command: `thread --all ${b} ${i}  ${n} ${state}`
  }).then(res => {
    const result = (res as CommonRes).body.results[0]

    // threadInfo.value = result.threadInfo
    if (result.type === "thread") {

      stackTrace.length = 0

      if (n === "") {
        // result.threadStateCount.
        result.threadStats.forEach(thread => {
          const map = new Map()
          // Object.entries(thread).forEach(([k, v]) => map.set(k, v.toString().trim() || "-"))
          for (const key in thread) {
            // if(key === 'id') map.set(key, thread[key as thkey])
            map.set(key, thread[key as keyof ThreadStats].toString().trim() || "-")
          }
          tableResults.unshift(map)

        })

        for (const key in result.threadStateCount) {
          if (Object.hasOwn(result.threadStateCount, key)) {
            //@ts-ignore
            infoCount.value[key] = result.threadStateCount[key];

          }
        }
      } else {
        result.busyThreads.forEach((thread) => {
          // allMap.set(v.name, Object.entries(v).filter(([k, v]) => k !== "name").map(([k, v]) => `${k} : ${v}`))
          // if (v.id > 0) optionThread.push({ name: v.name, value: v.id })
          const map = new Map()
          // Object.entries(thread).forEach(([k, v]) => map.set(k, v.toString().trim() || "-"))
          for (const key in thread) {
            // if(key === 'id') map.set(key, thread[key as thkey])
            map.set(key, thread[key as thkey].toString().trim() || "-")
          }
          tableResults.unshift(map)
        })

      }

      tableResults.sort((m1, m2) => parseFloat(m2.get("cpu")!) - parseFloat(m1.get("cpu")!))
    }
  })
}
const setlimit = publiC.inputDialogFactory(
  count,
  (raw) => {
    let valRaw = parseInt(raw)
    return Number.isNaN(valRaw) ? 3 : valRaw
  },
  (input) => input.value.toString(),
)
const setleast = publiC.inputDialogFactory(
  leastTime,
  (raw) => {
    let valRaw = parseInt(raw)
    return Number.isNaN(valRaw) ? 200 : valRaw
  },
  (input) => input.value.toString(),
)

const getSpecialThreads = (threadid: number = -1) => {
  let threadName = threadid > 0 ? `${threadid}` : ""
  // let i = leastTime.value > 0 ? "-i " + leastTime.value : ""
  // let n = count.value > 0 ? "-n " + count.value : ""
  // const b = isBlock.value ? "-b" : ""
  // let state = threadState.value.value === "" ? "" : `--state ${threadState.value.value}`
  fetchS.baseSubmit(FetchService, {
    action: "exec",
    command: `thread ${threadName}`
  }).then(res => {
    const result = (res as CommonRes).body.results[0]
    if (result.type === "thread") {
      // threadInfo.value = result.threadInfo
      stackTrace.length = 0
      result.threadInfo.stackTrace.forEach(stack => stackTrace.unshift(transformS.transformStackTrace(stack)))
    }
  })
}

// watchEffect(() => {
//   includesVal.forEach((v1) => {
//     let [key, vals] = v1.split(":")
//     const raw = vals.split("")
//     let includes: string[] = []
//     if (raw[0] === "[" && raw[raw.length - 1] === "]") {
//       raw.pop()
//       raw.shift()
//       includes = raw.join("").split(',')
//     } else {
//       includes = raw.join("").split(',')
//     }
//     tableResults.filter((map) => includes.includes(map.get(key.trim())!))
//   })
// })
</script>

<template>
  <div class="flex flex-col h-full">
    <div class="flex justify-end items-center h-[10vh]">

      <TodoList title="filter" :val-set="includesVal" class=" mr-2"></TodoList>
      <SwitchGroup as="div" class="input-btn-style flex ml-2 focus-within:ring ring-blue-500" v-show="count === 0">
        <SwitchLabel>is blocking:</SwitchLabel>
        <Switch v-model="isBlock" :class="isBlock ? 'bg-blue-400' : 'bg-gray-500'"
          class="relative items-center inline-flex h-6 w-12 shrink-0 cursor-pointer rounded-full border-transparent transition-colors ease-in-out focus:outline-none mx-2">
          <span aria-hidden="true" :class="isBlock ? 'translate-x-6' : '-translate-x-1'"
            class="pointer-events-none inline-block h-6 w-6 transform rounded-full bg-white shadow-md shadow-gray-500 ring-0 transition ease-in-out" />
        </Switch>
      </SwitchGroup>
      <button class="input-btn-style ml-2" @click="setleast">sample interval:{{leastTime}}</button>
      <button v-show="!isBlock" class="input-btn-style ml-2" @click="setlimit"> top n threads:{{count}}</button>
      <Listbox v-model="threadState">
        <div class=" relative mx-2 ">
          <ListboxButton class="input-btn-style w-40">state {{ threadState.name }}</ListboxButton>
          <ListboxOptions
            class=" absolute w-40 mt-2 border overflow-hidden rounded-md hover:shadow-xl transition bg-white">
            <ListboxOption v-for="(am,i) in statelist" :key="i" :value="am" v-slot="{active, selected}">
              <div class=" p-2 transition " :class="{
              'bg-blue-300 text-white': active,
              'bg-blue-500 text-white': selected,
              'text-gray-900': !active && !selected
              }">
                {{ am.name }}
              </div>
            </ListboxOption>
          </ListboxOptions>
        </div>
      </Listbox>
      <button class="button-style" @click="getThreads"> get threads</button>
    </div>
    <div class="w-full h-[50vh] input-btn-style my-2 p-4 flex flex-col">
      <div class="flex h-[8vh] flex-wrap flex-auto">
        <div v-for="(v, i) in Object.entries(infoCount)" :key="i" class="mr-2">
          <span class="bg-blue-500 w-44 px-2 rounded-l text-white">
            {{ v[0] }}
          </span>
          <span class="border-gray-300 bg-blue-100 rounded-r flex-1 px-1 border bordergre">
            {{v[1]}}
          </span>
        </div>
      </div>
      <div class="overflow-scroll h-[40vh] w-full">
        <table class="border-collapse border border-slate-400 mx-auto">
          <thead>
            <tr>
              <th class="border border-slate-300 p-2">get stackTrace</th>
              <template v-if="count===0">
                <th class="border border-slate-300 p-2" v-for="(v,i) in keyList" :key="i">{{v}}</th>
              </template>
              <template v-else>
                <th class="border border-slate-300 p-2" v-for="(v,i) in statsList" :key="i">{{v}}</th>
              </template>
            </tr>
          </thead>
          <tbody class="">
            <tr v-for="(map, i) in tableFilter" :key="i">
              <td class="border border-slate-300 p-2"><button class="button-style"
                  @click="getSpecialThreads(parseInt(map.get('id')!))" v-if="map.get('id')!=='-1'">
                  get stackTrace
                </button></td>
              <template v-if="count === 0">
                <td class="border border-slate-300 p-2" v-for="(key,j) in keyList" :key="j">
                  {{map.get(key)}}
                </td>
              </template>
              <template v-else>
                <td class="border border-slate-300 p-2" v-for="(key,j) in statsList" :key="j">
                  {{map.get(key)}}
                </td>
              </template>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div class="input-btn-style flex-auto overflow-auto">
      <h2 class="text-lg">stackTrace</h2>
      <div v-for="(stack, i) in stackTrace" class="mb-2" :key="i">{{stack}} </div>

    </div>
  </div>
</template>

<style scoped>

</style>