<script setup lang="ts">
// import machine from '@/machines/consoleMachine';
import { useInterpret, } from '@xstate/vue';
import { onBeforeMount, reactive, ref, computed } from 'vue';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import TodoList from '@/components/input/TodoList.vue';
import {
  Listbox, ListboxButton, ListboxOptions, ListboxOption
} from '@headlessui/vue';
import permachine from '@/machines/perRequestMachine';
import transformStackTrace from '@/utils/transform';

const fetchS = fetchStore()
const FetchService = useInterpret(permachine)
const stackTrace = reactive([] as string[])
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
const statsList: (keyof ThreadStats)[] = [
  "id",
  "name",
  "cpu",
  "daemon",
  "deltaTime",
  "group",
  "interrupted",
  "priority",
  "state",
  "time"]
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
  // 原本的数组
  let res = count.value > 0 ? tableResults.filter((v, i) => i < count.value) : tableResults
  if (includesVal.size === 0) return res;
  // 导入过滤条件
  includesVal.forEach((v1) => {
    let [key, vals] = v1.split(":")
    //@ts-ignore
    if (keyList.includes(key)) {
      const raw = vals.split("")
      let incudes: string[] = []
      if (raw[0] === "[" && raw[raw.length - 1] === "]") {
        raw.pop()
        raw.shift()
      }
      incudes = raw.join("").split(',')

      if (key === "name" || key === "group") {
        // 字符串是包含
        res = res.filter((map) => {
          let val = map.get(key.trim())!
          // 取对于多个子字符串， 取交集
          return incudes.every(reg => val.includes(reg))
        })
      } else res = res.filter((map) => incudes.includes(map.get(key.trim())!))
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
  getThreads()
})
const getThreads = () => {
  let i = leastTime.value > 0 ? "-i " + leastTime.value : ""
  // 在block时用这个参数会暴毙
  let n = count.value > 0 && !isBlock ? "-n " + count.value : ""
  const b = isBlock.value ? "-b --lockedMonitors  --lockedSynchronizers" : ""
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

    if (result.type === "thread") {

      stackTrace.length = 0

      if (n === "") {
        result.threadStats.forEach(thread => {
          const map = new Map()
          for (const key in thread) {
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
          const map = new Map()
          for (const key in thread) {
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
const { increase, decrease } = publiC.numberCondition(count, { min: 0 })
const getSpecialThreads = (threadid: number = -1) => {
  let threadName = threadid > 0 ? `${threadid}` : ""
  fetchS.baseSubmit(FetchService, {
    action: "exec",
    command: `thread ${threadName} `
  }).then(res => {
    const result = (res as CommonRes).body.results[0]
    if (result.type === "thread") {
      stackTrace.length = 0
      result.threadInfo.stackTrace.forEach(stack => stackTrace.unshift(transformStackTrace(stack)))
    }
  })
}

</script>

<template>
  <div class="flex flex-col h-full">
    <div class="flex justify-end items-center h-[10vh]">

      <TodoList title="filter" :val-set="includesVal" class=" mr-2"></TodoList>
      <button class="btn ml-2 btn-sm btn-outline" @click="setleast">sample interval:{{ leastTime }}</button>
      <div class="btn-group ml-2" v-show="!isBlock">
        <button class="btn btn-outline btn-sm" @click.prevent="decrease">-</button>
        <button class="btn btn-outline btn-sm border-x-0" @click.prevent="setlimit">top threads:{{ count }}</button>
        <button class="btn btn-outline btn-sm" @click.prevent="increase">+</button>
      </div>
      <Listbox v-model="threadState">
        <div class=" relative mx-2 ">
          <ListboxButton class="btn w-40 btn-sm btn-outline">state {{ threadState.name }}</ListboxButton>
          <ListboxOptions
            class=" z-10 absolute w-40 mt-2 border overflow-hidden rounded-md hover:shadow-xl transition bg-base-100">
            <ListboxOption v-for="(am, i) in statelist" :key="i" :value="am" v-slot="{ active, selected }">
              <div class=" p-2 transition " :class="{
                'bg-neutral text-neutral-content': active,
                'bg-neutral-focus text-neutral-content': selected,
              }">
                {{ am.name }}
              </div>
            </ListboxOption>
          </ListboxOptions>
        </div>
      </Listbox>
      <label class="label cursor-pointer btn-sm ">
        <span class="label-text uppercase font-bold mr-1">is blocking:</span>
        <input v-model="isBlock" type="checkbox" class="toggle" />
      </label>
      <button class="btn btn-primary btn-sm btn-outline" @click="getThreads"> get threads </button>
    </div>
    <div class="w-full h-[50vh] my-2 card rounded-box compact border">
      <div class="card-body overflow-auto">
        <div class="h-[8vh] flex-wrap flex-auto flex-row flex">
          <div v-for="(v, i) in Object.entries(infoCount)" :key="i" class="mr-2">
            <span class="text-primary-content border border-primary-focus bg-primary-focus w-44 px-2 rounded-l">
              {{ v[0] }}
            </span>
            <span class="border border-primary-focus bg-base-200 rounded-r flex-1 px-1">
              {{ v[1] }}
            </span>
          </div>
        </div>
        <div class="overflow-auto h-[40vh] w-full">
          <table class="table w-full">
            <thead>
              <tr>
                <th class=""></th>
                <template v-if="count === 0">
                  <th class="normal-case" v-for="(v, i) in keyList" :key="i">{{ v }}</th>
                </template>
                <template v-else>
                  <th class="normal-case" v-for="(v, i) in statsList" :key="i">{{ v }}</th>
                </template>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(map, i) in tableFilter" :key="i">
                <th class="2"><button class="btn-outline btn-primary btn btn-sm"
                    @click="getSpecialThreads(parseInt(map.get('id')!))" v-if="map.get('id') !== '-1'">
                    get stackTrace
                  </button></th>
                <template v-if="count === 0">
                  <td class="" v-for="(key, j) in keyList" :key="j">
                    {{ map.get(key) }}
                  </td>
                </template>
                <template v-else>
                  <td class="" v-for="(key, j) in statsList" :key="j">
                    {{ map.get(key) }}
                  </td>
                </template>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    <div class=" flex-auto card rounded-box compact border">
      <div class="card-body overflow-auto ">
        <h2 class="text-lg">stackTrace</h2>
        <div v-for="(stack, i) in stackTrace" class="mb-2" :key="i">{{ stack }} </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>