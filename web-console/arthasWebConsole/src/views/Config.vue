<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, ref, watchEffect } from 'vue';
import ConfigMenu from '@/components/show/ConfigMenu.vue';
import OptionConfigMenu from '@/components/show/OptionConfigMenu.vue';
import SwitchInput from '@/components/input/SwitchInput.vue';
import { publicStore } from '@/stores/public';
import { Disclosure, DisclosureButton, DisclosurePanel } from "@headlessui/vue"
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import { fetchStore } from '@/stores/fetch';
import permachine from '@/machines/perRequestMachine';
const fetchS = fetchStore()
const sysEnvMap = reactive(new Map<string, string[]>())
const sysPropMap = reactive(new Map<string, string[]>())
const vmOptionM = useMachine(machine)
const pwdM = useMachine(machine)
const pwd = ref("?")
const vmOptionMTree = reactive([] as VmOption[])
// 初始化
onBeforeMount(() => {
  fetchS.baseSubmit(useInterpret(permachine),{
    action:"exec",
    command:"pwd"
  }).then(
    res=>{
      const result = (res as CommonRes).body.results[0]
      if (result.type == "pwd") {
        pwd.value = result.workingDir
      }
    }
  )

  vmOptionM.send("INIT")
  vmOptionM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: "vmoption"
    }
  })
})


// 处理展示的树形数据
const handleTree = (data: Record<string, string | boolean | number>, map: Map<string, string[]>): string[] => {
  map.clear()
  let res: string[] = []
  Object.entries(data).forEach(([k, v]) => {
    res.push(k)
    if (typeof v === "boolean") v = v.toString()
    if (typeof v === "number") v = v.toString()
    if (k.toLowerCase().includes("path")) {
      map.set(k, v.split(":").filter(v => v.trim() !== ''))
    } else if (v.includes(";")) {
      map.set(k, v.split(";").filter(v => v.trim() !== ''))
    } else {
      map.set(k, [v])
    }
  })
  return res
}

const handleEnvTree = (data: Record<string, string>) => handleTree(data, sysEnvMap)

const handlePropTree = (data: Record<string, string>) => handleTree(data, sysPropMap)

// 处理可修改参数的树形结构
// const handleOptionTree = (data:Record<string>)



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
        console.log(result.vmOptions, "watchEffect!!!")
        result.vmOptions.forEach(v => {
          vmOptionMTree.push(v)
        })
      }
    }
  }
})
const vmOptionSend = (pre: string) => (v: { key: string, value: boolean | string }) => {
  if (pre === "HeapDumpPath" && v.value === "") {
    publicStore().$patch({ ErrMessage: "HeapDumpPath can't be set \"\" ", isErr: true })
    return
  } vmOptionM.send({
    type: 'SUBMIT', value: {
      action: "exec",
      command: `vmoption ${pre} ${v.value === "" ? '\"\"' : v.value}`
    }
  })
}

const jvmMap = reactive(new Map<string, string[]>())

const getJvm = () => fetchS.baseSubmit(useInterpret(permachine), {
  action: "exec",
  command: "jvm"
}).then(res => {
  const result = (res as CommonRes).body.results[0]
  if (result.type === "jvm") {
    jvmMap.clear()
    Object.entries(result.jvmInfo).forEach(([k, v]) => {
      jvmMap.set(k, v.map(v => `${v.name} : ${v.value}`))
    })
  }
})
const getSysenv = () => fetchS.baseSubmit(useInterpret(permachine), {
  action: "exec",
  command: "sysenv",
}).then(res => {
  let result = (res as CommonRes).body.results[0]
  if (result.type == "sysenv") {
    handleEnvTree(result.env)
  }
})
const getSysprop = () => fetchS.baseSubmit(useInterpret(permachine), {
  action: "exec",
  command: "sysprop",
}).then(res => {
  let result = (res as CommonRes).body.results[0]
  if (result.type == "sysprop") {
    handlePropTree(result.props)
  }
})
</script>

<template>
  <div class="p-2 h-[90vh] overflow-y-scroll">
    <article>
      <Disclosure as="section" class="flex w-10/12 mb-2">
        <DisclosureButton class="bg-blue-200 p-2 w-1/4 break-all">
          workingDir
        </DisclosureButton>
        <DisclosurePanel as="div" static class="flex-auto bg-blue-100 flex flex-col justify-center pl-2">
          {{ pwd }}
        </DisclosurePanel>
      </Disclosure>
      <CmdResMenu title="sysenv" :map="sysEnvMap" @click="getSysenv" />
      <CmdResMenu title="sysprop" :map="sysPropMap" @click="getSysprop" />
      <option-config-menu title="vmOption" :list="vmOptionMTree" title-key-name="name">
        <template #item="{ kv, itemTitle, idx }">
          <switch-input :send="vmOptionSend(itemTitle)" :data="{ key: kv[0], value: kv[1] }" v-if="kv[0] === 'value'"
            :class="{ 'border-t-4': (idx > 0), 'border-white': (idx > 0) }">
          </switch-input>
          <div v-else class="flex " :class="{ 'border-t-4': (idx > 0), 'border-white': (idx > 0) }">
            <div class="bg-blue-200 w-1/5 p-2">{{ kv[0] }}</div>
            <div class="grid place-items-center w-3/5">{{ kv[1] }}</div>
          </div>
        </template>
      </option-config-menu>
      <CmdResMenu title="jvm" :map="jvmMap" class="w-full" @click="getJvm" />
    </article>
  </div>
</template>

<style scoped>

</style>