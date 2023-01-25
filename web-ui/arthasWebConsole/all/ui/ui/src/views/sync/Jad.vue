<script setup lang="ts">
import 'highlight.js/lib/common';
import highlightjsP from "@highlightjs/vue-plugin";
import ClassInput from '@/components/input/ClassInput.vue';
import { useMachine } from '@xstate/vue';
import machine from '@/machines/consoleMachine';
import { onBeforeMount, reactive, ref } from 'vue';
import { publicStore } from '@/stores/public';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
const { getCommonResEffect } = publicStore()
const highlightjs = highlightjsP.component
const sourceM = useMachine(machine)
const code = ref('')
const locationMap = reactive(new Map<string, string[]>())
const getSource = (data: { classItem: Item; }) => {
  sourceM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: `jad ${data.classItem.value}`
    }
  })
}
getCommonResEffect(sourceM, body => {
  const result = body.results[0]

  if (result.type === "jad") {
    code.value = result.source
    locationMap.clear()

    locationMap.set('location', [result.location])
    Object.entries(result.classInfo).forEach(([k, v]) => {
      let value: string[] = []
      if (k === "classloader") value = v as string[]
      else value.push(v.toString())
      locationMap.set(k, value)
    })
  }
})
onBeforeMount(() => {
  sourceM.send("INIT")
})
</script>
<template>
  <div class="mb-4">
    <ClassInput :submit-f="getSource"></ClassInput>
  </div>
  <div v-if="code !== ''">
    <CmdResMenu title="classInfo" :map="locationMap"   class="mb-4"></CmdResMenu>
    <div class="w-10/12 rounded-xl border p-4 bg-[#f6f6f6] hover:shadow-gray-400 mx-auto shadow-lg transition mb-4">
      <highlightjs language="Java" :code="code" />
    </div>
  </div>
</template>

<style scoped>
</style>