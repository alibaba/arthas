<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, ref } from 'vue';
import AutoComplete from "@/components/input/AutoComplete.vue";
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import { waitFor } from 'xstate/lib/waitFor';
import { interpret } from 'xstate';
import permachine from '@/machines/perRequestMachine';
const { getCommonResEffect } = publicStore()
const searchMbean = useMachine(machine)
const allMbean = useMachine(machine)
const optionItems = ref([] as { name: string, value: string }[])
const attributesMap = reactive(new Map<string, string[]>())
const constructorsMap = reactive(new Map<string, string[]>())
const operationMap = reactive(new Map<string, string[]>())
const className = ref('')
const description = ref('')

let mbeanName = ''
onBeforeMount(() => {
  searchMbean.send("INIT")
  allMbean.send("INIT")
  // allLoop.open()
})
getCommonResEffect(searchMbean, body => {
  optionItems.value.length = 0
  const result = body.results[0]

  if (result.type === "mbean") {
    if (Object.hasOwn(result, "mbeanMetadata") && Object.hasOwn(result.mbeanMetadata, mbeanName)) {
      const res = result.mbeanMetadata[mbeanName]
      attributesMap.clear()
      constructorsMap.clear()
      operationMap.clear()
      className.value = ''
      description.value = ''
      res.attributes
        .forEach(v => {
          attributesMap.set(
            v.name,
            Object
              .entries(v)
              .filter(([k, v]) => k !== "name")
              .map(([k, v]) => {
                if (k === "openType") return `${k} : ${JSON.stringify(v)}`
                return `${k} : ${v.toString()}`
              })
          )
        })
      res.constructors
        .forEach(v => {
          constructorsMap.set(
            v.name,
            Object
              .entries(v)
              .filter(([k, v]) => k !== "name")
              .map(([k, v]) => {
                if (k === "signature") return `${k} : ${JSON.stringify(v)}`
                return `${k} : ${v.toString()}`
              })
          )
        })

      res.operations
        .forEach(operation => {
          operationMap.set(
            operation.name,
            Object
              .entries(operation)
              .filter(([k, v]) => k !== "name")
              .map(([k, v]) => {
                if (k === "signature") return `${k} : ${JSON.stringify(v)}`
                return `${k} : ${v.toString()}`
              })
          )
        })

      className.value = res.className
      description.value = res.description
    }
    if (Object.hasOwn(result, "mbeanAttribute") && Object.hasOwn(result.mbeanAttribute, mbeanName)) {
      const res = result.mbeanAttribute[mbeanName]
      res.forEach(({ name, value }) => {
        let format = "value : "
        if (["string", "number", "boolean"].includes(typeof value)) {
          format += value.toString()
        } else {
          format += JSON.stringify(value)
        }
        const v = attributesMap.get(name)
        if (v) v.push(format)
        else attributesMap.set(name, [format])
      })
    }
  }

})
const getMbeanInfo = async (item: Item) => {
  searchMbean.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: `mbean -m ${item.value}`
    }
  })
  mbeanName = item.value as string
  await waitFor(searchMbean.service, state => state.matches("ready"))
  searchMbean.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: `mbean ${item.value}`
    }
  })
}
const getAll = () => fetchStore().baseSubmit(interpret(permachine), {
  action: "exec",
  command: `mbean`
}).then(res=>{
  const result = (res as CommonRes).body.results[0]
  optionItems.value.length = 0
  if (result.type === "mbean" && Object.hasOwn(result, "mbeanNames")) {
    result.mbeanNames.forEach(name => {
      optionItems.value.push({
        name,
        value: name
      })
    })
  } 
})
</script>

<template>
  <AutoComplete label="mbeanInfo" :option-items="optionItems" :input-fn="getAll" v-slot="slotP" as="form">
    <button @click.prevent="getMbeanInfo(slotP.selectItem)"
      class="btn btn-primary btn-sm btn-outline transition">submit</button>
  </AutoComplete>
  <div v-if="className !== ''" class="mt-4">
    <h2 class="flex justify-center my-4 text-xl">{{ className }}</h2>
    <div class="flex my-4 pl-10">description : {{ description }}</div>
    <CmdResMenu title="arrtibute" :map="attributesMap"></CmdResMenu>
    <CmdResMenu title="constructors" :map="constructorsMap"></CmdResMenu>
    <CmdResMenu title="operations" :map="operationMap"></CmdResMenu>
  </div>
</template>
