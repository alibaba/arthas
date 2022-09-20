<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { useInterpret, useMachine } from '@xstate/vue';
import { onBeforeMount, ref } from 'vue';
import { interpret } from 'xstate';
import { waitFor } from 'xstate/lib/waitFor';
import AutoComplete from './AutoComplete.vue';
const { label = "inputClassName",ncondition=false,nexpress=false } = defineProps<{
  label?: string,
  nexpress?:boolean,
  ncondition?:boolean,
  submitF: (data:{classItem: Item, methodItem: Item,
    conditon:string,
    express:string
  }) => void
}>()
const { getCommonResEffect } = publicStore()
// const searchClass = useMachine(machine)
const fetchS = fetchStore()
const optionClass = ref([] as { name: string, value: string }[])
const optionMethod = ref([] as { name: string, value: string }[])
const conditon = ref("")
const express=ref("")

const changeClass = (value: string) => {
  const searchClass = interpret(permachine)
  if (value.length > 2) {
    fetchS.baseSubmit(searchClass, {
      action: "exec",
      command: `sc *${value}*`
    }).then(
      res => {
        optionClass.value.length = 0
        let result = (res as CommonRes).body.results[0]
        if (result.type === "sc" && !result.detailed && !result.withField) {
          result.classNames.forEach(name => {
            optionClass.value.push({
              name,
              value: name
            })
          })
        }
      }
    )
  }

}
const changeMethod = (classV: string, value: string) => {
  const searchMethod = interpret(permachine)
  fetchS.baseSubmit(searchMethod, {
    action: "exec",
    command: `sm ${classV} *${value}*`
  }).then(
    res => {
      optionMethod.value.length = 0
      ;(res as CommonRes).body.results.forEach(result => {
          if (result.type === "sm") {

            const name = result.methodInfo.methodName
            optionMethod.value.push({
              name,
              value: name
            })
          }
      })
    }
  )
  // if (searchClass.state.value.matches("ready")) {
  //   searchClass.send({
  //     type: "SUBMIT",
  //     value: {
  //       action: "exec",
  //       command: `sm ${classV} *${value}*`
  //     }
  //   })
  // }
}

const setConditon = publicStore().inputDialogFactory(
  conditon,
  raw=>raw,
  _=>_.value
)
const setExpress = publicStore().inputDialogFactory(
  express,
  raw=>raw,
  _=>_.value
)
const filterfn = (_: any, item: Item) => true
</script>

<template>
  <AutoComplete :label="label" :option-items="optionClass" :input-fn="changeClass" :filter-fn="filterfn"
    v-slot="slotClass">
    <AutoComplete label="input Method" :option-items="optionMethod"
      :input-fn="(value) => changeMethod(slotClass.selectItem.value as string, value)" :filter-fn="filterfn"
      v-slot="slotMethod">
      <button v-if="nexpress" class="input-btn-style ml-2" @click="setExpress">express:{{express}}</button>
      <button v-if="ncondition" class="input-btn-style ml-2" @click="setConditon">condition:{{conditon}}</button>
      <slot name="others"></slot>
      <button @click.prevent="submitF({
        classItem: slotClass.selectItem, 
        methodItem: slotMethod.selectItem,
        conditon,
        express
        })"
        class="border bg-blue-400 p-2 rounded-md mx-2 hover:opacity-50 transition">submit</button>
    </AutoComplete>

  </AutoComplete>
</template>