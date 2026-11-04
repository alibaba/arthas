<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useInterpret } from '@xstate/vue';
import { reactive, ref } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import { fetchStore } from '@/stores/fetch';
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
    <label class="label cursor-pointer btn-sm mr-2">
      <span class="label-text uppercase font-bold mr-1">explicitly trigger</span>
      <input v-model="enabled" type="checkbox" class="toggle" />
    </label>
    <button @click.prevent="onSubmit" class="btn btn-primary btn-sm btn-outline mr-4 truncate">submit</button>
  </form>

  <CmdResMenu title="response" :map="retransformRes" open v-if="retransformRes.size !== 0"></CmdResMenu>

  <CmdResMenu title="entries" :map="retransformListMap" @myclick="openList"></CmdResMenu>
</template>