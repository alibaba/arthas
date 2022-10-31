<script setup lang="ts">
import 'highlight.js/lib/common';
import highlightjsP from "@highlightjs/vue-plugin";
import { useInterpret } from '@xstate/vue';
import { ref } from 'vue';
import { fetchStore } from '@/stores/fetch';
import permachine from '@/machines/perRequestMachine';
import { publicStore } from '@/stores/public';
const publiC = publicStore()
const express = ref("")
const highlightjs = highlightjsP.component
const sourceM = useInterpret(permachine)
const code = ref('')
const depth = ref(1)
const classloaderName = ref("")
const hashcode = ref("")
const setDepth = publiC.inputDialogFactory(
  depth,
  (raw) => {
    const valRaw = parseInt(raw)
    const realVal = Number.isNaN(valRaw) ? 1 : valRaw
    return realVal
  },
  (input) => input.value.toString(),
)
const {increase, decrease} = publiC.numberCondition(depth,{min:1})
const setHash = publiC.inputDialogFactory(
  hashcode,
  (raw) => raw,
  (input) => input.value.toString(),
)
const setClassLoader = publiC.inputDialogFactory(
  classloaderName,
  (raw) => raw,
  (input) => input.value.toString(),
)
const getSource = () => {
  let nhash = ""
  let nclassLoader = ""
  let ndepth = `-x ${depth.value}`
  if(classloaderName.value !== "") nclassLoader += `--classLoaderClass ${classloaderName.value}`
  if(hashcode.value !== "") nhash += `-c ${hashcode.value}}`

  fetchStore().baseSubmit(sourceM, {
    action: "exec",
    command: `ognl ${express.value} ${nclassLoader} ${nhash} ${ndepth}`
  }).then(
    res => {
      const result = (res as CommonRes).body.results[0]
      if (result.type === "ognl") {
        code.value = result.value
      }
    }
  )
}

</script>
<template>
  <form class="mb-4 flex items-center justify-between">
    <label class="flex flex-1 items-center"> ognl
      <div class="w-full cursor-default 
        overflow-hidden rounded-lg bg-white text-left border 
        focus-within:outline
        outline-2
        min-w-[15rem]
        mx-2
        hover:shadow-md transition">
        <input type="text" v-model="express"
          class="w-full border-none py-2 pl-3 pr-10 leading-5 text-gray-900 focus-visible:outline-none">
      </div>
      <div class="btn-group mr-2">
        <button class="btn btn-outline btn-sm" @click.prevent="decrease">-</button>
        <button class="btn btn-outline btn-sm border-x-0" @click.prevent="setDepth">depth:{{depth}}</button>
        <button class="btn btn-outline btn-sm" @click.prevent="increase">+</button>
      </div>
      <button class="btn btn-sm btn-outline mr-2" @click.prevent="setClassLoader" v-if="hashcode === ''">ClassLoaderClass:{{classloaderName}}</button>
      <button class="btn btn-sm btn-outline mr-2" @click.prevent="setHash" v-if="classloaderName === ''">hashcode:{{hashcode}}</button>
    </label>

    <button @click.prevent="getSource"
      class="btn btn-primary btn-sm btn-outline truncate p-2">submit</button>
  </form>
  <div v-if="code !== ''">
    <div class="w-10/12 rounded-xl border p-4 bg-[#f6f6f6] hover:shadow-gray-400 mx-auto shadow-lg transition mb-4">
      <highlightjs language="bash" :code="code" />
    </div>
  </div>
</template>