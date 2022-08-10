<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, watchEffect } from 'vue';
import CmdResMenu from '@/components/CmdResMenu.vue';
const list = reactive([] as string[])
const map = new Map<string, string[]>()
const fetchM = useMachine(machine)
onBeforeMount(() => {
  fetchM.send("INIT")
  fetchM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: "jvm"
    }
  })
})

watchEffect(() => {
  if (fetchM.state.value.context.response) {
    const response = fetchM.state.value.context.response
    if (Object.hasOwn(response, "body")) {
      const result = (response as CommonRes).body.results[0]
      if (result.type === "jvm") {
        list.length = 0
        map.clear()
        Object.entries(result.jvmInfo).forEach(([k, v]) => {
          list.push(k)
          map.set(k, v.map(v => `${v.name} : ${v.value}`))
        })
      }
    }

  }
})
</script>

<template>
  <div class="p-2 overflow-auto flex-1">
    <CmdResMenu title="jvm" :list="list" :map="map" class="w-full" />
  </div>
</template>

<style scoped>
</style>