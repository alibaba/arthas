<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { publicStore } from '@/stores/public';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, ref } from 'vue';
import { Switch } from "@headlessui/vue"
import CmdResMenu from '@/components/show/CmdResMenu.vue';

const { getCommonResEffect } = publicStore()
const fetchM = useMachine(machine)
const path = ref("")
const enabled = ref(false)
const map = reactive(new Map())
onBeforeMount(() => {
  fetchM.send("INIT")
})
getCommonResEffect(fetchM,body=>{
  const result = body.results.filter(result=>result.type === "heapdump")[0]
  if(result.type === "heapdump"){
    map.clear()
    map.set("filePath", [result.dumpFile])
    map.set("live",[result.live])
  }
})
const submitCommand = (e: Event) => {
  fetchM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: `heapdump ${enabled.value?"--live":''}${path.value}`
    }
  })
}
</script>

<template>
  <form class="h-[10vh] flex items-center border shadow p-2" @submit.prevent="submitCommand">
    <label for="command-input" class=" m-2 ">path : </label>
    <div class=" flex-auto grid place-items-start">
      <input type="text" v-model="path" id="command-input"
        class=" outline-1 focus-visible:outline-gray-600 border rounded hover:shadow h-10 transition w-full box-border">
    </div>
    <div class="mx-2">only live object : </div>
    <Switch v-model="enabled" :class="enabled ? 'bg-blue-400' : 'bg-gray-500'"
      class="relative items-center inline-flex h-6 w-12 shrink-0 cursor-pointer rounded-full border-transparent transition-colors ease-in-out focus:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-opacity-75 mr-2">
      <span aria-hidden="true" :class="enabled ? 'translate-x-6' : '-translate-x-1'"
        class="pointer-events-none inline-block h-6 w-6 transform rounded-full bg-white shadow-md shadow-gray-500 ring-0 transition ease-in-out" />
    </Switch>
    <button class="hover:shadow w-24 h-10 border rounded-md">
      dump
    </button>
  </form>
  <CmdResMenu title="dumpRes" open :map="map" v-if="map.size > 0" class="mt-4"></CmdResMenu>
</template>

<style scoped>
</style>