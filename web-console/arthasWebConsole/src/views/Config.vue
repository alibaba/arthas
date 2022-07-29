<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useMachine } from '@xstate/vue';
import { onBeforeMount } from 'vue';
const { state, send } = useMachine(machine)
onBeforeMount(() => {
  send({ type: "INIT" })


})
</script>

<template>
  <div>
    <article>
      <section>
        
      </section>
    </article>
  </div>
</template>

<style scoped>
</style>