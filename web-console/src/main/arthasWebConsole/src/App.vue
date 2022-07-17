<script setup lang="ts">
import "./components/NavAside.vue"
import NavAside from "./components/NavAside.vue";
</script>

<template>
  <div class="flex flex-row h-screen">
  <nav-aside class=" bg-pink-800"></nav-aside>
  <router-view class=" flex-1 bg-blue-50">
  </router-view>
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
