<script setup lang="ts">

import { ref } from 'vue';
import { useMachine } from '@xstate/vue';
import machine from '@/machines/consoleMachine';
import { publicStore } from '@/stores/public';
import transformMachine from "@/machines/transformConfigMachine"
const store = publicStore()

const val = ref(JSON.stringify({
  // action: "exec",
  // command: "version",
  action:"init_session"
}));
// const submit = () => {
//   const req = store.getRequest(JSON.parse(val.value))
//   fetch(req).then(res=>console.log(res))
// };
// const notJson = Symbol('')
const { state, send } = useMachine(machine)
send({type:"INIT"})

// const transformInput = (val: string):(object|typeof notJson) => {
//   try {
//     return JSON.parse(val)
//   } catch {
//     return notJson
//   }
// }

const submitCommand = ()=>{
  // const input = transformInput(val.value)
  // if (input === notJson) {
  //   //todo
  //   store.isErr = true
  //   store.$patch({
  //     isErr:true,
  //     ErrMessage: "不是json格式的数据"
  //   })
  //   return
  // }
  const toObj = useMachine(transformMachine)
  toObj.send("INIT")
  toObj.send({type:"INPUT", data: val.value})
  toObj.send("TRANSFORM")
  // input 不是arthasReq的报错还没写
  if(toObj.state.value.matches("success")) send({ type: 'SUBMIT', value: toObj.state.value.context.output as ArthasReq})
}
</script>

<template>
  <div class="flex flex-col">
    <form class="h-[10vh] flex items-center border shadow">
      <label for="command-input" class=" m-2 ">command:</label>
      <div class=" flex-auto grid place-items-start">
        <input type="text" placeholder="input command" v-model="val" id="command-input"
          class=" outline-1 focus-visible:outline-gray-600 border rounded hover:shadow h-10 transition w-11/12 box-border">
      </div>
      <button class="hover:shadow w-24 h-10 border rounded-md  mr-20 "
        @click="submitCommand">
        submit
      </button>
    </form>
    <article class="flex-1 bg-white overflow-auto max-h-[80vh]">
      <section v-for="(v, i) in state.context.resArr" :key="i"
        class="w-full  rounded-sm mb-2 p-2 bg-green-200 box-border break-all"
        :class="{ 'bg-blue-200': v&&!Object.hasOwn(v, 'jobId')}"
        >
        {{ JSON.stringify(v) }}
      </section>
    </article>
  </div>

</template>

<style scoped>
</style>·