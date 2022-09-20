<script setup lang="ts">
import { useMachine } from '@xstate/vue';
import { waitFor } from 'xstate/lib/waitFor'
import { onBeforeMount, Ref, ref, watchEffect } from 'vue';
import { RefreshIcon, LogoutIcon, LoginIcon } from '@heroicons/vue/outline';
import { fetchStore } from '@/stores/fetch';
import machine from "@/machines/consoleMachine"
import { publicStore } from '@/stores/public';
import { interpret } from 'xstate';
import permachine from '@/machines/perRequestMachine';
const fetchM = useMachine(machine)
const publicS = publicStore()
const { state, send } = fetchM
const fetchS = fetchStore()
const sessionM = useMachine(machine)
const version = ref("N/A")
const vCmd: CommandReq = {
  action: "exec",
  command: "version"
}

const restBtnclass: Ref<'animate-spin-rev-pause' | 'animate-spin-rev-running'> = ref('animate-spin-rev-pause')
publicS.getCommonResEffect(fetchM, body => {
  const result = body.results[0]
  if (result.type === "version") version.value = result.version
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
const interruptEvent = () => {
  fetchS.interruptJob()
}

const logout = async () => {

  restBtnclass.value = "animate-spin-rev-running"

  interruptEvent()

  sessionM.send("SUBMIT", {
    value: {
      action: "close_session",
      sessionId: undefined
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
const shutdown = () => {
  publicS.warnMessage = "Are you sure stop the arthas? All the Arthas clients connecting to this server will be disconnected."
  publicS.warningFn = () => {
    fetchS.baseSubmit(interpret(permachine), {
      command: "stop",
      action: "exec"
    })
    fetchS.online = false
    fetchS.wait = false
  }
  publicS.isWarn = true
}
</script>

<template>
  <nav class=" h-[10vh] flex justify-between items-center min-h-max border-b-2 shadow-orange-300">
    <a class="w-40 flex items-center justify-center" href="https://arthas.aliyun.com/doc/commands.html" target="_blank">
      <img src="@/assets/arthas.png" alt="logo" class=" w-3/4"/>
    </a>

    <div class="flex items-center h-20">
      <div class=" mr-4 bg-gray-200 h-12 rounded-full flex justify-center items-center text-gray-500 font-bold p-2">
        sessionId: {{fetchS.sessionId}}</div>
      <div class=" mr-4 bg-gray-200 h-12 p-2 rounded-full flex justify-center items-center text-gray-500 font-bold">
        version:{{ version }}
      </div>
      <button v-if="fetchS.jobRunning" @click.prevent="interruptEvent"
        class="bg-red-600 text-white h-1/2 p-2 rounded hover:bg-red-400 transition mr-4">interrupt</button>
      <button class="button-style bg-red-600 text-white mr-4" @click="shutdown">
        shutdown
      </button>
      <button class=" rounded-full bg-gray-200 h-12 w-12 flex justify-center items-center mr-4 " @click="reset">
        <refresh-icon class=" text-gray-500 h-3/4 w-3/4" :class="restBtnclass" />
      </button>
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