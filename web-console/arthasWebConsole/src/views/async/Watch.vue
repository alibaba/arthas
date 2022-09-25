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
  ListboxOption
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
type Mode = "-f" | "-s" | "-e" | "-b"
const modelist: { name: string, value: Mode }[] = [
  { name: "调用开始之前", value: "-b" },
  { name: "异常返回之后", value: "-e" },
  { name: "正常返回之后", value: "-s" },
  { name: "调用结束之后", value: "-f" }
]
const mode = ref(modelist[3])

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
  tableResults.length = 0
  fetchS.baseSubmit(fetchM, {
    action: "async_exec",
    command: `watch ${mode.value.value} ${data.classItem.value} ${data.methodItem.value} -x ${depth.value} ${conditon} ${express}`,
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
      <Listbox v-model="mode">
        <div class=" relative mx-2 ">
          <ListboxButton class="input-btn-style w-40">{{ mode.name }}</ListboxButton>
          <ListboxOptions
            class=" absolute w-40 mt-2 border overflow-hidden rounded-md hover:shadow-xl transition bg-white">
            <ListboxOption v-for="(am,i) in modelist" :key="i" :value="am" v-slot="{active, selected}">
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
      <button class="input-btn-style" @click="setDepth">depth:{{depth}}</button>
    </template>
  </MethodInput>
  <template v-if="pollResults.length > 0 || enhancer">
    <Enhancer :result="enhancer" v-if="enhancer"></Enhancer>
    <div class="flex justify-center mt-4">
      <table class="border-collapse border border-slate-400 table-fixed pointer-events-auto">
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
  </template>
</template>