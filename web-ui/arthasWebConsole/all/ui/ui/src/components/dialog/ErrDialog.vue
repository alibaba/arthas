<script setup lang="ts">
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public'
import {
  Dialog,
  DialogPanel,
  DialogTitle,
  DialogDescription,
  TransitionChild,
  TransitionRoot
} from '@headlessui/vue'
import { ExclamationCircleIcon } from '@heroicons/vue/outline';
import { onBeforeMount } from 'vue';
const store = publicStore()

function setIsOpen(value: boolean) {
  store.isErr = value
}
onBeforeMount(()=>{fetchStore().curPolling.close()})
</script>

<template>
  <TransitionRoot :show="true" as="template">
    <Dialog @close="setIsOpen" class="min-w-max z-20">
      <TransitionChild enter="transition-opacity duration-300" enter-from="opacity-0" enter-to="opacity-100"
        leave="transition-opacity duration-300" leave-from="opacity-100" leave-to="opacity-0">
        <div class="fixed inset-0 bg-black bg-opacity-25 z-20" />
      </TransitionChild>
      <div class="fixed inset-0 grid place-items-center min-w-max z-30">
        <TransitionChild as="template" enter="duration-300 ease-out" enter-from="opacity-0 scale-95"
          enter-to="opacity-100 scale-100" leave="duration-200 ease-in" leave-from="opacity-100 scale-100"
          leave-to="opacity-0 scale-95">

          <DialogPanel
            class=" w-1/3 h-1/2 bg-base-100 p-10 rounded-xl shadow-xl flex flex-col justify-between items-center min-w-max">
            <DialogTitle>
              <ExclamationCircleIcon class="w-12 h-12 text-error" />
            </DialogTitle>
            <DialogDescription as="section"
              class="flex-auto self-stretch my-10 rounded p-2 break-all max-w-4xl overflow-auto">
              reason:
              {{ store.ErrMessage }}
            </DialogDescription>

            <button @click="setIsOpen(false)"
              class="btn btn-primary rounded-xl">OK</button>
          </DialogPanel>
        </TransitionChild>
      </div>
    </Dialog>
  </TransitionRoot>
</template>
