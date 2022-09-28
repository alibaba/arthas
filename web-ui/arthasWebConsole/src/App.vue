<script setup lang="ts">
import NavAside from "@/components/routeTo/NavAside.vue";
import NavHeader from "@/components/NavHeader.vue";
import ErrDialog from "@/components/dialog/ErrDialog.vue";
import SuccessDialog from "@/components/dialog/SuccessDialog.vue";
import { onBeforeUnmount } from "vue";
import machine from "./machines/consoleMachine";
import { interpret } from "xstate";
import { fetchStore } from "./stores/fetch";
import InputDialog from "./components/dialog/InputDialog.vue";
import { publicStore } from "./stores/public";
import WarnDialog from "./components/dialog/WarnDialog.vue";
const fetchS = fetchStore()
const publicS = publicStore()
onBeforeUnmount(() => {
  const actor = interpret(machine)
  actor.start()
  console.log('asdfasdf')
  actor.send("INIT")
  actor.send({
    type: "SUBMIT",
    value: {
      action: "interrupt_job",
    } as AsyncReq
  })
})
// :class='{"pointer-events-none":fetchS.jobRunning}'
</script>

<template>
  <div class=" h-screen flex flex-col relative">
    <nav-header class="h-[10vh]"></nav-header>
    <div class=" flex-auto h-[90vh] overflow-auto" 
>
      <div class="flex flex-row h-full">
        <nav-aside></nav-aside>
        <div class="flex-auto overflow-auto h-[90vh]">
          <router-view>
          </router-view>
        </div>
      </div>
    </div>
      <err-dialog v-if="publicS.isErr"/>
      <success-dialog v-if="publicS.isSuccess"/>
      <input-dialog v-if="publicS.isInput " />
      <warn-dialog v-if="publicS.isWarn"></warn-dialog>
  </div>

</template>
