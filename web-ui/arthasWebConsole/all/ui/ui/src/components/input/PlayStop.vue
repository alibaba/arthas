<script setup lang="ts">
import { ref } from 'vue'
import { Switch } from '@headlessui/vue'
import { PlayIcon, StopIcon } from "@heroicons/vue/solid"
const { playFn = () => { }, stopFn = () => { }, defaultEnabled } = defineProps<{
  playFn?: Function,
  stopFn?: Function,
  defaultEnabled: boolean
}>()
const enabled = ref(defaultEnabled)
const toggle = () => enabled.value ? stopFn() : playFn()
</script>
<template>
  <Switch v-model="enabled"
    class="rounded-full grid place-items-center hover:opacity-50 transition" @click="toggle">
    <PlayIcon v-if="!enabled" class="w-full h-full text-blue-500" />
    <StopIcon v-else class="w-full h-full text-red-500" />
  </Switch>
</template>

