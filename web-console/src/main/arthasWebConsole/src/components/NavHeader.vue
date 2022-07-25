<script setup lang="ts">
import { useMachine } from '@xstate/vue';
import { onBeforeMount, ref, watchEffect } from 'vue';
import { assign, createMachine, DoneInvokeEvent } from 'xstate';
import { fetchStore } from '../stores/fetch'
import { RefreshIcon } from '@heroicons/vue/outline';
import machine from "@/machines/consoleMachine"
import { stat } from 'fs';

// const store = fetchStore()
// interface CTX{
//   version: string,
//   request: Request,
//   response: ArthasRes,
//   err: string,
// }
// const context = {
//   version: 'N/A',
//   request: {},
//   response: {},
//   err: '',
// } as CTX

// // 声明状态机

// const machine = createMachine<CTX>({
//   id: 'header',
//   initial: 'idle',
//   context,
//   states: {
//     idle: {
//       on: {
//         FETCH: {
//           target: 'loading',
//           actions: assign({
//             request: () => (store.getRequest({
//               "action": "exec",
//               "command": "version",
//             }))
//           }
//           )
//         } as any
//       }
//     },
//     loading: {
//       invoke: {
//         id: 'getData',
//         src: context => fetch(context.request).then(res => res.json(), err => Promise.reject(err)),
//         onDone: {
//           target: 'success',
//           actions: assign({
//             response: (context, event) => {
//               console.log(event.data, context.request)
//               return event.data
//             }
//           })
//         },
//         onError: {
//           target: 'failure',
//           actions: assign({
//             err: (context, event) => {
//               console.log(event.data)
//               return event.data
//             }
//           })
//         }
//       },
//     },
//     success: {
//       entry: [(context, event) => {
//         console.log(state.value.context)
//         context.version = context.response.body.results[0].version as string
//       }],
//       type: 'final'
//     },
//     failure: {
//       entry: [(context, event) => {
//         console.log(context.err)
//         context.version = 'N/A'
//       }],
//       type: 'final'
//     }
//   }
// })
// let state: any, send
const { state, send } = useMachine(machine)
const version = ref("N/A")
const vCmd = JSON.stringify({
  "action": "exec",
  "command": "version",
})
watchEffect(() => {
  if (state.value.context.response) {
    // 直接遍历以后会有性能问题
    state.value.context.resArr.forEach(res => {
      if (res.type == "version") version.value = res.version
    })
    // if(state.value.context.response.body?.command === "version") version.value = state.value.context.response.body.command
  }
})
onBeforeMount(() => {

  send({
    type: "SUBMIT",
    value: vCmd
  })
})
// 手动重来
const reset = () => send({
  type: "SUBMIT",
  value: vCmd
})
</script>

<template>
  <nav class=" h-[10vh] flex justify-between items-center min-h-max border-b-2 shadow-orange-300">
    <div class="w-40 flex items-center justify-center">
      <img src="@/assets/arthas.png" alt="logo" class=" w-3/4" />
    </div>
    <div class="flex items-center h-20">
      <button class=" rounded-full bg-gray-200 h-12 w-12 flex justify-center items-center mr-4 " @click="reset">
        <refresh-icon class=" text-gray-500 h-3/4 w-3/4 animate-spin-rev-pause" />
      </button>
      <div class=" mr-4 bg-gray-200 h-12 w-32 rounded-full flex justify-center items-center text-gray-500 font-bold">
        version:{{ version }}
      </div>
    </div>
  </nav>
</template>
<style scoped>
</style>