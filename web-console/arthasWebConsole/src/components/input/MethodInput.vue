<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { publicStore } from '@/stores/public';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, ref } from 'vue';
import { waitFor } from 'xstate/lib/waitFor';
import AutoComplete from './AutoComplete.vue';
const { label = "inputClassName" } = defineProps<{
  label?: string,
  submitF: (classItem:Item,methodItem: Item) => void
}>()
const { getCommonResEffect } = publicStore()
const searchClass = useMachine(machine)

const optionClass = ref([] as { name: string, value: string }[])
const optionMethod = ref([] as { name: string, value: string }[])
onBeforeMount(() => {
  searchClass.send("INIT")
})

getCommonResEffect(searchClass, body => {
  optionClass.value.length = 0
  const result = body.results[0]
  if (result.type === "sc" && !result.detailed && !result.withField) {
    result.classNames.forEach(name => {
      optionClass.value.push({
        name,
        value: name
      })
    })
  } else if (result.type === "sm") {
    optionMethod.value.length = 0
    body.results.forEach(result => {
      if (result.type === "sm") {

        const name = result.methodInfo.methodName
        optionMethod.value.push({
          name,
          value: name
        })
      }
    })
  }
})

const changeClass = (value: string) => {
  if (value.length > 2 && searchClass.state.value.matches("ready")) {
    searchClass.send({
      type: "SUBMIT",
      value: {
        action: "exec",
        command: `sc *${value}*`
      }
    })
  }
}
const changeMethod = (classV: string, value: string) => {
  if (searchClass.state.value.matches("ready")) {
    searchClass.send({
      type: "SUBMIT",
      value: {
        action: "exec",
        command: `sm ${classV} *${value}*`
      }
    })
  }
}
const initMethod = async (classV:string)=>{
  await waitFor(searchClass.service,state=>state.matches("ready"))
      searchClass.send({
      type: "SUBMIT",
      value: {
        action: "exec",
        command: `sm ${classV}`
      }
    })
}
const filterfn = (_: any, item: Item) => true
</script>

<template>
  <AutoComplete :label="label" :option-items="optionClass" :input-fn="changeClass" :filter-fn="filterfn"
    v-slot="slotClass">
    <!-- <AutoComplete label="input Method" :option-items="optionMethod"
      :options-init="(_)=>"
      :input-fn="(value) => changeMethod(slotClass.selectItem.value as string, value)" 
      :filter-fn="filterfn"
      v-slot="slotMethod">     -->
      <AutoComplete label="input Method" :option-items="optionMethod"
      :options-init="(_)=>initMethod(slotClass.selectItem.value)"
      class="w-1/2"
      v-slot="slotMethod">
      <button @click.prevent="submitF(slotClass.selectItem, slotMethod.selectItem)"
        class="border bg-blue-400 p-2 rounded-md mx-2 hover:opacity-50 transition">submit</button>
    </AutoComplete>

  </AutoComplete>
</template>