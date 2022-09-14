<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, ref, watchEffect } from 'vue';
import { PlusCircleIcon, MinusCircleIcon } from "@heroicons/vue/outline"
import {
  Disclosure,
  DisclosureButton,
  DisclosurePanel,
  Listbox,
  ListboxButton,
  ListboxOptions,
  ListboxOption,
  Menu,
  MenuButton,
  MenuItem,
  MenuItems
} from "@headlessui/vue"
import { waitFor } from 'xstate/lib/waitFor';
import { publicStore } from '@/stores/public';
import TodoList from '@/components/input/TodoList.vue';
import { fetchStore } from '@/stores/fetch';
import machine from '@/machines/consoleMachine';
const fetchM = useInterpret(permachine)
const publicS = publicStore()
const fetchS = fetchStore()
const pollingM = useMachine(machine)
const loop = fetchS.pullResultsLoop(pollingM)
// const pollResults = reactive([] as [string, Map<string, string[]>, TreeNode][])
const baseSubmit = async (fetchM: ReturnType<typeof useInterpret>, value: ArthasReq, success: (res?: ArthasRes) => void, err?: Function) => {
  fetchM.start()
  fetchM.send("INIT")
  fetchM.send({
    type: "SUBMIT",
    value
  })
  const state = await waitFor(fetchM, state => state.hasTag("result"))

  if (state.matches("success")) {
    success(state.context.response)
  } else {
    err && err()
  }
  fetchM.stop()
}
let eventList = reactive(["all"] as string[]);
let selectEvent = ref(eventList[0])
let includesVal = reactive(new Set<string>())
let excludesVal = reactive(new Set<string>())
// let duration = ref(300)
onBeforeMount(async () => {
  publicS.inputVal = "",
    includesVal.clear()
  await baseSubmit(fetchM, {
    action: "exec",
    command: "profiler list"
  }, res => {
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
  })
}
)
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

  const value = {
    action: "async_exec",
    command: `profiler ${start} ${evenOption} ${includeOption} ${excludeOption}`
  } as ArthasReq

  baseSubmit(fetchM, value, (res) => {
    // pollResults.length = 0
    loop.open()
  })
}

</script>

<template>
  <div class="flex">
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
    <TodoList title="include" :val-set="includesVal" class=" mr-2"></TodoList>
    <TodoList title="exclude" :val-set="excludesVal"></TodoList>
    <button class="button-style" @click="startSubmit">start</button>
  </div>
</template>

<style scoped>

</style>