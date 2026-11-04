<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { publicStore } from '@/stores/public';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, ref } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';

const { getCommonResEffect } = publicStore()
const fetchM = useMachine(machine)
const path = ref("")
const enabled = ref(false)
const map = reactive(new Map())
onBeforeMount(() => {
  fetchM.send("INIT")
})
getCommonResEffect(fetchM, body => {
  const result = body.results.filter(result => result.type === "heapdump")[0]
  if (result.type === "heapdump") {
    map.clear()
    map.set("filePath", [result.dumpFile])
    map.set("live", [result.live])
  }
})
const submitCommand = (e: Event) => {
  fetchM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: `heapdump ${enabled.value ? "--live" : ''}${path.value}`
    }
  })
}
</script>

<template>
  <form class="mb-4 flex items-center justify-between">
    <label class="flex flex-1 items-center"> path
      <div class="flex-1
        overflow-hidden rounded-lg bg-white text-left border 
        focus-within:outline
        outline-2
        min-w-[15rem]
        mx-2
        hover:shadow-md transition">
        <input type="text" v-model="path"
          class="w-full border-none py-2 pl-3 pr-10 leading-5 text-gray-900 focus-visible:outline-none">
      </div>

      <label class="label cursor-pointer btn-sm mr-2">
        <span class="label-text uppercase font-bold mr-1">only live object</span>
        <input v-model="enabled" type="checkbox" class="toggle" />
      </label>
    </label>

    <button @click.prevent="submitCommand"
      class="btn btn-primary btn-sm btn-outline transition-all truncate p-2">dump</button>
  </form>
  <CmdResMenu title="dumpRes" open :map="map" v-if="map.size > 0" class="mt-4"></CmdResMenu>
</template>

<style scoped>

</style>