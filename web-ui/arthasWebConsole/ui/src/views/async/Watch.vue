<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
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
const {increase,decrease} = publiC.numberCondition(depth,{min:1,max:6})
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
        <div class="btn btn-sm btn-outline">watching point</div>
        <div class="h-0 group-hover:h-auto group-focus-within:h-auto absolute overflow-clip transition z-10 top-full pt-2">

          <label class="label cursor-pointer btn-sm border border-neutral ml-2 bg-base-100"
            v-for="(mode,i) in modereflist" :key="i">
            <span class="label-text uppercase font-bold mr-1">{{mode.name}}</span>
            <input v-model="mode.enabled.value" type="checkbox" class="toggle" />
          </label>

        </div>

      </div>
      <div class="btn-group ml-2">
        <button class="btn btn-sm btn-outline" @click.prevent="decrease">-</button>
        <button class="btn btn-sm btn-outline border-x-0" @click.prevent="setDepth">depth:{{depth}}</button>
        <button class="btn btn-sm btn-outline" @click.prevent="increase">+</button>
      </div>
    </template>
  </MethodInput>
  <Enhancer :result="enhancer" v-if="enhancer"></Enhancer>
  <div class="flex justify-center mt-4 overflow-auto">
    <table class="table w-full group">
      <thead>
        <tr>
          <th class="border border-slate-300" v-for="(v,i) in keyList" :key="i" :class="{'group-first:z-0':i===0}">{{v}}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(map, i) in tableResults" :key="i">
          <td class="border border-slate-300" v-for="(key,j) in keyList" :key="j">
            <div v-if=" key !== 'value'">
              {{map.get(key)}}
            </div>

            <div class="flex flex-col" v-else>
              <Tree :root="(map.get('value') as TreeNode)" class="mt-2" button-class=" ">
                <template #meta="{ data, active }">
                  <div 
                    class="bg-info  px-2 rounded-r rounded-br mr-2 text-info-content" :class='{
                  "hover:opacity-50":active
                }'
                    >
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