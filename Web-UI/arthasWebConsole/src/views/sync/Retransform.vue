<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, ref } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { Disclosure, DisclosureButton, DisclosurePanel, Switch, } from '@headlessui/vue';
import { interpret } from 'xstate';
import permachine from '@/machines/perRequestMachine';
const retransformListM = useInterpret(machine)
const { getPollingLoop } = fetchStore()
const publiC = publicStore()
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
},{
  step:2000,
  globalIntrupt:true
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
let list = false
const openList = () => {
  list = !list
  if (list) {
    listLoop.open()
  } else {
    listLoop.close()
  }
}
</script>

<template>
  <CmdResMenu title="entries" :map="retransformListMap" @myclick="openList"></CmdResMenu>
  <Disclosure class="w-100 flex flex-col mb-2" as="section">
    <DisclosureButton
      class="py-2 bg-blue-400  rounded self-start hover:opacity-50 transition-all duration-100 truncate w-80">
      retransform
    </DisclosureButton>
    <DisclosurePanel class=" border-t-2 mt-4">
      <form class="mt-4 flex items-center justify-between">
        <label class="flex flex-1 items-center"> retransform :
          <div class="flex-1 flex justify-center ">
            <input type="text" v-model="retransformPath"
              class="border focus-visible:outline-none  m-2 w-full rounded-lg p-2 hover:shadow-md focus-visible:shadow-md focus:shadow-md transition">
          </div>
        </label>
        <button @click.prevent="onSubmit"
          class="bg-blue-400  rounded mr-4 hover:opacity-50 transition-all truncate p-2">submit</button>
      </form>
      <div class="flex items-center mt-2 justify-end mr-4">
        <span class="mr-4">显式使用?</span>
        <Switch v-model="enabled" :class="enabled ? 'bg-blue-400' : 'bg-gray-500'"
          class="relative items-center inline-flex h-6 w-12 shrink-0 cursor-pointer rounded-full border-transparent transition-colors ease-in-out focus:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-opacity-75">
          <span aria-hidden="true" :class="enabled ? 'translate-x-6' : '-translate-x-1'"
            class="pointer-events-none inline-block h-6 w-6 transform rounded-full bg-white shadow-md shadow-gray-500 ring-0 transition ease-in-out" />
        </Switch>
      </div>
      <CmdResMenu title="response" :map="retransformRes" open v-if="retransformRes.size !== 0"></CmdResMenu>
    </DisclosurePanel>
  </Disclosure>
</template>