<script setup lang="ts">
import NavAside from "@/components/routeTo/NavAside.vue";
import NavHeader from "@/components/NavHeader.vue";
import ErrDialog from "@/components/dialog/ErrDialog.vue";
import SuccessDialog from "@/components/dialog/SuccessDialog.vue";
import { onBeforeUnmount } from "vue";
import machine from "./machines/consoleMachine";
import { interpret } from "xstate";
import { fetchStore } from "./stores/fetch";
const fetchS = fetchStore()
onBeforeUnmount(()=>{
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
</script>

<template>
  <div class=" h-screen flex flex-col">
    <nav-header class="h-[10vh]"></nav-header>
    <div class=" flex-auto h-[90vh] overflow-auto" :class='{"pointer-events-none":fetchS.jobRunning}'>
      <div class="flex flex-row h-full">
        <nav-aside></nav-aside>
        <div class="flex-auto overflow-auto h-[90vh]">
        <router-view >
        </router-view>
        </div>
      </div>
    </div>
    <err-dialog/>
    <success-dialog/>
  </div>
  
</template>
