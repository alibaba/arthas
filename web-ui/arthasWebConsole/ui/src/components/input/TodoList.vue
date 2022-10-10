<script setup lang="ts">
import { PlusCircleIcon, MinusCircleIcon } from "@heroicons/vue/outline"
import {
  Menu,
  MenuButton,
  MenuItem,
  MenuItems
} from "@headlessui/vue"
import { ref } from "vue";
import { publicStore } from "@/stores/public";
const { valSet = new Set<string>(), getInput = () => true } = defineProps<{
  valSet?: Set<string>,
  getInput?: (raw: string) => boolean,
  title: string
}>()
const publicS = publicStore()

const openInput = publicS.inputDialogFactory(
  ref(""),
  (raw) => {
    if (getInput(raw)) valSet.add(raw)
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
    <MenuButton class=" w-52 hover:shadow-md btn btn-sm btn-outline">{{title}}</MenuButton>
    <MenuItems
      class=" absolute w-52 mt-2 border py-2 rounded-md hover:shadow-xl transition bg-base-100 max-h-80 overflow-y-auto top-[100%]">
      <MenuItem v-slot="{active}">
      <li class="flex justify-center " :class='{" bg-neutral text-neutral-content": active}'>
        <PlusCircleIcon class="w-6 h-6 cursor-pointer" @click="openInput"></PlusCircleIcon>
      </li>

      </MenuItem>
      <template v-if="valSet.size > 0">
        <MenuItem v-slot="{ active, selected }" v-for="(v) in valSet.values()" :key="v">
        <li class="flex w-full justify-between px-2" :class='{"bg-neutral text-neutral-content":
        active, "bg-neutral-focus text-neutral-content" : selected,}'>
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