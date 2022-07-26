<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { publicStore } from '@/stores/public'
import {
  Dialog,
  DialogPanel,
  DialogTitle,
  DialogDescription,
} from '@headlessui/vue'
import { watch } from 'fs';
const store = publicStore()

function setIsOpen(value: boolean) {
  store.isErr = value
}
watchEffect(()=>{
  console.log(store.isErr)
})
</script>

<template>
  <Dialog :open="store.isErr" @close="setIsOpen" class="absolute top-0 left-0 right-0 bottom-0 bg-sky-600" >
    <DialogPanel class="">
      <DialogTitle>出错了</DialogTitle>
      <DialogDescription>
        reason: {{ store.ErrMessage }}
      </DialogDescription>

      <button @click="setIsOpen(false)">好的</button>
    </DialogPanel>
  </Dialog>
</template>

<style scoped>
</style>