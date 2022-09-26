<script setup lang="ts">
import { PlusCircleIcon, MinusCircleIcon } from "@heroicons/vue/outline"
import {
  Menu,
  MenuButton,
  MenuItem,
  MenuItems
} from "@headlessui/vue"
import { watchEffect, ref } from "vue";
import { publicStore } from "@/stores/public";
const { valSet = new Set<string>() } = defineProps<{
  valSet?: Set<string>,
  title: string
}>()
const publicS = publicStore()

const openInput = publicS.inputDialogFactory(
  ref(""),
  (raw) => {
    valSet.add(raw)
    return ""
  },
  _ => ""
)


const removeValSet = (val: string, valSet: Set<string>) => {
  valSet.delete(val)
}
</script>

<template>
  <Menu as="div" class=" relative flex items-center">
    <MenuButton class=" w-52 hover:shadow-md input-btn-style">{{title}}</MenuButton>
    <MenuItems
      class=" absolute w-52 mt-2 border py-2 rounded-md hover:shadow-xl transition bg-white max-h-80 overflow-y-auto top-[100%]">
      <MenuItem v-slot="{active}">
      <li class="flex justify-center " :class='{"bg-blue-300":active}'>
        <PlusCircleIcon class="w-6 h-6 cursor-pointer" @click="openInput"></PlusCircleIcon>
      </li>

      </MenuItem>
      <template v-if="valSet.size > 0">
        <MenuItem v-slot="{ active }" v-for="(v) in valSet.values()" :key="v">
        <li class="flex w-full justify-between px-2" :class='{"bg-blue-300":active}'>
          <div>{{v}}</div>
          <MinusCircleIcon class="w-6 h-6 cursor-pointer" @click="removeValSet(v,valSet)"></MinusCircleIcon>
        </li>
        </MenuItem>
      </template>
    </MenuItems>
  </Menu>
</template>

<style scoped>

</style>