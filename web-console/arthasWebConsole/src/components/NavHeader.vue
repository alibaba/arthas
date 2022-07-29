<script setup lang="ts">
import { useMachine } from '@xstate/vue';
import { onBeforeMount, reactive, Ref, ref, watchEffect } from 'vue';
import { RefreshIcon, LogoutIcon, LoginIcon } from '@heroicons/vue/outline';
import { fetchStore } from '@/stores/fetch';
import Menu from '@/components/Menu.vue';
import machine from "@/machines/consoleMachine"

const { state, send } = useMachine(machine)
const fetchS = fetchStore()
const version = ref("N/A")
const vCmd: CommandReq = {
  action: "exec",
  command: "version",
  sessionId: undefined,
  consumerId: undefined,
  requestId: undefined,
  execTimeout: undefined
}
const restBtnclass: Ref<'animate-spin-rev-pause' | 'animate-spin-rev-running'> = ref('animate-spin-rev-pause')
watchEffect(() => {
  console.log(state.value.context)
  if (state.value.context.response) {
    // 直接遍历以后会有性能问题
    state.value.context.resArr.forEach(res => {
      if ('type' in res && res.type == "version") version.value = res.version
    })
    // if(state.value.context.response.body?.command === "version") version.value = state.value.context.response.body.command
  }
  if (state.value.matches("loading")) restBtnclass.value = "animate-spin-rev-running"
  else restBtnclass.value = "animate-spin-rev-pause"
})
onBeforeMount(() => {
  send({ type: 'INIT' })

  send({
    type: "SUBMIT",
    value: vCmd
  })
})
// 手动重来
const reset = () => send({
  type: "SUBMIT",
  value: vCmd
})

// 关闭session

const logout = () => send({
  type: "SUBMIT",
  value: {
    action: "close_session",
    sessionId: undefined
  }
})
const login = () => send({
  type: "SUBMIT",
  value: {
    action: "init_session"
  }
})
</script>

<template>
  <nav class=" h-[10vh] flex justify-between items-center min-h-max border-b-2 shadow-orange-300">
    <div class="w-40 flex items-center justify-center">
      <img src="@/assets/arthas.png" alt="logo" class=" w-3/4" />
    </div>
    <div class="flex items-center h-20">
      <button class=" rounded-full bg-gray-200 h-12 w-12 flex justify-center items-center mr-4 " @click="reset">
        <refresh-icon class=" text-gray-500 h-3/4 w-3/4" :class="restBtnclass" />
      </button>
      <Menu>
        
      </Menu>
      <div class=" mr-4 bg-gray-200 h-12 w-32 rounded-full flex justify-center items-center text-gray-500 font-bold">
        version:{{ version }}
      </div>
      <button class="h-12 w-12 grid place-items-center bg-red-600 shadow-red-500 rounded-full mr-2 transition-all"
        :class=" {'bg-blue-600':!fetchS.online, 'shadow-blue-500':!fetchS.online}"
      >
        <LogoutIcon class="h-1/2 w-1/2 text-white" @click="logout" v-if="fetchS.online" />
        <login-icon class="h-1/2 w-1/2 text-white" @click="login" v-else/>
      </button>
    </div>
  </nav>
</template>
<style scoped>
</style>