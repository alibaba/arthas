<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { onBeforeMount, ref } from 'vue';
import { interpret } from 'xstate';
import AutoComplete from './AutoComplete.vue';
const { label = "inputClassName" } = defineProps<{
  label?: string,
  submitF: (item: Item) => void
}>()
const { getCommonResEffect } = publicStore()
// const searchClass = useMachine(machine)
// const searchClass = useInterpret(permachine)
const optionClass = ref([] as { name: string, value: string }[])
const fetchS = fetchStore()
const changeValue = (value: string) => {
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
const filterfn = (_: any, item: Item) => true
</script>

<template>
  <AutoComplete :label="label" :option-items="optionClass" :input-fn="changeValue" :filter-fn="filterfn" v-slot="slotP"
    as="form">
    <button @click.prevent="submitF(slotP.selectItem)"
      class="border bg-blue-400 p-2 rounded-md mx-2 hover:opacity-50 transition">submit</button>
  </AutoComplete>
</template>
