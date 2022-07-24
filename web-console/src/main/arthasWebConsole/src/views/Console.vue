<script setup lang="ts">
import { fetchStore } from '@/stores/fetch';
import { ref } from 'vue';
import { assign, createMachine, DoneInvokeEvent } from 'xstate';
import { useMachine } from '@xstate/vue';
import machine from '@/machines/consoleMachine';
const store = fetchStore();

const val = ref(JSON.stringify({
  action: "exec",
  command: "version",
}));

// const submit = () => {
//   const req = store.getRequest(JSON.parse(val.value))
//   fetch(req).then(res=>console.log(res))
// };

const { state, send } = useMachine(machine)
// send({type:"INIT",})
</script>

<template>
  <div class="flex flex-col">
    <div class="h-[10vh] flex items-center border shadow">
      <label for="command-input" class=" m-2 ">command:</label>
      <div class=" flex-auto grid place-items-start">
        <input type="text" placeholder="input command" v-model="val" id="command-input"
          class=" outline-1 focus-visible:outline-gray-600 border rounded hover:shadow h-10 transition w-11/12 box-border">
      </div>
      <button class="hover:shadow w-24 h-10 border rounded-md  mr-20 " @click="() => send({ type: 'SUBMIT', value: val })">
        submit
      </button>
    </div>
    <article class="flex-1 bg-white overflow-auto max-h-[80vh]">
      <section v-for="(v, i) in state.context.resArr" :key="v.jobId"
        class="w-full  rounded-sm mb-2 p-2 bg-green-200 box-border break-all">
        {{ JSON.stringify(v) }}
      </section>
    </article>
  </div>

</template>

<style scoped>
</style>·