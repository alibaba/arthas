<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, watchEffect } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
const map = reactive(new Map<string, string[]>())
const fetchM = useMachine(machine)
const {getCommonResEffect} = publicStore()
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

getCommonResEffect(fetchM,(body)=>{
  const result = body.results[0]
      if (result.type === "jvm") {
        map.clear()
        Object.entries(result.jvmInfo).forEach(([k, v]) => {
          map.set(k, v.map(v => `${v.name} : ${v.value}`))
        })
      }
})

</script>

<template>
  <CmdResMenu title="jvm" :map="map" class="w-full" />
</template>

<style scoped>
</style>