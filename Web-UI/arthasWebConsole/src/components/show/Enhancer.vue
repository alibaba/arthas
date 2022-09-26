<script setup lang="ts">
const { result } = defineProps<{
  result: EnchanceResult
}>()
let enhancer = new Map<string, string>()
if (result.type === "enhancer") {
  enhancer.clear();
  enhancer.set("success", result.success.toString());
  for (const k in result.effect) {
    enhancer.set(k, result.effect[k as "cost"].toString());
  }
}
</script>

<template>
  <div class="flex items-center w-full justify-center flex-col">
  <h3 class=" text-2xl mb-2">enchance</h3>
  <ul class="flex">
    <li 
    v-for="(kv,i) in enhancer.entries()" :key="i" 
    class="flex flex-col mx-1">
      <div class="bg-orange-300 p-2 rounded-b">{{kv[0]}}</div>
      <div class=" bg-orange-100 grid place-items-center">{{kv[1]}}</div>
    </li>
  </ul>
</div>
</template>

<style scoped>

</style>