<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { useInterpret, useMachine } from '@xstate/vue';
import { onBeforeMount, ref } from 'vue';
import { interpret } from 'xstate';
import { waitFor } from 'xstate/lib/waitFor';
import AutoComplete from './AutoComplete.vue';
const { label = "inputClassName" } = defineProps<{
  label?: string,
  submitF: (classItem: Item, methodItem: Item) => void
}>()
const { getCommonResEffect } = publicStore()
// const searchClass = useMachine(machine)
const fetchS = fetchStore()
const optionClass = ref([] as { name: string, value: string }[])
const optionMethod = ref([] as { name: string, value: string }[])
// onBeforeMount(() => {
//   searchClass.send("INIT")
// })

// getCommonResEffect(searchClass, body => {
//   optionClass.value.length = 0
//   const result = body.results[0]
//   if (result.type === "sc" && !result.detailed && !result.withField) {
//     result.classNames.forEach(name => {
//       optionClass.value.push({
//         name,
//         value: name
//       })
//     })
//   } else if (result.type === "sm") {
//     optionMethod.value.length = 0
//     body.results.forEach(result => {
//       if (result.type === "sm") {

//         const name = result.methodInfo.methodName
//         optionMethod.value.push({
//           name,
//           value: name
//         })
//       }
//     })
//   }
// })

// const changeClass = (value: string) => {
//   const searchClass = interpret(permachine)
//   if (searchClass.state.value.matches("ready")) {
//     searchClass.send({
//       type: "SUBMIT",
//       value: {
//         action: "exec",
//         command: `sc *${value}*`
//       }
//     })
//   }
// }
const changeClass = (value: string) => {
  const searchClass = interpret(permachine)
  if (value.length > 2) {
    fetchS.baseSubmit(searchClass, {
      action: "exec",
      command: `sc *${value}*`
    }).then(
      res => {
        optionClass.value.length = 0
        let result = (res as CommonRes).body.results[0]
        if (result.type === "sc" && !result.detailed && !result.withField) {
          result.classNames.forEach(name => {
            optionClass.value.push({
              name,
              value: name
            })
          })
        }
      }
    )
  }

}
const changeMethod = (classV: string, value: string) => {
  const searchMethod = interpret(permachine)
  fetchS.baseSubmit(searchMethod, {
    action: "exec",
    command: `sm ${classV} *${value}*`
  }).then(
    res => {
      optionMethod.value.length = 0
      ;(res as CommonRes).body.results.forEach(result => {
          if (result.type === "sm") {

            const name = result.methodInfo.methodName
            optionMethod.value.push({
              name,
              value: name
            })
          }
      })
    }
  )
  // if (searchClass.state.value.matches("ready")) {
  //   searchClass.send({
  //     type: "SUBMIT",
  //     value: {
  //       action: "exec",
  //       command: `sm ${classV} *${value}*`
  //     }
  //   })
  // }
}
// const initMethod = async (classV: string) => {
//   // await waitFor(searchClass.service, state => state.matches("ready"))
//   // searchClass.send({
//   //   type: "SUBMIT",
//   //   value: {
//   //     action: "exec",
//   //     command: `sm ${classV}`
//   //   }
//   // })
//   changeMethod(classV,"")
// }
const filterfn = (_: any, item: Item) => true
</script>

<template>
  <AutoComplete :label="label" :option-items="optionClass" :input-fn="changeClass" :filter-fn="filterfn"
    v-slot="slotClass">
    <AutoComplete label="input Method" :option-items="optionMethod"
      :input-fn="(value) => changeMethod(slotClass.selectItem.value as string, value)" :filter-fn="filterfn"
      v-slot="slotMethod">
      <slot name="others"></slot>
      <button @click.prevent="submitF(slotClass.selectItem, slotMethod.selectItem)"
        class="border bg-blue-400 p-2 rounded-md mx-2 hover:opacity-50 transition">submit</button>
    </AutoComplete>

  </AutoComplete>
</template>