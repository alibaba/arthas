<template>
<!--  <img alt="Vue logo" src="./assets/logo.png">-->
  <DemoUI/>
  <router-view></router-view>


</template>

<script>
import DemoUI from './components/DemoUI.vue'

export default {
  name: 'App',
  components: {
    DemoUI
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px;
}
</style>
