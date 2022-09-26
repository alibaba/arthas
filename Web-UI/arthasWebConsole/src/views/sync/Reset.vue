<script setup lang="ts">
/**
 * @zh reset 功能比较常用，之后应该装载到header上进行操作
 */
import ClassInput from '@/components/input/ClassInput.vue';
import machine from '@/machines/consoleMachine';
import { publicStore } from '@/stores/public';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, reactive } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
const fetchM = useMachine(machine)
const {getCommonResEffect} = publicStore()
const res = reactive(new Map())
onBeforeMount(()=>{
  fetchM.send("INIT")
})
getCommonResEffect(fetchM,body=>{
  const result = body.results[0]
  res.clear()
  if(result.type === "reset"){
    Object.entries(result.affect).forEach(([k,v])=>{
      res.set(k,k === "cost"? [`${v}ms`]:[v])
    }) 
  }
})
const resetClass = (data:{classItem:Item})=>{
    fetchM.send({
    type:"SUBMIT",
    value:{
      action:"exec",
      command: `reset ${data.classItem.value as string}`
    }
  })
}
</script>

<template>
  <ClassInput :submit-f="resetClass"></ClassInput>
  <CmdResMenu title="reset affect" open :map="res" v-if="res.size > 0"></CmdResMenu>
</template>

<style scoped>

</style>