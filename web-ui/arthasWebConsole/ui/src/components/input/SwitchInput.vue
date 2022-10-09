<script setup lang="ts">
import { onBeforeMount, reactive, ref, watch, watchEffect } from 'vue';
import {
  Switch
} from "@headlessui/vue"
const props = defineProps<{
  data: { key: string, value: boolean | string },
  send: Function
}>()
const modelvalue = ref('' as string | boolean)
watch(props, () => {
  modelvalue.value = props.data.value
  console.log("watch", props.data)
}, {
  deep: true,
  immediate: true
})
onBeforeMount(() => {
  console.log(props.data)
})
</script>

<template>
  <form class="flex justify-between items-center" @submit.prevent="send({ key: data.key, value: modelvalue })">
    <div class="w-1/5 bg-blue-200 p-2 min-w-max">{{ data.key }}</div>
    <div class="w-3/5 grid place-items-center">
      <Switch v-if="typeof data.value === 'boolean'" v-model="(modelvalue as boolean)"
        :class="modelvalue ? 'bg-blue-500' : 'bg-teal-700'"
        class="relative inline-flex h-6 w-11 items-center rounded-full">
        <span class="sr-only">Enable notifications</span>
        <span :class="modelvalue ? 'translate-x-6' : 'translate-x-1'"
          class="inline-block h-4 w-4 transform rounded-full bg-white transition" />
      </Switch>
      <input v-else-if="typeof data.value === 'string'" v-model="(modelvalue as string)" class="rounded-full pl-3 w-1/2"/>
    </div>
    <div class="w-1/5">
      <button 
        class="p-1 w-24 rounded-full bg-blue-300 hover:bg-blue-500 transition text-black">
        change
      </button>
    </div>
  </form>
</template>