<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import { waitFor } from 'xstate/lib/waitFor';
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import {
  Disclosure,
  DisclosureButton,
  DisclosurePanel,
  Listbox,
  ListboxButton,
  ListboxOptions,
  ListboxOption
} from "@headlessui/vue"
import { onBeforeMount, onBeforeUnmount, reactive, Ref, ref, watchEffect } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import Tree from '@/components/show/Tree.vue';
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const { pullResultsLoop, interruptJob, getCommonResEffect, getPullResultsEffect } = fetchS
const fetchM = useInterpret(permachine)
const loop = pullResultsLoop(pollingM)
const pollResults = reactive([] as [string, Map<string, string[]>, TreeNode][])
const enhancer = reactive(new Map())

const tranOgnl = (s: string): string[] => s.split("\n")
type Mode = "-f" | "-s" | "-e" | "-b"
const modelist: { name: string, value: Mode }[] = [
  { name: "调用开始之前", value: "-b" },
  { name: "异常返回之后", value: "-e" },
  { name: "正常返回之后", value: "-s" },
  { name: "调用结束之后", value: "-f" }
]
const mode = ref(modelist[3])
// const transTT = (result:CommonRes["body"]["results"][0])
getPullResultsEffect(
  pollingM,
  enhancer,
  result => {
    if (result.type === "watch") {
      const map = new Map();
      const key = result.ts
      map.set('accessPoint', [result.accessPoint])
      map.set("cost", [result.cost])
      map.set("sizeLimit", [result.sizeLimit + "M"])
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
          meta: str.substring(0,str.length - 1)
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

        console.log(JSON.stringify(stk))
      })

      pollResults.unshift([key, map, stk[0]])
    }
  })

onBeforeMount(() => {
  pollingM.send("INIT")
})
onBeforeUnmount(() => {
  loop.close()
})
// 最基本的submit基于Interrupt
const baseSubmit = async (value: ArthasReq, fn: (res?: ArthasRes) => void, err?: Function) => {
  fetchM.start()
  fetchM.send("INIT")
  fetchM.send({
    type: "SUBMIT",
    value
  })
  const state = await waitFor(fetchM, state => state.hasTag("result"))

  if (state.matches("success")) {
    fn(state.context.response)
  } else {
    err && err()
  }
  fetchM.stop()
}
const submit = async (classI: Item, methI: Item) => {
  baseSubmit({
    action: "async_exec",
    command: `watch ${mode.value.value} ${classI.value} ${methI.value} -x 4`,
    sessionId:undefined
  } , () => {
    pollResults.length = 0
    enhancer.clear()
    loop.open()
  })
}

</script>
  
<template>
  <MethodInput :submit-f="submit">
    <template #others>
      <Listbox v-model="mode">
        <div class=" relative mx-2 ">
          <ListboxButton class="border p-2 w-40 rounded-xl hover:shadow-md transition">{{ mode.name }}</ListboxButton>
          <ListboxOptions class=" absolute w-40 mt-2 border py-2 rounded-md hover:shadow-xl transition bg-white">
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
    </template>
  </MethodInput>
  <template v-if="pollResults.length > 0 || enhancer.size > 0">
    <CmdResMenu title="enhancer" :map="enhancer" open class="mt-4"></CmdResMenu>
    <ul class=" pointer-events-auto mt-10">
      <template v-for="(result, i) in pollResults" :key="i">
        <CmdResMenu :title="result[0]" :map="result[1]" open>
          <template #others>
            <Tree :root="result[2]" class="mt-2" button-class=" ">
              <template #meta="{ data, active }">
                <div 
                class="bg-blue-200 p-2 mb-2 rounded-r rounded-br"
                :class='{"hover:bg-blue-300 bg-blue-400":active}'
                >
                  {{data}}
                </div>
              </template>
            </Tree>
          </template>
        </CmdResMenu>
      </template>
    </ul>
  </template>
</template>