<template>
  <img class="users-logo" :src="img.logo" />
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
  max-width: 140px;
  object-fit: contain;
  margin: 10px;
  user-select: none;
}
</style>
