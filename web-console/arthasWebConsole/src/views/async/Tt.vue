<script setup lang="ts">
import MethodInput from '@/components/input/MethodInput.vue';
const submit = (val:Item)=>{
  console.log(val)
}
</script>

<template>
<MethodInput :submit-f="submit"></MethodInput>
</template>

<style scoped>

</style>