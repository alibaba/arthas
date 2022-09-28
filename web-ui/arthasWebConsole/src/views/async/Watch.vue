<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import {
  Listbox,
  ListboxButton,
  ListboxOptions,
  ListboxOption,
  Switch,
  SwitchLabel,
  SwitchGroup,
  SwitchDescription
} from "@headlessui/vue"
import { onBeforeMount, onBeforeUnmount, reactive, Ref, ref, watchEffect } from 'vue';
import Tree from '@/components/show/Tree.vue';
import Enhancer from '@/components/show/Enhancer.vue';
import { publicStore } from '@/stores/public';
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const publiC = publicStore()
const { pullResultsLoop, getPullResultsEffect } = fetchS
const fetchM = useInterpret(permachine)
const loop = pullResultsLoop(pollingM)
const pollResults = reactive([] as [string, Map<string, string[]>, TreeNode][])
const enhancer = ref(undefined as EnchanceResult | undefined)
const depth = ref(1)
const tableResults = reactive([] as Map<string, string | TreeNode>[])
const keyList = [
  "ts",
  "accessPoint",
  "className",
  "methodName",
  "cost",
  // "sizeLimit",
  "value",
]
const tranOgnl = (s: string): string[] => s.split("\n")
// type Mode = "-f" | "-s" | "-e" | "-b"
// const modelist: { name: string, value: Mode }[] = [
//   { name: "before method being invoked", value: "-b" },
//   { name: "when method encountering exceptions", value: "-e" },
//   { name: "when method exits normally", value: "-s" },
//   { name: "when method exits", value: "-f" }
// ]
const beforeInvoke = ref(false)
const successInvoke = ref(false)
const failureInvoke = ref(false)
const allInvoke = ref(true)
const modereflist: { enabled: Ref<boolean>, name: string }[] = [
  { enabled: beforeInvoke, name: "before" },
  { enabled: successInvoke, name: "success" },
  { enabled: failureInvoke, name: "exception" },
  { enabled: allInvoke, name: "finish" }
]
// const mode = ref(modelist[3])

const transform = (result: CommandResult) => {
  const map = new Map();
  if (result.type !== "watch") return map

  for (const key in result) {
    if (key !== "value") {
      //@ts-ignore
      map.set(key, result[key])
    }
  }
  let raw = tranOgnl(result.value)
  const stk: TreeNode[] = []
  // Tree的构建
  raw.forEach(v => {
    let str = v.trim()
    let match = 0
    for (let s of str) {
      if (s === "[") {
        match++
      } else if (s === "]") {
        match--
      }
    }
    const root = {
      children: [],
      meta: str.substring(0, str.length - 1)
    } as TreeNode

    if (match > 0) {
      stk.push(root)
    } else if (match === 0) {
      let cur = stk.pop()
      if (cur) {
        cur.children!.push(root)
        stk.push(cur)
      } else {
        stk.push(root)
      }

    } else {
      /// 默认每行只会一个]
      //!可能会有bug
      let cur = stk.pop()!
      if (stk.length > 0) {
        let parent = stk.pop()!
        parent.children!.push(cur)
        stk.push(parent)
      } else {
        // 构建结束
        stk.push(cur)
      }

    }
  })
  map.set("value", stk[0])
  return map
}
getPullResultsEffect(
  pollingM,
  result => {
    console.log(result)
    if (result.type === "watch") {
      tableResults.unshift(transform(result))
    }
    if (result.type === "enhancer") {
      enhancer.value = result
    }
  })
const setDepth = publiC.inputDialogFactory(
  depth,
  (raw) => {
    let valRaw = parseInt(raw)
    return Number.isNaN(valRaw) ? 1 : valRaw
  },
  (input) => input.value.toString(),
)
onBeforeMount(() => {
  pollingM.send("INIT")
  fetchS.asyncInit()
})
onBeforeUnmount(() => {
  loop.close()
})

const submit = async (data: { classItem: Item, methodItem: Item, conditon: string, express: string }) => {
  let conditon = data.conditon.trim() == "" ? "" : `'${data.conditon.trim()}'`
  let express = data.express.trim() == "" ? "" : `'${data.express.trim()}'`
  let mode = ""
  if (beforeInvoke.value) mode += " -b"
  if (failureInvoke.value) mode += " -e"
  if (successInvoke.value) mode += " -s"
  if (allInvoke.value) mode += " -f"
  tableResults.length = 0
  fetchS.baseSubmit(fetchM, {
    action: "async_exec",
    command: `watch ${mode} ${data.classItem.value} ${data.methodItem.value} -x ${depth.value} ${conditon} ${express}`,
    sessionId: undefined
  }).finally(() => {
    pollResults.length = 0
    enhancer.value = undefined
    loop.open()
  })
}

</script>
  
<template>
    <MethodInput :submit-f="submit" nexpress ncondition>
      <template #others>
        <div class="relative group ml-2">
          <div class="input-btn-style">watching point</div>
          <div class="h-0 group-hover:h-auto group-focus-within:h-auto absolute overflow-clip transition z-10 top-full">
            <SwitchGroup v-for="(mode,i) in modereflist" :key="i">
              <div class="flex input-btn-style ml-2 focus-within:outline outline-1 justify-between m-2 bg-white">
                <SwitchLabel class="mr-2">{{mode.name}}:</SwitchLabel>
                <Switch v-model="mode.enabled.value" :class="mode.enabled.value ? 'bg-blue-400' : 'bg-gray-500'"
                  class="relative items-center inline-flex h-6 w-12 shrink-0 cursor-pointer rounded-full border-transparent transition-colors ease-in-out focus:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-opacity-75 mr-2">
                  <span aria-hidden="true" :class="mode.enabled.value ? 'translate-x-6' : '-translate-x-1'"
                    class="pointer-events-none inline-block h-6 w-6 transform rounded-full bg-white shadow-md shadow-gray-500 ring-0 transition ease-in-out" />
                </Switch>
              </div>
            </SwitchGroup>
          </div>
        </div>
        <button class="input-btn-style ml-2" @click="setDepth">depth:{{depth}}</button>
      </template>
    </MethodInput>
    <div v-if="pollResults.length > 0 || enhancer" class=" pointer-events-auto">
      <Enhancer :result="enhancer" v-if="enhancer"></Enhancer>
      <div class="flex justify-center mt-4 pointer-events-auto">
        <table class="border-collapse border border-slate-400 table-fixed">
          <thead>
            <tr>
              <th class="border border-slate-300 p-2" v-for="(v,i) in keyList" :key="i">{{v}}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(map, i) in tableResults" :key="i">
              <td class="border border-slate-300 p-2" v-for="(key,j) in keyList" :key="j">
                <div v-if=" key !== 'value'">
                  {{map.get(key)}}
                </div>

                <div class="flex flex-col" v-else>
                  <Tree :root="(map.get('value') as TreeNode)" class="mt-2" button-class=" ">
                    <template #meta="{ data, active }">
                      <div class="bg-blue-200 p-2 mb-2 rounded-r rounded-br"
                        :class='{"hover:bg-blue-300 bg-blue-400":active}'>
                        {{data}}
                      </div>
                    </template>
                  </Tree>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
</template>