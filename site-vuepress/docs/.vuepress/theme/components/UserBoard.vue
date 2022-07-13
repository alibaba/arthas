<template>
  <img class="users-logo" :src="img.logo" :title="img.name" />
</template>

<script setup>
defineProps({
  img: {
    type: Object,
    required: true,
  },
});
</script>

<style lang="scss" scoped>
.users-logo {
  width: 140px;
  margin: 10px;
}
</style>
