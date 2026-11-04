<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret } from '@xstate/vue';
import { nextTick, onBeforeMount, onBeforeUnmount, reactive, Ref, ref, watchEffect } from 'vue';
import {
  Listbox,
  ListboxButton,
  ListboxOptions,
  ListboxOption,
} from "@headlessui/vue"
import { publicStore } from '@/stores/public';
import TodoList from '@/components/input/TodoList.vue';
import { fetchStore } from '@/stores/fetch';
import { interpret } from 'xstate';
const fetchM = useInterpret(permachine)
const publicS = publicStore()
const fetchS = fetchStore()
let eventList = reactive([] as string[]);
let selectEvent = ref("cpu")
let includesVal = reactive(new Set<string>())
let excludesVal = reactive(new Set<string>())
let framebuf = ref(1_000_000)
let duration = ref(300)
let profilerStatus = ref({
  is: false,
  message: ""
})
let outputPath = ref("")
let samples = ref(0)
const support = ref(false)
let fileformat = ref("%t-%p.html")

const getStatusLoop = fetchS.getPollingLoop(() => {
  const statusM = interpret(permachine)
  fetchS.baseSubmit(statusM, {
    command: "profiler status",
    action: "exec",
    sessionId: "",
  }).then(
    res => {
      support.value = true
      if (res) {
        let result = (res as CommonRes).body.results[0]
        if (result.type == "profiler") {
          if (result.executeResult.search("not") >= 0) {
            profilerStatus.value.is = false
          } else profilerStatus.value.is = true
          profilerStatus.value.message = result.executeResult
        }
      }
    },
    reject => {
      getStatusLoop.close()
    }
  )
}, {
  step: 2000,
})
const handleFramebuf = publicS.numberCondition(framebuf, {})
const handleduration = publicS.numberCondition(duration, {})
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
const changeFramebuf = publicS.inputDialogFactory(
  framebuf,
  (raw) => {
    let valRaw = parseInt(raw)
    return Number.isNaN(valRaw) ? 1000000 : valRaw
  },
  (input) => input.value.toString()
)
const changeFile = publicS.inputDialogFactory(fileformat,
  (raw) => raw.trim(),
  (input) => input.value)
const changeDuration = publicS.inputDialogFactory(
  duration,
  (raw) => {
    let valRaw = parseInt(raw)
    return Number.isNaN(valRaw) ? 300 : valRaw
  },
  (input) => input.value.toString()
)

const restartInit = () => {
  profilerStatus.value.is = true
  outputPath.value = ""
}
const transformStartProps = () => {
  let start = "start"
  let evenOption = ""
  let includeOption = ""
  let excludeOption = ""
  let file = "--file arthas-output/"
  if (selectEvent.value !== "all") {
    evenOption = "--event " + selectEvent.value
  }
  for (const v of includesVal) {
    includeOption += "--include " + v + " "
  }
  for (const v of excludesVal) {
    excludeOption += "--exclude " + v + " "
  }
  file += fileformat
  return {
    start,
    evenOption,
    includeOption,
    excludeOption,
    file
  }
}
const startSubmit = () => {
  const { start, evenOption, includeOption, excludeOption, file } = transformStartProps()


  fetchS.baseSubmit(fetchM, {
    action: "exec",
    command: `profiler ${start} ${evenOption} ${includeOption} ${excludeOption} ${fileformat}`,
    sessionId: undefined
  })
    .then(restartInit)
}
const stopProfiler = () => fetchS.baseSubmit(fetchM, {
  action: "exec",
  command: "profiler stop"
}).then(
  res => {
    profilerStatus.value.is = false
    let result = (res as CommonRes).body.results[0]
    if (result.type === "profiler" && result.outputFile) {
      outputPath.value = result.outputFile

      let reg = /arthas-output\/.*/
      let arr = reg.exec(result.outputFile)
      if (arr && arr.length > 0) {
        let url = window.origin + "/" + arr[0]
        window.open(url)
      }
    }

  }
)
const resumeProfiler = () => fetchS.baseSubmit(fetchM, {
  action: "exec",
  command: "profiler resume"
}).then(
  restartInit
)
const toOutputDir = () => window.open(window.location.origin + "/arthas-output/")
onBeforeMount(async () => {
  publicS.inputVal = ""
  includesVal.clear()
  excludesVal.clear()
  getStatusLoop.open()
  getSampleLoop.open()

  fetchS.asyncInit()
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
  <template v-if="support">
    <div class="flex py-2 border-b-2  border-gray-300">
      <h3 class="text-lg w-40">status: </h3>
      <div class="mx-2">
        <div>{{profilerStatus.message}}</div>
        <div v-if="profilerStatus.is">{{samples}} samples</div>
      </div>

    </div>
    <div class="flex border-b-2  border-gray-300 items-center py-2" v-if="!profilerStatus.is">
      <h3 class="text-lg w-40">How to start: </h3>
      <Listbox v-model="selectEvent">
        <div class=" relative mx-2">
          <ListboxButton class="btn btn-sm btn-outline"> even:
            <span class="normal-case">{{ selectEvent}}</span>
          </ListboxButton>
          <ListboxOptions
            class=" absolute w-52 mt-2 border py-2 rounded-md hover:shadow-xl transition max-h-80 overflow-y-auto bg-base-100">
            <ListboxOption v-for="(e,i) in eventList" :key="i" :value="e" v-slot="{active, selected}">
              <div class=" p-2 transition break-words" :class="{
              'bg-neutral text-neutral-content': active,
              'bg-neutral-focus text-neutral-content': selected,
              ' text-neutral':!active && !selected
              }">
                {{ e }}
              </div>
            </ListboxOption>
          </ListboxOptions>
        </div>
      </Listbox>
      <div class="btn-group mr-2">
        <button class="btn btn-sm btn-outline" @click.prevent="handleduration.decrease">-</button>
        <button class="btn btn-sm btn-outline border-x-0" @click.prevent="changeDuration">duration :{{duration}}</button>
        <button class="btn btn-sm btn-outline" @click.prevent="handleduration.increase">+</button>
      </div>
      <div class="btn-group mr-2">
        <button class="btn btn-sm btn-outline" @click.prevent="handleFramebuf.decrease">-</button>
        <button class="btn btn-sm btn-outline border-x-0" @click.prevent="changeFramebuf">framebuf :{{framebuf}}</button>
        <button class="btn btn-sm btn-outline" @click.prevent="handleFramebuf.increase">+</button>
      </div>
      <button class="btn btn-sm btn-outline mr-2" @click="changeFile">file :<span
          class="normal-case">{{fileformat}}</span></button>
      <TodoList title="include" :val-set="includesVal" class=" mr-2"></TodoList>
      <TodoList title="exclude" :val-set="excludesVal" class="mr-2"></TodoList>
      <button class="btn btn-primary btn-sm btn-outline" @click="startSubmit">start</button>
    </div>
    <div class="flex items-center border-b-2 border-gray-300 py-2">
      <h3 class="text-lg w-40">Resume or stop: </h3>
      <button class="btn btn-primary btn-sm btn-outline mx-2" @click="resumeProfiler"
        v-if="!profilerStatus.is">resume</button>
      <button class="btn btn-primary btn-sm btn-outline" @click="stopProfiler" v-if="profilerStatus.is">stop</button>
    </div>
    <div class="flex items-center py-2">
      <h3 class="text-lg w-40">output file path: </h3>
      <div class=" ml-2" v-if="outputPath.trim() !== ''">{{ outputPath }}</div>
      <button class="btn btn-primary btn-sm btn-outline ml-2" @click="toOutputDir">go to the output direction</button>
    </div>
  </template>
  <div v-else>
    Your system is not supported!
  </div>
</template>
