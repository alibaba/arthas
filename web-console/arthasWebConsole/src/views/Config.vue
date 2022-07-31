<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import transform from "@/machines/transformConfigMachine";
import { useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, ref, watchEffect } from 'vue';
import ConfigMenu from '@/components/ConfigMenu.vue';
const { state, send } = useMachine(machine)
const map = reactive(new Map<string, string[]>())

onBeforeMount(() => {
  send({ type: "INIT" })

  send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: "sysenv",
    }
  })
})

const envTree = reactive([] as string[])
const handleEnvTree = (data: object): string[] => {
  map.clear()
  let res: string[] = []
  Object.entries(data).forEach(([k, v]) => {
    res.push(k)
    if (v.includes(";")) {
      map.set(k, reactive(v.split(";")))
    } else {
      map.set(k, reactive([v]))
    }
  })
  return res
}

watchEffect(() => {
  const response = state.value.context.response
  if (response) {
    if (Object.hasOwn(response, "body")) {
      const result = (response as CommonRes).body.results[0]

      if (result.type == "sysenv") {
        handleEnvTree(result.env).forEach(v => {
          envTree.push(v)
        })
      }
    }
  }
})
</script>

<template>
  <div class="p-2 max-h-[90vh] overflow-auto">
    <article>
        <config-menu title="sysenv" :list="envTree" :map="map" v-if="state.context.response"/>
    </article>
  </div>
</template>

<style scoped>
</style>