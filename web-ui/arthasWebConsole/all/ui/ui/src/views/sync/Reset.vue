<script setup lang="ts">
/**
 * @zh reset 功能比较常用，之后应该装载到header上进行操作
 */
import ClassInput from '@/components/input/ClassInput.vue';
import { reactive } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import { fetchStore } from '@/stores/fetch';
import { interpret } from 'xstate';
import permachine from '@/machines/perRequestMachine';
const fetchS = fetchStore()
const res = reactive(new Map())

const resetClass = (data: { classItem: Item }) => {
  fetchS.baseSubmit(interpret(permachine), {
    action: "exec",
    command: `reset ${data.classItem.value as string}`
  }).then(response => {
    const result = (response as CommonRes).body.results[0]
    if (result.type === "reset") {
      Object.entries(result.affect).forEach(([k, v]) => {
        res.set(k, k === "cost" ? [`${v}ms`] : [v])
      })
    }
  })
}
</script>

<template>
  <ClassInput :submit-f="resetClass"></ClassInput>
  <CmdResMenu title="reset affect" open :map="res" v-if="res.size > 0"></CmdResMenu>
</template>
