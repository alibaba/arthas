<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, ref, watchEffect } from 'vue';
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
const perfcounterMap = reactive(new Map<string, string[]>())
const vmOptionM = useMachine(machine)
const pwd = ref("?")
const vmOptionMTree = reactive([] as VmOption[])
// 初始化
onBeforeMount(() => {
  fetchS.baseSubmit(useInterpret(permachine), {
    action: "exec",
    command: "pwd"
  }).then(
    res => {
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


const getPerCounter = () => fetchS.baseSubmit(useInterpret(permachine), { action: "exec", command: "perfcounter -d" }).then(res => {
  const result = (res as CommonRes).body.results[0]
  if (result.type === "perfcounter") {
    const perfcounters = result.perfCounters
    perfcounterMap.clear()
    perfcounters.forEach(v => {
      perfcounterMap.set(v.name, Object.entries(v).filter(v => v[0] !== "name").map(([key, value]) => `${key} : ${value}`))
    })
  }
})
</script>

<template>
  <div class="p-2 h-[90vh] overflow-y-scroll">
    <article>
      <div class="flex items-center">
        <div class="btn-info btn my-2 btn-sm normal-case">workingDir</div>
        <div class="bg-base-200 w-full text-base-content pl-2"> {{ pwd }}</div>
      </div>
      <CmdResMenu title="sysenv" :map="sysEnvMap" @click="getSysenv" />
      <CmdResMenu title="sysprop" :map="sysPropMap" @click="getSysprop" />
      <option-config-menu title="vmOption" :list="vmOptionMTree" title-key-name="name">
        <template #item="{ kv, itemTitle, idx }">
          <switch-input :send="vmOptionSend(itemTitle)" :data="{ key: kv[0], value: kv[1] }" v-if="kv[0] === 'value'"
            :class="{ 'border-t-4': (idx > 0), 'border-base-100': (idx > 0) }">
          </switch-input>
          <div v-else class="flex " :class="{ 'border-t-4': (idx > 0), 'border-base-100': (idx > 0) }">
            <div class="bg-blue-200 w-1/5 p-1">{{ kv[0] }}</div>
            <div class="grid place-items-center w-3/5">{{ kv[1] }}</div>
          </div>
        </template>
      </option-config-menu>
      <CmdResMenu title="jvm" :map="jvmMap" class="w-full" @click="getJvm" />
      <CmdResMenu title="perfcounter" :map="perfcounterMap" @click="getPerCounter" />
    </article>
  </div>
</template>

<style scoped>

</style>