<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useActor, useMachine } from '@xstate/vue';
import { onBeforeMount, onUnmounted, reactive, ref } from 'vue';
import { Disclosure, DisclosureButton, DisclosurePanel } from '@headlessui/vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import { publicStore } from "@/stores/public"
import transformMachine from '@/machines/transformConfigMachine';
import ClassInput from '@/components/input/ClassInput.vue';
import { fetchStore } from '@/stores/fetch';
import { interpret } from 'xstate';
const { getCommonResEffect } = publicStore()
const allClassM = useMachine(machine)
const urlStatM = useMachine(machine)
const classInfoM = useMachine(machine)
const actor = useMachine(transformMachine)
// const actor = useActor()
const classMethodInfoM = useMachine(machine)
const { getPollingLoop } = fetchStore()
const map = ref([] as [string, Map<"hash" | "parent" | "classes", string[]>][])
const urlStats = ref([] as [
  string,
  Map<"hash" | "unUsedUrls" | "usedUrls"|"parent", string[]>
][])
// const classDetailInfo = ref<string[]>([])
const classDetailMap = reactive(new Map<string, string[]>())
const classFields = reactive(new Map<string, string[]>())
const classMethodMap = reactive(new Map<string, string[]>())
const urlStatsLoop = getPollingLoop(() => urlStatM.send({
  type: "SUBMIT",
  value: {
    action: "exec",
    command: "classloader --url-stat"
  }
}),3000)
getCommonResEffect(allClassM, body => {
  console.log("all", body)
  body.results.filter(res => res.type === "classloader").reduce((pre, cur) => {
    if (cur.type === "classloader" && Object.hasOwn(cur, "classSet")) {
      const classSet = cur.classSet
      const classes = classSet.classes
      if (classSet.segment === 0) {
        const listMap = new Map<"hash" | "parent" | "classes", string[]>([
          ["hash", [classSet.classloader.hash]],
          ["parent", [classSet.classloader.parent]],
          ["classes", classes]
        ])
        map.value.push([classSet.classloader.name, listMap])
      } else {
        const listMap = map.value[map.value.length - 1][1]
        listMap.set("classes", [...listMap.get("classes")!, ...classes])
      }
    }
    return pre
  }, [] as string[][])
})

getCommonResEffect(urlStatM, body => {
  console.log("urlStatM", body)
  const result = body.results[0]
  if (result.type === "classloader" && Object.hasOwn(result, "urlStats")) {
    urlStats.value.length = 0
    Object.entries(result.urlStats).forEach(([k, v]) => {
      const actor = interpret(transformMachine)
      actor.start()
      console.log("key", k)
      actor.send("INPUT", {
        data: k
      })
      if (actor.state.matches("failure")) {
        publicStore().$patch({
          isErr: true,
          ErrMessage: actor.state.context.err
        })
        
      } else {
        const obj = actor.state.context.output as Record<"hash" | "name"|"parent", string>
        console.log('helloworld', obj)
        urlStats.value.push([
          obj.name,
          new Map([
            ["parent", [obj.parent]],
            ["hash", [obj.hash]],
            ["unUsedUrls", v.unUsedUrls],
            ["usedUrls", v.usedUrls]
          ])
        ])
      }

    })
  }
})

getCommonResEffect(classInfoM, body => {
  const result = body.results[0]
  if (result.type === "sc" && result.detailed === true && result.withField === true) {

    classDetailMap.clear()
    classFields.clear()

    Object.entries(result.classInfo).filter(([k, v]) => k !== "fields").forEach(([k, v]) => {
      let value: string[] = []
      if (!["interfaces", "annotations", "classloader", "superClass"].includes(k)) value.push(v.toString())
      else value = v as string[]
      classDetailMap.set(k, value)
    })

    result.classInfo.fields.forEach(field => {
      classFields.set(field.name, Object.entries(field).filter(([k, v]) => k !== "name").map(([k, v]) => {
        if (k === "value") v = JSON.stringify(v)
        return `${k}: ${v}`
      }))
    })
  }
})
getCommonResEffect(classMethodInfoM,body=>{
    const result = body.results[0]
  if (result.type === "sm" && result.detail === true) {

    // classDetailMap.clear()
    // classFields.clear()

    // Object.entries(result.classInfo).filter(([k, v]) => k !== "fields").forEach(([k, v]) => {
    //   let value: string[] = []
    //   if (!["interfaces", "annotations", "classloader", "superClass"].includes(k)) value.push(v.toString())
    //   else value = v as string[]
    //   classDetailMap.set(k, value)
    // })

    // result.classInfo.fields.forEach(field => {
    //   classFields.set(field.name, Object.entries(field).filter(([k, v]) => k !== "name").map(([k, v]) => {
    //     if (k === "value") v = JSON.stringify(v)
    //     return `${k}: ${v}`
    //   }))
    // })
  }
})
onBeforeMount(() => {
  allClassM.send("INIT")
  allClassM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: "classloader -a"
    }
  })
  urlStatM.send("INIT")
  urlStatsLoop.open()
  classInfoM.send("INIT")
})
onUnmounted(() => {
  urlStatsLoop.close()
})
const getClassInfo = (item: Item) => {
  classInfoM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: `sc -d -f ${item.value}`
    }
  })
} 
</script>

<template>
  <Disclosure>
    <DisclosureButton class="w-1/3 bg-blue-500 h-10 p-2 rounded mb-2">
      all classloader
    </DisclosureButton>
    <DisclosurePanel>
      <li v-for="v in map" :key="v[0]" class="flex flex-col">
        <CmdResMenu :title="v[0]" :list="['hash', 'parent', 'classes']" :map="v[1]" button-width="w-1/2"></CmdResMenu>
      </li>
    </DisclosurePanel>

  </Disclosure>
  <Disclosure>
    <DisclosureButton class="w-1/3 bg-blue-500 h-10 p-2 rounded mb-2">
      urlStats
    </DisclosureButton>
    <DisclosurePanel>
      <li v-for="v in urlStats" :key="v[0]" class="flex flex-col">
        <CmdResMenu :title="v[0]" :map="v[1]" button-width="w-1/2" open>
        </CmdResMenu>
      </li>
    </DisclosurePanel>

  </Disclosure>
  <Disclosure>
    <DisclosureButton class="w-1/3 bg-blue-500 h-10 p-2 rounded mb-2  ">
      classInfo
    </DisclosureButton>
    <DisclosurePanel class="border-t-2 py-4 mt-4">
      <ClassInput :submit-f="getClassInfo"></ClassInput>
      <div>
        <template v-if="classDetailMap.size !== 0">
          <h4 class="grid place-content-center mb-2 text-3xl mt-4">classInfo</h4>
          <CmdResMenu :map="classFields" title="fields"></CmdResMenu>
          <CmdResMenu :map="classDetailMap" title="detail"></CmdResMenu>
        </template>
      </div>
    </DisclosurePanel>
  </Disclosure>
</template>

<style scoped>
</style>