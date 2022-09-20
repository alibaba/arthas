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
  DisclosurePanel
} from "@headlessui/vue"
import { onBeforeMount, onBeforeUnmount, reactive, ref, watchEffect } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import Enhancer from '@/components/show/Enhancer.vue';
import { publicStore } from '@/stores/public';
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const { pullResultsLoop, interruptJob, getCommonResEffect, getPullResultsEffect } = fetchS
const fetchM = useInterpret(permachine)
const loop = pullResultsLoop(pollingM)
const pollResults = reactive([] as [string, Map<keyof TimeFragment, string[]>][])
const timeFragmentL = ref([] as typeof pollResults)
// const ttSet = new Set()
const enhancer = ref(undefined as EnchanceResult | undefined)
const trigerRes = reactive(new Map<string, string[]>)
const cacheIdx = ref("-1")
const inputVal = ref("")
type tfkey = keyof TimeFragment
const tranOgnl = (s: string): string[] => s.replace(/\r\n\tat/g, "\r\n\t@").split("\r\n\t")
// const transTT = (result:CommonRes["body"]["results"][0])
getPullResultsEffect(
  pollingM,
  result => {
    if (result.type === "tt") {
      result.timeFragmentList.forEach(tf => {
        const Mkey = tf.index

        const map = new Map<tfkey, string[]>()
        Object.keys(tf).forEach((k) => {
          let val: string[] = []
          if ((k as tfkey) === "params") {
            tf.params.forEach(para => {
              val.push(JSON.stringify(para))
            })
          } else if ((k as tfkey) === "throwExp") {
            val = tranOgnl(tf.throwExp)
          } else {
            val.push(tf[k as tfkey].toString())
          }
          map.set(k as tfkey, val)
        })
        // if (!ttSet.has(Mkey)) {
        //   ttSet.add(Mkey)
        pollResults.unshift([Mkey.toString(), map])
        // }
      })
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
const submit = async (data: { classItem: Item, methodItem: Item }) => {
  // let express = data.express.trim() == "" ? "" : `-w '${data.express.trim()}'`

  baseSubmit({
    action: "async_exec",
    command: `tt -t ${data.classItem.value} ${data.methodItem.value}`,
    sessionId: undefined
  }, () => {
    pollResults.length = 0
    timeFragmentL.value.length = 0
    enhancer.value = undefined
    loop.open()
  })
}
const alltt = async () => {
  fetchS.baseSubmit(fetchM, {
    action: "exec",
    command: `tt -l`
  }).then((res) => {
    let result = (res as CommonRes).body.results[0]
    timeFragmentL.value.length = 0
    pollResults.length = 0
    if (result.type === "tt") {
      result.timeFragmentList.forEach(tf => {
        const Mkey = tf.index

        const map = new Map<keyof TimeFragment, string[]>()
        Object.keys(tf).forEach((k) => {
          let val: string[] = []
          if ((k as keyof TimeFragment) === "params") {
            tf.params.forEach(para => {
              val.push(JSON.stringify(para))
            })
          } else if ((k as tfkey) === "throwExp") {
            val = tranOgnl(tf.throwExp)
          } else {
            val.push(tf[k as keyof TimeFragment].toString())
          }
          map.set(k as tfkey, val)
        })

        timeFragmentL.value.unshift([Mkey.toString(), map])

      })
    }
  })
}
const reTrigger = async (idx: string) => {
  await baseSubmit({
    action: "exec",
    command: `tt -i ${idx} -p`,
  }, res => {
    let result = (res as CommonRes).body.results[0]

    if (result.type === "tt") {
      trigerRes.clear()
      cacheIdx.value = idx
      let tf = result.replayResult

      const Mkey = tf.index
      Object.keys(tf).forEach((k) => {
        let val: string[] = []
        if ((k as keyof TimeFragment) === "params") {
          tf.params.forEach(para => {
            val.push(JSON.stringify(para))
          })
        } else if ((k as tfkey) === "throwExp") {
          val = tranOgnl(tf.throwExp)
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
  })
}
const searchTt = () => {
  let condition = inputVal.value.trim() !== "" ? `'${inputVal.value}'` : ''
  fetchS.baseSubmit(fetchM, {
    action: "exec",
    command: `tt -s ${condition}`
  }).then(res => {
    pollResults.length = 0
    timeFragmentL.value.length = 0
    let result = (res as CommonRes).body.results[0]
    timeFragmentL.value.length = 0
    if (result.type === "tt") {
      result.timeFragmentList.forEach(tf => {
        const Mkey = tf.index

        const map = new Map<keyof TimeFragment, string[]>()
        Object.keys(tf).forEach((k) => {
          let val: string[] = []
          if ((k as keyof TimeFragment) === "params") {
            tf.params.forEach(para => {
              val.push(JSON.stringify(para))
            })
          } else if ((k as tfkey) === "throwExp") {
            val = tranOgnl(tf.throwExp)
          } else {
            val.push(tf[k as keyof TimeFragment].toString())
          }
          map.set(k as tfkey, val)
        })

        timeFragmentL.value.unshift([Mkey.toString(), map])

      })
    }
  })
}
</script>

<template>
  <MethodInput :submit-f="submit"></MethodInput>
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
    <CmdResMenu title="result" :map="trigerRes" v-if="trigerRes.size > 0">
      <!-- <div class="flex mt-2 justify-end mr-1">
        <button @click="reTrigger(cacheIdx)" class="bg-blue-400 hover:opacity-60 transition p-1 rounded">invoke</button>
      </div> -->
      <template #headerAside>
        <div class="flex mt-2 justify-end mr-1">
          <button @click="reTrigger(cacheIdx)"
            class="button-style p-1">invoke</button>
        </div>
      </template>
    </CmdResMenu>
    <template v-if="timeFragmentL.length > 0">
      <template v-for="(result, i) in timeFragmentL" :key="result[0]">
        <CmdResMenu :title="result[0]" :map="result[1]">
          <template #headerAside>
            <div class="flex mt-2 justify-end mr-1">
              <button @click="reTrigger(result[0])"
                class="bg-blue-400 hover:opacity-60 transition p-1 rounded">invoke</button>
            </div>
          </template>
        </CmdResMenu>
      </template>
    </template>
  </div>

  <template v-if="pollResults.length > 0 || enhancer">
    <Enhancer :result="enhancer" v-if="enhancer"></Enhancer>
    <ul class=" pointer-events-auto mt-10">
      <template v-for="(result, i) in pollResults" :key="i">
        <CmdResMenu :title="result[0]" :map="result[1]" open></CmdResMenu>
      </template>
    </ul>
  </template>
</template>

<style scoped>

</style>