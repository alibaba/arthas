<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret } from '@xstate/vue';
import { nextTick, onBeforeMount, onBeforeUnmount, reactive, ref, watchEffect } from 'vue';
import {
  Listbox,
  ListboxButton,
  ListboxOptions,
  ListboxOption,
} from "@headlessui/vue"
import { waitFor } from 'xstate/lib/waitFor';
import { publicStore } from '@/stores/public';
import TodoList from '@/components/input/TodoList.vue';
import { fetchStore } from '@/stores/fetch';
import machine from '@/machines/consoleMachine';
import { interpret } from 'xstate';
const fetchM = useInterpret(permachine)
const publicS = publicStore()
const fetchS = fetchStore()

let eventList = reactive(["all"] as string[]);
let selectEvent = ref(eventList[0])
let includesVal = reactive(new Set<string>())
let excludesVal = reactive(new Set<string>())
let framebuf = ref(1000000)
let profilerStatus = ref({
  is: false,
  message: ""
})
let outputPath = ref("")
let samples = ref(0)
let mutexFrambuf = false

const getStatusLoop = fetchS.getPollingLoop(() => {
  const statusM = interpret(permachine)
  fetchS.baseSubmit(statusM, {
    command: "profiler status",
    action: "exec",
    sessionId: "",
  }).then(
    res => {
    if (res) {
      let result = (res as CommonRes).body.results[0]
      if (result.type == "profiler") {
        if (result.executeResult.search("not") >= 0) {
          profilerStatus.value.is = false
        } else profilerStatus.value.is = true
        profilerStatus.value.message = result.executeResult
      }
    }
  }
  )
}, {
  step: 2000,
})

const getSampleLoop = fetchS.getPollingLoop(() => {
  let statusM = interpret(permachine)
  fetchS.baseSubmit(statusM, {
    command: "profiler getSamples",
    action: "exec",
    // 置空sessionId,使得不与session冲突
    sessionId: ""
  }).then(
    res => {
      if (res) {
        let result = (res as CommonRes).body.results[0]
        if (result.type == "profiler") {
          samples.value = parseInt(result.executeResult)
        }
      }
    }
  )
}, {
  step: 2000,
})
const changeFramebuf = () => {
  // 先赋值，再打开inputDialog
  // publicS.inputVal = framebuf.value.toString()
  publicS.$patch({
    isInput: true,
    inputVal: framebuf.value.toString()
  })
  // nextTick(()=>publicS.isInput=true)

  console.log(publicS.inputVal, framebuf.value)
  // 先触发effect, 再解锁
  mutexFrambuf = true
}

watchEffect(() => {
  if (publicS.inputVal !== "" && mutexFrambuf) {

    /**
     * 先上锁，防止再次触发该副作用
     */
    mutexFrambuf = false
    let valRaw = parseInt(publicS.inputVal)
    framebuf.value = Number.isNaN(valRaw) ? 1000000 : valRaw
    nextTick(() => publicS.inputVal = "")
  }
})
// let duration = ref(300)

const transformStartProps = () => {
  let start = "start"
  let evenOption = ""
  let includeOption = ""
  let excludeOption = ""
  if (selectEvent.value !== "all") {
    evenOption = "--event " + selectEvent.value
  }
  for (const v of includesVal) {
    includeOption += "--include " + v + " "
  }
  for (const v of excludesVal) {
    excludeOption += "--exclude " + v + " "
  }
  return {
    start,
    evenOption,
    includeOption,
    excludeOption
  }
}
const startSubmit = () => {
  const { start, evenOption, includeOption, excludeOption } = transformStartProps()

  const value: ArthasReq = {
    action: "exec",
    command: `profiler ${start} ${evenOption} ${includeOption} ${excludeOption}`,
    sessionId: undefined
  }

  fetchS.baseSubmit(fetchM, value)
}
const stopProfiler = () => fetchS.baseSubmit(fetchM, {
  action: "exec",
  command: "profiler stop"
}).then(
  res => {
    let result = (res as CommonRes).body.results[0]
    if (result.type === "profiler" && result.outputFile) {
      outputPath.value = result.outputFile
    }
  }
)
const resumeProfiler = () => fetchS.baseSubmit(fetchM, {
  action: "exec",
  command: "profiler resume"
}).then(
  res => console.log(res)
)

onBeforeMount(async () => {
  publicS.inputVal = "",
  includesVal.clear()
  excludesVal.clear()
  getStatusLoop.open()
  getSampleLoop.open()
  
  await fetchS.baseSubmit(fetchM, {
    action: "exec",
    command: "profiler list"
  }).then(
    res => {
    let result = (res as CommonRes).body.results[0]
    if (result.type == "profiler") {
      result.executeResult.split('\n').forEach(raw => {
        let cmd = raw.trim();
        if (!["Basic events:",
          "Java method calls:",
          "Perf events:",
          ""
        ].includes(cmd)) eventList.push(cmd)
      })
    }
  }
  )
}
)
onBeforeUnmount(() => {
  getStatusLoop.close()
  getSampleLoop.close()
})
</script>

<template>
  <div class="flex py-2 border-b-2  border-gray-300">
    <h3 class="text-lg w-40">status: </h3>
    <div class="mx-2">
      <div>{{profilerStatus.message}}</div>
      <div v-if="profilerStatus.is">{{samples}} samples</div>
    </div>

  </div>
  <div class="flex border-b-2  border-gray-300 items-center py-2">
    <h3 class="text-lg w-40">How to start: </h3>
    <Listbox v-model="selectEvent">
      <div class=" relative mx-2">
        <ListboxButton class="border p-2 w-52 rounded-xl hover:shadow-md transition bg-blue-400 hover:opacity-50"> even:
          {{ selectEvent}}
        </ListboxButton>
        <ListboxOptions
          class=" absolute w-52 mt-2 border py-2 rounded-md hover:shadow-xl transition bg-white max-h-80 overflow-y-auto">
          <ListboxOption v-for="(e,i) in eventList" :key="i" :value="e" v-slot="{active, selected}">
            <div class=" p-2 transition break-words" :class="{
            'bg-blue-300 text-white': active,
            'bg-blue-500 text-white': selected,
            'text-gray-900': !active && !selected
            }">
              {{ e }}
            </div>
          </ListboxOption>
        </ListboxOptions>
      </div>
    </Listbox>
    <button class="button-style mr-2" @click="changeFramebuf">framebuf :{{framebuf}}</button>
    <TodoList title="include" :val-set="includesVal" class=" mr-2"></TodoList>
    <TodoList title="exclude" :val-set="excludesVal" class="mr-2"></TodoList>
    <button class="button-style" @click="startSubmit">start</button>
  </div>
  <div class="flex items-center border-b-2 border-gray-300 py-2">
    <h3 class="text-lg w-40">Resume or stop: </h3>
    <button class="button-style mx-2" @click="resumeProfiler">resume</button>
    <button class="button-style" @click="stopProfiler">stop</button>
  </div>
  <div class="flex items-center py-2" v-if="outputPath!==''">
    <h3 class="text-lg w-40">output file path: </h3>
    <div class=" mx-2">{{ outputPath }}</div>
  </div>
</template>

<style scoped>

</style>