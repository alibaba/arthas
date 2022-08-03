<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, ref, watchEffect } from 'vue';
import ConfigMenu from '@/components/ConfigMenu.vue';
import { watch } from 'fs';
import { Console } from 'console';
import OptionConfigMenu from '@/components/OptionConfigMenu.vue';
import SwitchInput from '@/components/SwitchInput.vue';
const sysEnvM = useMachine(machine)
const sysPropM = useMachine(machine)
const sysEnvMap = reactive(new Map<string, string[]>())
const sysPropMap = reactive(new Map<string, string[]>())
const vmOptionM = useMachine(machine)
// 初始化
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

  vmOptionM.send("INIT")
  vmOptionM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: "vmoption"
    }
  })
})

const sysEnvTree = reactive([] as string[])
const sysPropTree = reactive([] as string[])
const vmOptionMTree = reactive([] as VmOption[])
// 处理展示的树形数据
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

// 处理可修改参数的树形结构
// const handleOptionTree = (data:Record<string>)


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

watchEffect(() => {
  const response = vmOptionM.state.value.context.response
  if (response) {
    if (Object.hasOwn(response, "body")) {
      console.log(response, "vmoption")
      const result = (response as CommonRes).body.results[0]

      if (result.type == "vmoption") {
        // handlePropTree(result.props).forEach(v => {
        //   sysPropTree.push(v)
        // })
        // 先clear一下之前的东西
        vmOptionMTree.length = 0
        console.log(result.vmOptions,"watchEffect!!!")
        result.vmOptions.forEach(v => {
          vmOptionMTree.push(v)
        })
      }
    }
  }
})
const vmOptionSend = (pre: string) => (v: { key: string, value: boolean }) => vmOptionM.send({
  type: 'SUBMIT', value: {
    action: "exec",
    command: `vmoption ${pre} ${v.value}`
  }
})

</script>

<template>
  <div class="p-2 max-h-[90vh] overflow-auto">
    <article>
      <config-menu title="sysenv" :list="sysEnvTree" :map="sysEnvMap" v-if="sysEnvM.state.value.context.response" />
      <config-menu title="sysprop" :list="sysPropTree" :map="sysPropMap" v-if="sysPropM.state.value.context.response" />
      <!-- <config-menu title="memory" :list="sysPropTree" :map="sysPropMap" v-if="sysPropM.state.value.context.response" /> -->
      <option-config-menu title="vmOption" :list="vmOptionMTree" title-key-name="name">
        <template #item="{ kv, itemTitle, idx }">
          <switch-input :send="vmOptionSend(itemTitle)" :data="{ key: kv[0], value: kv[1] }"
            v-if="kv[0] === 'value'" :class="{ 'border-t-4': (idx > 0), 'border-white': (idx > 0) }">
          </switch-input>
          <div v-else class="flex " :class="{ 'border-t-4': (idx > 0), 'border-white': (idx > 0) }">
            <div class="bg-blue-200 w-1/5 p-2">{{ kv[0] }}</div>
            <div class="grid place-items-center w-3/5">{{ kv[1] }}</div>
          </div>
        </template>
      </option-config-menu>
    </article>
  </div>
</template>

<style scoped>
</style>