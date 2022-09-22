<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { onBeforeMount, onBeforeUnmount, reactive, ref, } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import Enhancer from '@/components/show/Enhancer.vue';
import { publicStore } from '@/stores/public';
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const { pullResultsLoop, getPullResultsEffect } = fetchS
const fetchM = useInterpret(permachine)
const loop = pullResultsLoop(pollingM)
const pollResults = reactive([] as [string, Map<keyof TimeFragment, string[]>][])

const enhancer = ref(undefined as EnchanceResult | undefined)
const trigerRes = reactive(new Map<string, string[]>)
const cacheIdx = ref("-1")
const inputVal = ref("")
const keyList:tfkey[] = [
  "index",
  "timestamp",
  "className",
  "methodName",
  "cost",
  "object",
  "params",
  "returnObj",
  "throwExp",
  // 暂时隐藏这两个属性，不够宽了
  // "return",
  // "throw",
]
const tableResults = reactive([] as Map<string, string>[])
type tfkey = keyof TimeFragment
// const tranOgnl = (s: string): string[] => s.replace(/\r\n\tat/g, "\r\n\t@").split("\r\n\t")
const transform = (tf:TimeFragment)=>{
  const map = new Map()
  Object.keys(tf).forEach((k) => {
          let val:string|string[] = []
          if ((k) === "params") {
            tf.params.forEach(para => {
              // 以后可能会有bug
              for(const key in para){
                // @ts-ignore
                val.push(`${key}:${para[key].toString()}`)
              }
            })
          } else {
            val = (tf[k as tfkey].toString())
          }
          map.set(k , val)
        })
  return map
}
getPullResultsEffect(
  pollingM,
  result => {
    if (result.type === "tt") {
      result.timeFragmentList.forEach(tf => {
        tableResults.unshift(transform(tf))
      })
    }
  })
onBeforeMount(() => {
  pollingM.send("INIT")
  fetchS.asyncInit()
})
onBeforeUnmount(() => {
  loop.close()
})

const submit = async (data: { classItem: Item, methodItem: Item, count: number }) => {
  // let express = data.express.trim() == "" ? "" : `-w '${data.express.trim()}'`
  let n = data.count > 0 ? `-n ${data.count}` : ""
  fetchS.baseSubmit(fetchM, {
    action: "async_exec",
    command: `tt -t ${data.classItem.value} ${data.methodItem.value} ${n}`,
    sessionId: undefined
  }).then(res => {
    enhancer.value = undefined
    loop.open()
  })
}
const alltt = () => fetchS.baseSubmit(fetchM, {
  action: "exec",
  command: `tt -l`
}).then((res) => {
  let result = (res as CommonRes).body.results[0]
  trigerRes.clear()
  tableResults.length = 0
  if (result.type === "tt") {
    result.timeFragmentList.forEach(tf => {
        tableResults.unshift(transform(tf))

    })
  }
})

const reTrigger = (idx: string) => fetchS.baseSubmit(fetchM, {
  action: "exec",
  command: `tt -i ${idx} -p`,
}).then(
  res => {
    let result = (res as CommonRes).body.results[0]

    if (result.type === "tt") {
      trigerRes.clear()
      cacheIdx.value = idx
      let tf = result.replayResult

      Object.keys(tf).forEach((k) => {
        let val: string[] = []
        if ((k as keyof TimeFragment) === "params") {
          tf.params.forEach(para => {
            val.push(JSON.stringify(para))
          })
        } else {
          val.push(tf[k as keyof TimeFragment].toString())
        }
        trigerRes.set(k as tfkey, val)
      })

      trigerRes.set("sizeLimit", [result.sizeLimit.toString()])
      trigerRes.set("replayNo", [result.replayNo.toString()])
    }
  }, () => {
    trigerRes.clear()
  }
)

const searchTt = () => {
  let condition = inputVal.value.trim() !== "" ? `'${inputVal.value}'` : ''
  return fetchS.baseSubmit(fetchM, {
    action: "exec",
    command: `tt -s ${condition}`
  }).then(res => {
    tableResults.length = 0
    trigerRes.clear()
    let result = (res as CommonRes).body.results[0]
    if (result.type === "tt") {
      if(result.timeFragmentList.length === 0) {
        publicStore().$patch({
          isErr:true,
          ErrMessage: "not found"
        })
        return
      }
      result.timeFragmentList.forEach(tf => {
        tableResults.unshift(transform(tf))
      })
    }
  })
}
</script>

<template>
  <MethodInput :submit-f="submit" ncount>
  </MethodInput>
  <div class="flex items-center border-t-2 pt-4 mt-4 justify-between">
    <div class="mr-2">searching records</div>
    <div
      class="flex-1 cursor-default overflow-hidden rounded-lg bg-white text-left border focus:outline-none hover:shadow-md transition">
      <input type="text" v-model="inputVal"
        class="w-full border-none py-2 pl-3 pr-10 h-full text-gray-900  focus:outline-none">
    </div>
    <button @click="searchTt" class="mx-2 button-style">search</button>
  </div>
  <div class="flex justify-end">
    <button class="button-style my-4" @click="alltt">
      all records
    </button>
  </div>
  <div class="text-gray-500">
    <CmdResMenu title="invoked result" :map="trigerRes" v-if="trigerRes.size > 0">
      <template #headerAside>
        <div class="flex mt-2 justify-end mr-1">
          <button @click="reTrigger(cacheIdx)" class="button-style p-1">invoke</button>
        </div>
      </template>
    </CmdResMenu>
  </div>
  <template v-if="pollResults.length > 0 || enhancer|| tableResults.length > 0">
    <Enhancer :result="enhancer" v-if="enhancer"></Enhancer>
    <div class="w-full flex justify-center items-center ">
      <table class="border-collapse border border-slate-400 table-fixed">
        <thead>
          <tr>
            <th class="border border-slate-300 p-1" v-for="(v,i) in keyList" :key="i">{{v}}</th>
            <th class="border border-slate-300 p-1">invoke</th>
          </tr>
        </thead>
        <tbody class="">
          <tr v-for="(map, i) in tableResults" :key="i">
            <td class="border border-slate-300 p-1" v-for="(key,j) in keyList" :key="j">
              <template v-if=" key !== 'params'">
                {{map.get(key)}}
              </template>
              
              <div class="flex flex-col" v-else>
                <div v-for="(row, k) in map.get(key)" :key="k">
                  {{row}}
                </div>
              </div>
            </td>
            <td class="border border-slate-300 ">
              <button class="button-style" @click="reTrigger(map.get('index')!)">invoke</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </template>
</template>

<style scoped>

</style>