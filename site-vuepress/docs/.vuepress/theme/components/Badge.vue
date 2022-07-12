<template>
  <div class="my-badge">
    <component :is="comp" />
    &nbsp;
    <span>{{ data }}</span>
  </div>
</template>

<script setup>
defineProps({
  comp: {
    type: Object,
    required: true,
  },
  data: {
    type: Number,
    required: true,
  },
});
</script>

<style scoped>
.my-badge {
  width: fit-content;
  width: -webkit-fit-content;
  width: -moz-fit-content;
  padding: 5px;
  margin: 0 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 5px;
  background-color: rgba(89, 95, 101, 0.2);
}

@media (max-width: 719px) {
  .my-badge {
    margin: 0 5px;
  }
}
</style>
