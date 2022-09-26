<script setup lang="ts">

  import { Menu, MenuButton, MenuItems, MenuItem } from '@headlessui/vue'
  defineProps<{
    title:string
  }>()

</script>
<template>
  <Menu>
    <MenuButton class="bg">{{title}}</MenuButton>
    <MenuItems>
      <MenuItem v-slot="{ active }">
        <a :class='{ "bg-blue-500": active }' href="/account-settings">
          Account settings
        </a>
      </MenuItem>
      <MenuItem v-slot="{ active }">
        <a :class='{ "bg-blue-500": active }' href="/account-settings">
          Documentation
        </a>
      </MenuItem>
      <MenuItem disabled>
        <span class="opacity-75">Invite a friend (coming soon!)</span>
      </MenuItem>
    </MenuItems>
  </Menu>
</template>

