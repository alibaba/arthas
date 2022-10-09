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
  <!-- <div class="flex items-center w-full justify-center flex-col"> -->
    <!-- <h3 class=" text-2xl mb-2">enchance</h3> -->
    <!-- <ul class="flex">
    <li 
    v-for="(kv,i) in enhancer.entries()" :key="i" 
    class="flex flex-col mx-1">
      <div class="bg-orange-300 p-2 rounded-b">{{kv[0]}}</div>
      <div class=" bg-orange-100 grid place-items-center">{{kv[1]}}</div>
    </li>
  </ul> -->
    <!-- <table class="table table-compact group w-full">
      <thead>
        <tr>
          <th v-for="(k,i) in enhancer.keys()" :key="i" class='border border-slate-300'
            :class="{'group-first:z-0':i==0}">{{k}}</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td v-for="(v, i) in enhancer.values()" class="border border-slate-300">
            {{v}}
          </td>
        </tr>
      </tbody>
    </table> -->
    <div class="stats shadow my-2 mx-auto">

      <div class="stat place-items-center" v-for="(kv,i) in enhancer.entries()" :key="i" >
        <div class="stat-title">{{kv[0]}}</div>
        <div class="stat-value">{{kv[1]}}</div>
      </div>

    </div>
  <!-- </div> -->
</template>

<style scoped>

</style>