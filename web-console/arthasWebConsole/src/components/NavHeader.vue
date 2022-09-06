<script setup lang="ts">
import { useMachine } from '@xstate/vue';
import { waitFor } from 'xstate/lib/waitFor'
import { onBeforeMount, Ref, ref, watchEffect } from 'vue';
import { RefreshIcon, LogoutIcon, LoginIcon } from '@heroicons/vue/outline';
import { fetchStore } from '@/stores/fetch';
import machine from "@/machines/consoleMachine"
import { publicStore } from '@/stores/public';
const fetchM = useMachine(machine)
const {getCommonResEffect } = publicStore()
const { state, send } = fetchM
const fetchS = fetchStore()
const sessionM = useMachine(machine)
const version = ref("N/A")
const vCmd: CommandReq = {
  action: "exec",
  command: "version"
}

const restBtnclass: Ref<'animate-spin-rev-pause' | 'animate-spin-rev-running'> = ref('animate-spin-rev-pause')
getCommonResEffect(fetchM,body=>{
  const result = body.results[0]
  if(result.type === "version") version.value = result.version
})

watchEffect(() => {
  if (!fetchS.wait) restBtnclass.value = "animate-spin-rev-pause"
  else restBtnclass.value = "animate-spin-rev-running"
})
onBeforeMount(() => {
  send("INIT")
  send({
    type: "SUBMIT",
    value: vCmd
  })
  sessionM.send("INIT")
})
// 手动重来
const reset = () => {
  send({
    type: "SUBMIT",
    value: vCmd
  })
}
const interruptEvent = ()=>{
  fetchS.interruptJob()
}

const logout = async () => {

  restBtnclass.value = "animate-spin-rev-running"
  
  interruptEvent()

  sessionM.send("SUBMIT", {
    value: {
      action: "close_session"
    }
  })
  restBtnclass.value = "animate-spin-rev-pause"
}
const login = async () => {
  sessionM.send("SUBMIT", {
    value: {
      action: "init_session"
    }
  })
}

</script>

<template>
  <nav class=" h-[10vh] flex justify-between items-center min-h-max border-b-2 shadow-orange-300">
    <div class="w-40 flex items-center justify-center">
      <img src="@/assets/arthas.png" alt="logo" class=" w-3/4" />
    </div>
    <div class="flex items-center h-20">
      <button
        v-if="fetchS.jobRunning"
        @click.prevent="interruptEvent"
        class="bg-red-600 text-white h-1/2 p-2 rounded hover:bg-red-400 transition mr-4"
      >interrupt</button>
      <button class=" rounded-full bg-gray-200 h-12 w-12 flex justify-center items-center mr-4 " @click="reset">
        <refresh-icon class=" text-gray-500 h-3/4 w-3/4" :class="restBtnclass" />
      </button>
      <div class=" mr-4 bg-gray-200 h-12 w-32 rounded-full flex justify-center items-center text-gray-500 font-bold">
        version:{{ version }}
      </div>
      <button class="hover:opacity-50 h-12 w-12 grid place-items-center  rounded-full mr-2 transition-all"
        :class="{ 'bg-blue-600 shadow-blue-500': !fetchS.online, 'bg-red-600 shadow-red-500': fetchS.online }">
        <LogoutIcon class="h-1/2 w-1/2 text-white" @click="logout" v-if="fetchS.online" />
        <login-icon class="h-1/2 w-1/2 text-white" @click="login" v-else />
      </button>
    </div>
  </nav>
</template>
<style scoped>
</style>