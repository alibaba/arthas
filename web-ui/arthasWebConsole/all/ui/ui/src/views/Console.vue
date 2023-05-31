<script setup lang="ts">

import { onBeforeMount, ref } from 'vue';
import { useMachine } from '@xstate/vue';
import machine from '@/machines/consoleMachine';
const fetchM = useMachine(machine)
const val = ref(JSON.stringify({
  action: "exec",
  command: "version"
}));


onBeforeMount(()=>{
  fetchM.send("INIT")
})

const submitCommand = ()=>{
  fetchM.send({ type: 'SUBMIT', value: val.value})
}
</script>

<template>
  <div class="flex flex-col ">
    <form class="h-[10vh] flex items-center border shadow" @submit.prevent="submitCommand">
      <label for="command-input" class=" m-2 ">command:</label>
      <div class=" flex-auto grid place-items-start">
        <input type="text" placeholder="input command" v-model="val" id="command-input"
          class=" outline-1 focus-visible:outline-gray-600 border rounded hover:shadow h-10 transition w-11/12 box-border">
      </div>
      <button class="hover:shadow w-24 h-10 border rounded-md  mr-20 "
        >
        submit
      </button>
    </form>
    <article class="flex-1 bg-white overflow-auto max-h-[70vh]">
      <section v-for="(v, i) in fetchM.state.value.context.resArr" :key="i"
        class="w-full  rounded-sm mb-2 p-2 bg-green-200 box-border break-all"
        :class="{ 'bg-blue-200': v&&!Object.hasOwn(v, 'jobId')}"
        >
        {{ JSON.stringify(v) }}
      </section>
    </article>
  </div>

</template>

<style scoped>
</style>