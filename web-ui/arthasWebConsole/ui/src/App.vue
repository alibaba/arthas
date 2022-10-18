 <script setup lang="ts">
// import NavAside from "@/components/routeTo/NavAside.vue";
import NavHeader from "@/components/NavHeader.vue";
import ErrDialog from "@/components/dialog/ErrDialog.vue";
import SuccessDialog from "@/components/dialog/SuccessDialog.vue";
import { onBeforeUnmount } from "vue";
import { fetchStore } from "./stores/fetch";
import InputDialog from "./components/dialog/InputDialog.vue";
import { publicStore } from "./stores/public";
import WarnDialog from "./components/dialog/WarnDialog.vue";
const fetchS = fetchStore()
const publicS = publicStore()
onBeforeUnmount(() => {
  fetchS.interruptJob()
})
</script>
<!-- dialog用v-if方便触发hooks -->
<template>
  <div class=" h-screen flex flex-col">
    <nav-header class="h-[10vh]"></nav-header>
    <div class=" flex-auto h-[90vh] overflow-auto">
      <div class="flex flex-row h-full">
        <!-- <nav-aside ></nav-aside> -->
        <div class="flex-auto overflow-auto h-[90vh] w-[90vw]">
          <router-view>
          </router-view>
        </div>
      </div>
    </div>

  </div>
  <err-dialog v-if="publicS.isErr" />
  <success-dialog v-if="publicS.isSuccess" />
  <input-dialog v-if="publicS.isInput " />
  <warn-dialog v-if="publicS.isWarn"></warn-dialog>
</template>
