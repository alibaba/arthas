<script setup lang="ts">
import { publicStore } from '@/stores/public'
import {
  Dialog,
  DialogPanel,
  DialogTitle,
  DialogDescription,
  TransitionChild,
  TransitionRoot
} from '@headlessui/vue'
import { onMounted, ref } from 'vue';
const store = publicStore()

const inputV = ref("")
onMounted(() => {
  inputV.value = store.inputVal
})


function setIsOpen() {
  store.inputVal = inputV.value
  store.inputE = true
  store.isInput = false
}
function setIsOpenCancel() {
  store.inputE = false
  store.isInput = false
}
</script>
  
<template>
  <TransitionRoot as="template" :show="true">
    <Dialog @close="setIsOpenCancel" class="min-w-max z-20">
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
              input value
            </DialogTitle>
            <DialogDescription class=" bg-slate-200 my-10 rounded-full w-full flex justify-center px-4">
              <input type="text" v-model="inputV" class="bg-slate-200 h-full p-2 w-full focus-visible:outline-none"/>
            </DialogDescription>
            <div class="flex justify-evenly w-full">
              <button @click="setIsOpen"
                class="btn btn-primary rounded-xl">OK</button>
              <button @click="setIsOpenCancel"
                class="btn btn-primary rounded-xl">Cancel</button>
            </div>
          </DialogPanel>
        </TransitionChild>
      </div>
    </Dialog>
  </TransitionRoot>
</template>