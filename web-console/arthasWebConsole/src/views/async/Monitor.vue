<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import { waitFor } from 'xstate/lib/waitFor';
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { onBeforeMount, onBeforeUnmount, reactive } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const { getPollingLoop, pullResultsLoop, interruptJob, getCommonResEffect } = fetchS
const fetchM = useInterpret(permachine)
const loop = pullResultsLoop(pollingM)
const pollResults = reactive([] as [string, Map<string, string[]>][])
const enhancer = reactive(new Map())

/**
 * 打算引入动态的堆叠图，但是不知道timestamp还有cost 应该是rt，估计得找后端去补这个接口
 */


getCommonResEffect(pollingM, body => {
  if (body.results.length > 0) {
    body.results.forEach(result => {
      if (result.type === "monitor") {
        result.monitorDataList.forEach(data => {
          console.log(data),
            console.log(new Date().getDate())
          const map = new Map<string,string[]>()
          Object
            .entries(data)
            .filter(([k, _]) => !["className", "methodName"].includes(k))
            .forEach(([k, v]) => {
              let val: string[] = []
              if(k === "cost"){
                val.push((v as number).toFixed(2) + 'ms')
              }else val.push(v.toString())
              map.set(k, val)
            })
          pollResults.unshift([pollResults.length.toString(), map])
        })
      }
      if (result.type === "enhancer") {
        enhancer.clear()
        enhancer.set("success", [result.success])
        for (const k in result.effect) {
          enhancer.set(k, [result.effect[k as "cost"]])
        }
      }
    })
  }
})
onBeforeMount(() => {
  pollingM.send("INIT")
})
onBeforeUnmount(() => {
  loop.close()
})
const submit = async (classI: Item, methI: Item) => {
  fetchM.start()
  fetchM.send("INIT")
  fetchM.send({
    type: "SUBMIT",
    value: {
      action: "async_exec",
      command: `monitor -c 5 ${classI.value} ${methI.value}`,
      sessionId:undefined
    }
  })
  const state = await waitFor(fetchM, state => state.hasTag("result"))

  if (state.matches("success")) {
    loop.open()
  }
  fetchM.stop()
}
</script>

<template>
  <MethodInput :submit-f="submit" class="mb-4"></MethodInput>
  <template v-if="pollResults.length > 0 || enhancer.size > 0">
    <CmdResMenu title="enhancer" :map="enhancer" open></CmdResMenu>
    <ul class=" pointer-events-auto mt-10">
      <template v-for="(result, i) in pollResults" :key="i">
        <CmdResMenu :title="result[0]" :map="result[1]" open></CmdResMenu>
      </template>
    </ul>
  </template>
</template>