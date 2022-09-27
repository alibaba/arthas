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
    <label class="flex flex-1 items-center"> ognl:
      <div class="flex-1 flex justify-center ">
        <input type="text" v-model="express"
          class="border focus-visible:outline-none  mx-2 w-full rounded-lg p-2 hover:shadow-md focus-visible:shadow-md focus:shadow-md transition">
      </div>
      <button class="input-btn-style mr-2" @click.prevent="setDepth">depth:{{depth}}</button>
      <button class="input-btn-style mr-2" @click.prevent="setClassLoader" v-if="hashcode === ''">ClassLoaderClass:{{classloaderName}}</button>
      <button class="input-btn-style mr-2" @click.prevent="setHash" v-if="classloaderName === ''">hashcode:{{hashcode}}</button>
    </label>

    <button @click.prevent="getSource"
      class="bg-blue-400  rounded mr-4 hover:opacity-50 transition-all truncate p-2">submit</button>
  </form>
  <div v-if="code !== ''">
    <!-- <CmdResMenu title="classInfo" :map="locationMap"   class="mb-4"></CmdResMenu> -->
    <div class="w-10/12 rounded-xl border p-4 bg-[#f6f6f6] hover:shadow-gray-400 mx-auto shadow-lg transition mb-4">
      <highlightjs language="bash" :code="code" />
    </div>
  </div>
</template>