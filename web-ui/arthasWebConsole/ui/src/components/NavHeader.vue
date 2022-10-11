<script setup lang="ts">
import { useMachine } from '@xstate/vue';
import { onBeforeMount, Ref, ref, watchEffect } from 'vue';
import { RefreshIcon, LogoutIcon, LoginIcon, MenuIcon, XCircleIcon } from '@heroicons/vue/outline';
import { fetchStore } from '@/stores/fetch';
import machine from "@/machines/consoleMachine"
import { publicStore } from '@/stores/public';
import { interpret } from 'xstate';
import { Menu, MenuButton, MenuItems, MenuItem } from '@headlessui/vue'
import permachine from '@/machines/perRequestMachine';
const fetchM = useMachine(machine)
const publicS = publicStore()
const { send } = fetchM
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
const forceGc = () => {
  fetchS.baseSubmit(interpret(permachine), {
    action: "exec",
    command: "vmtool --action forceGc "
  }).then(
    res => publicS.$patch({
      isSuccess: true,
      SuccessMessage: "GC success!",
    })
  )
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
  publicS.warnMessage = "Are you sure to stop the arthas? All the Arthas clients connecting to this server will be disconnected."
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
const resetAllClass = () => {
  fetchS.baseSubmit(interpret(permachine), {
    action: "exec",
    command: `reset`
  }).then(response => {
    const result = (response as CommonRes).body.results[0]
    if (result.type === "reset") {
      publicS.isSuccess=true
    publicS.SuccessMessage = JSON.stringify(result.affect)
    }

  })
}
const tools:[string,()=>void][] = [
  ["forceGc", forceGc],
  ["shutdown", shutdown],
  ["reset class", resetAllClass]
]
</script>

<template>
  <nav class=" h-[10vh] flex justify-between items-center min-h-max border-b-2 shadow-orange-300">
    <a class="w-40 flex items-center justify-center" href="https://arthas.aliyun.com/doc/commands.html" target="_blank">
      <img src="/arthas.png" alt="logo" class=" w-3/4" />
    </a>

    <div class="flex items-center h-20">
      <div class=" mr-4 bg-info text-info-content h-12 rounded-full flex justify-center items-center font-bold p-2">
        sessionId: {{fetchS.sessionId}}</div>
      <div class=" mr-4 bg-info text-info-content h-12 p-2 rounded-full flex justify-center items-center font-bold">
        version:{{ version }}
      </div>
      <button v-if="fetchS.jobRunning" @click.prevent="interruptEvent"
        class="btn-error btn rounded-full h-1/2 p-2 transition mr-4">interrupt</button>
      <button class=" rounded-full btn btn-info btn-circle h-12 w-12 flex justify-center items-center mr-4 " @click="reset">
        <refresh-icon class="h-3/4 w-3/4" :class="restBtnclass" />
      </button>
      <button class="hover:opacity-50 h-12 w-12 grid place-items-center  rounded-full mr-2 transition-all"
        :class="{ 'bg-primary': !fetchS.online, 'bg-error': fetchS.online }">
        <LogoutIcon class="h-1/2 w-1/2 text-error-content" @click="logout" v-if="fetchS.online" />
        <login-icon class="h-1/2 w-1/2 text-primary-content" @click="login" v-else />
      </button>
      <Menu as="div" class="relative mr-4">
        <MenuButton
          class="w-12 h-12 input-btn-style grid place-items-center rounded-full bg-primary transition">
          <MenuIcon class="h-3/4 w-3/4 text-primary-content"></MenuIcon>
          <!-- <XCirleIcon class="h-3/4 w-3/4"></XCirleIcon> -->
        </MenuButton>
        <MenuItems class="absolute right-0 top-full input-btn-style mt-4 bg-white px-0 z-10 w-40">
          <MenuItem v-slot="{ active }" v-for="(v,i) in tools" :key="i">
          <div :class='{ "bg-blue-500 text-primary-content": active }' class="px-4 py-2">
            <button @click.prevent="v[1]">{{v[0]}}</button>
          </div>

          </MenuItem>
        </MenuItems>
      </Menu>
    </div>
  </nav>
</template>
<style scoped>

</style>