<script setup lang="ts">
import NavAside from "./components/NavAside.vue";
import NavHeader from "./components/NavHeader.vue";
import ErrDialog from "./components/ErrDialog.vue";
import SuccessDialog from "./components/SuccessDialog.vue";

</script>

<template>
  <div class=" h-screen flex flex-col">
    <nav-header class="h-[10vh]"></nav-header>
    <div class=" flex-auto h-[90vh] overflow-auto">
      <div class="flex flex-row h-full">
        <nav-aside></nav-aside>
        <router-view class=" flex-auto">
        </router-view>
      </div>
    </div>
    <err-dialog/>
    <success-dialog/>
  </div>
  
</template>

<style>
/* #app { */
/* font-family: Avenir, Helvetica, Arial, sans-serif; */
/* -webkit-font-smoothing: antialiased; */
/* -moz-osx-font-smoothing: grayscale; */
/* text-align: center; */
/* color: #2c3e50; */
/* margin-top: 60px; */
/* } */
</style>
