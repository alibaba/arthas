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
import { onMounted, ref, onBeforeMount } from 'vue';
const store = publicStore()
const debug = (e: any) => console.log(e)

const inputV = ref("")
onMounted(() => {
  inputV.value = store.inputVal
})


function setIsOpen() {
  store.inputVal = inputV.value
  console.log(store.inputVal, inputV.value)
  store.isInput = false
}
function setIsOpenCancel() {
  store.isInput = false
}
</script>
  
<template>
  <TransitionRoot as="template" :show="true">
    <Dialog @close="setIsOpenCancel" class="min-w-max">
      <TransitionChild enter="transition-opacity duration-300" enter-from="opacity-0" enter-to="opacity-100"
        leave="transition-opacity duration-300" leave-from="opacity-100" leave-to="opacity-0">
        <div class="fixed inset-0 bg-black bg-opacity-25" />
      </TransitionChild>
      <div class="fixed inset-0 grid place-items-center min-w-max">
        <TransitionChild as="template" enter="duration-300 ease-out" enter-from="opacity-0 scale-95"
          enter-to="opacity-100 scale-100" leave="duration-200 ease-in" leave-from="opacity-100 scale-100"
          leave-to="opacity-0 scale-95">

          <DialogPanel
            class=" w-1/3 h-1/2 bg-white p-10 rounded-xl shadow-xl flex flex-col justify-between items-center min-w-max">
            <DialogTitle>
              input value
            </DialogTitle>
            <DialogDescription class=" bg-slate-200 my-10 rounded-full max-w-4xl grid place-content-center place-items-center">
              <input type="text" v-model="inputV" class="bg-slate-200 h-full p-2 w-10/12 focus-visible:outline-none"/>
            </DialogDescription>
            <div class="flex justify-evenly w-full">
              <button @click="setIsOpen"
                class="border bg-blue-200 w-40 h-10 rounded-full hover:bg-blue-500 transition">OK</button>
              <button @click="setIsOpenCancel"
                class="border bg-blue-200 w-40 h-10 rounded-full hover:bg-blue-500 transition">Cancel</button>
            </div>
          </DialogPanel>
        </TransitionChild>
      </div>
    </Dialog>
  </TransitionRoot>
</template>