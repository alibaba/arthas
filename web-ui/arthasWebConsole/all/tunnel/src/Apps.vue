<script setup lang="ts">
import { onMounted, reactive } from 'vue';

const apps: string[] = reactive([])
function fetchMyApps() {
  fetch('/api/tunnelApps')
    .then((response) => response.json())
    .then((data: string[]) => {
      data.length > 0 && data.forEach(app => apps.push(app))
    })
    .catch((error) => {
      console.error('api error ' + error)
    })
}
function detailLink(appName: string) {
  return "agents.html?app=" + appName;
}
onMounted(() => {
  fetchMyApps()
})
</script>

<template>
  <table class="table w-[100vw] table-normal">
    <thead>
      <tr>
        <th class="normal-case">name of application</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="app in apps" :key="app" class="hover">
        <td>
          <a class="btn btn-primary btn-sm normal-case" :href="detailLink(app)">{{ app }}</a>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<style scoped>

</style>