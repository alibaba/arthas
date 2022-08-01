<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, ref, watchEffect } from 'vue';
import ConfigMenu from '@/components/ConfigMenu.vue';
const sysEnvM = useMachine(machine)
const sysPropM = useMachine(machine)
const sysEnvMap = reactive(new Map<string, string[]>())
const sysPropMap = reactive(new Map<string, string[]>())

onBeforeMount(() => {
  sysEnvM.send("INIT")
  sysEnvM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: "sysenv",
    }
  })
  sysPropM.send("INIT")
  sysPropM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: "sysprop",
    }
  })
})

const sysEnvTree = reactive([] as string[])
const sysPropTree = reactive([] as string[])

// 处理数据
const handleTree = (data: Record<string, string>, map: Map<string, string[]>): string[] => {
  map.clear()
  let res: string[] = []
  Object.entries(data).forEach(([k, v]) => {
    res.push(k)
    if (v.includes(";")) {
      map.set(k, reactive(v.split(";").filter(v => v.trim() !== '')))
    } else {
      map.set(k, reactive([v]))
    }
  })
  return res
}

const handleEnvTree = (data: Record<string, string>) => handleTree(data, sysEnvMap)

const handlePropTree = (data: Record<string, string>) => handleTree(data, sysPropMap)

watchEffect(() => {
  const response = sysEnvM.state.value.context.response
  if (response) {
    if (Object.hasOwn(response, "body")) {
      const result = (response as CommonRes).body.results[0]

      if (result.type == "sysenv") {
        handleEnvTree(result.env).forEach(v => {
          sysEnvTree.push(v)
        })
      }
    }
  }
})

watchEffect(() => {
  const response = sysPropM.state.value.context.response
  if (response) {
    if (Object.hasOwn(response, "body")) {
      const result = (response as CommonRes).body.results[0]

      if (result.type == "sysprop") {
        handlePropTree(result.props).forEach(v => {
          sysPropTree.push(v)
        })
      }
    }
  }
})

</script>

<template>
  <div class="p-2 max-h-[90vh] overflow-auto">
    <article>
      <config-menu title="sysenv" :list="sysEnvTree" :map="sysEnvMap" v-if="sysEnvM.state.value.context.response" />
      <config-menu title="sysprop" :list="sysPropTree" :map="sysPropMap" v-if="sysPropM.state.value.context.response" />
    </article>
  </div>
</template>

<style scoped>
</style>