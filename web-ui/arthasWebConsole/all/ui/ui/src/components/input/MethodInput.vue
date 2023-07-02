<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { ref } from 'vue';
import { interpret } from 'xstate';
import AutoComplete from './AutoComplete.vue';
const { label = "className", ncondition = false, nexpress = false, ncount = false } = defineProps<{
  label?: string,
  nexpress?: boolean,
  ncondition?: boolean,
  ncount?: boolean,
  submitF: (data: {
    classItem: Item, methodItem: Item,
    conditon: string,
    express: string,
    count: number
  }) => void
}>()
const fetchS = fetchStore()
const optionClass = ref([] as { name: string, value: string }[])
const optionMethod = ref([] as { name: string, value: string }[])
const conditon = ref("")
const express = ref("")
const autoStop = ref(0)
const changeClass = (value: string) => {
  const searchClass = interpret(permachine)
  if (value.length > 2) {
    return fetchS.baseSubmit(searchClass, {
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
  return Promise.resolve()
}
const changeMethod = (classV: string, value: string) => {
  const searchMethod = interpret(permachine)
  return fetchS.baseSubmit(searchMethod, {
    action: "exec",
    command: `sm ${classV} *${value}*`
  }).then(
    res => {
      optionMethod.value.length = 0
        ; (res as CommonRes).body.results.forEach(result => {
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
}
const setCount = publicStore().inputDialogFactory(autoStop,
  (raw) => {
    let valRaw = parseInt(raw)
    return Number.isNaN(valRaw) ? 0 : valRaw
  },
  (input) => input.value.toString()
)
const setConditon = publicStore().inputDialogFactory(
  conditon,
  raw => raw,
  _ => _.value
)
const setExpress = publicStore().inputDialogFactory(
  express,
  raw => raw,
  _ => _.value
)
const { increase, decrease } = publicStore().numberCondition(autoStop, { min: 0, max: 100 })

const filterfn = (_: any, item: Item) => true
</script>

<template>
  <AutoComplete :label="label" :option-items="optionClass" :input-fn="changeClass" :filter-fn="filterfn"
    v-slot="slotClass">
    <AutoComplete label="method" :option-items="optionMethod"
      :input-fn="(value: string) => changeMethod(slotClass.selectItem.value as string, value)" :filter-fn="filterfn"
      v-slot="slotMethod">
      <button v-if="nexpress" class="btn btn-sm btn-outline ml-2"
        @click.prevent="setExpress">express: <span class="normal-case">{{express}}</span></button>
      <button v-if="ncondition" class="btn btn-sm btn-outline ml-2"
        @click.prevent="setConditon">condition: <span class="normal-case">{{conditon}}</span></button>
      <div class="btn-group ml-2" v-if="ncount">
        <button class="btn btn-sm btn-outline" @click.prevent="decrease">-</button>
        <button class="btn btn-sm btn-outline border-x-0" @click.prevent="setCount">count:{{autoStop}}</button>
        <button class="btn btn-sm btn-outline" @click.prevent="increase">+</button>
      </div>

      <slot name="others" :methodItem="slotMethod.selectItem" :classItem="slotClass.selectItem"></slot>
      <button @click.prevent="submitF({
      classItem: slotClass.selectItem, 
      methodItem: slotMethod.selectItem,
      conditon,
      express,
      count:autoStop
      })" class="btn btn-primary btn-sm btn-outline mx-2 transition">submit</button>
    </AutoComplete>

  </AutoComplete>
</template>