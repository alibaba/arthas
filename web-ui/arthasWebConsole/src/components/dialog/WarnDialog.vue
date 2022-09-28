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
  import {
    ExclamationCircleIcon
  } from "@heroicons/vue/outline"
  import { onMounted, ref, onBeforeMount } from 'vue';
  const store = publicStore()
  
  
  function setIsOpen() {
    store.warningFn()
    store.isWarn = false
  }
  function setIsOpenCancel() {
    store.isWarn = false
  }
  </script>
    
  <template>
    <TransitionRoot as="template" :show="true">
      <Dialog @close="setIsOpenCancel" class="min-w-max min-h-max z-20">
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
                <ExclamationCircleIcon class="w-12 h-12 text-red-500" />
              </DialogTitle>
              <DialogDescription 
              class="flex-auto self-stretch bg-slate-50 my-10 rounded p-2 break-all max-w-4xl overflow-auto"
              >
                {{store.warnMessage}}
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