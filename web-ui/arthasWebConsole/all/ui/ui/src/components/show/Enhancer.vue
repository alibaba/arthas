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
    <div class="stats shadow my-2 mx-auto">

      <div class="stat place-items-center" v-for="(kv,i) in enhancer.entries()" :key="i" >
        <div class="stat-title">{{kv[0]}}</div>
        <div class="stat-value">{{kv[1]}}</div>
      </div>
      <slot name="otherStat">

      </slot>
    </div>
</template>

<style scoped>

</style>