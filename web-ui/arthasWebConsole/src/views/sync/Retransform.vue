<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useInterpret } from '@xstate/vue';
import { reactive, ref } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { Switch, } from '@headlessui/vue';
import { interpret } from 'xstate';
import permachine from '@/machines/perRequestMachine';
const retransformListM = useInterpret(machine)
const { getPollingLoop } = fetchStore()
const retransformListMap = reactive(new Map<string, string[]>())
const fetchS = fetchStore()
const retransformRes = reactive(new Map<string, string[]>())
const retransformPath = ref('')
const enabled = ref(false)
const listLoop = getPollingLoop(() => {
  fetchS.baseSubmit(retransformListM, {
    action: "exec",
    command: "retransform -l"
  }).then(res => {
    const result = (res as CommonRes).body.results[0]
    retransformListMap.clear()
    if (result.type === "retransform") {
      result.retransformEntries.forEach(v => {
        console.log(111)
        retransformListMap.set(v.className, Object.entries(v).filter(([k, v]) => k !== "className").map(([k, v]) => `${k} : ${v.toString()}`))
      })
    }
  })
}, {
  step: 2000,
  globalIntrupt: true
})


const onSubmit = () => {
  let classPattern = ""
  if (enabled.value) classPattern += `--classPattern`
  return fetchStore().baseSubmit(interpret(permachine), {
    action: "exec",
    command: `retransform ${retransformPath.value} ${classPattern}`
  }).then(res => {
    let result = (res as CommonRes).body.results[0]

    if (result.type === "retransform") {
      retransformRes.clear()
      retransformRes.set("retransformClass", result.retransformClasses)
      retransformRes.set("retransformCount", [result.retransformCount.toString()])
    }
  })
}

const openList = () => {
  listLoop.invoke()
}
</script>

<template>
  <form class=" flex items-center justify-between mb-2">
    <label class="flex flex-1 items-center"> retransform
      <div class="w-full cursor-default 
        overflow-hidden rounded-lg bg-white text-left border 
        focus-within:outline
        outline-2
        min-w-[15rem]
        mx-2
        hover:shadow-md transition">
        <input type="text" v-model="retransformPath"
          class="w-full border-none py-2 pl-3 pr-10 leading-5 text-gray-900 focus-visible:outline-none">
      </div>
    </label>
    <div class="flex input-btn-style mr-2 focus-within:outline outline-2">
      <div class="mx-2">explicitly trigger</div>
      <Switch v-model="enabled" :class="enabled ? 'bg-blue-400' : 'bg-gray-500'"
        class="relative items-center inline-flex h-6 w-12 shrink-0 cursor-pointer rounded-full border-transparent transition-colors ease-in-out focus:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-opacity-75 mr-2">
        <span aria-hidden="true" :class="enabled ? 'translate-x-6' : '-translate-x-1'"
          class="pointer-events-none inline-block h-6 w-6 transform rounded-full bg-white shadow-md shadow-gray-500 ring-0 transition ease-in-out" />
      </Switch>
    </div>
    <button @click.prevent="onSubmit"
      class="bg-blue-400  rounded mr-4 hover:opacity-50 transition-all truncate p-2">submit</button>
  </form>

  <CmdResMenu title="response" :map="retransformRes" open v-if="retransformRes.size !== 0"></CmdResMenu>

  <CmdResMenu title="entries" :map="retransformListMap" @myclick="openList"></CmdResMenu>
</template>