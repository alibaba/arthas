<script setup lang="ts">
import 'highlight.js/lib/common';
import highlightjsP from "@highlightjs/vue-plugin";
import ClassInput from '@/components/input/ClassInput.vue';
import { useMachine } from '@xstate/vue';
import machine from '@/machines/consoleMachine';
import { onBeforeMount, ref } from 'vue';
import { publicStore } from '@/stores/public';
const { getCommonResEffect } = publicStore()
const highlightjs = highlightjsP.component
const sourceM = useMachine(machine)
const code = ref('')

const getSource = (item: Item) => {
  sourceM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: `jad ${item.value}`
    }
  })
}
getCommonResEffect(sourceM, body => {
  const result = body.results[0]

  if (result.type === "jad") {
    code.value = result.source
  }
})
onBeforeMount(() => {
  sourceM.send("INIT")
})
</script>
<template>
  <!-- <div class="flex flex-col items-center w-full"> -->
    <div class="mb-4">
      <ClassInput :submit-f="getSource"></ClassInput>
    </div>
    <div class="w-10/12 rounded-xl border p-4 bg-[#f6f6f6] hover:shadow-gray-600 mx-auto shadow-lg transition">
      <highlightjs language="Java" :code="code" />
    </div>
  <!-- </div> -->
</template>

<style scoped>
</style>