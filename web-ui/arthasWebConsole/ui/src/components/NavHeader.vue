<script setup lang="ts">
import { useMachine } from '@xstate/vue';
import { computed, onBeforeMount, Ref, ref, watchEffect } from 'vue';
import { RefreshIcon, LogoutIcon, LoginIcon, MenuIcon, XCircleIcon } from '@heroicons/vue/outline';
import { fetchStore } from '@/stores/fetch';
import machine from "@/machines/consoleMachine"
import { publicStore } from '@/stores/public';
import { interpret } from 'xstate';
import { PuzzleIcon, TerminalIcon, ViewGridIcon } from "@heroicons/vue/outline"
import { DesktopComputerIcon } from "@heroicons/vue/solid"
import { useRoute, useRouter } from 'vue-router';
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
  if (result.type === "version") {
    let _raw = result.version.split(".")
    if(_raw.length === 4) _raw.length = 3 
    version.value = _raw.join(".")

  }})

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
  // fetchS.baseSubmit()
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
      publicS.isSuccess = true
      publicS.SuccessMessage = JSON.stringify(result.affect)
    }

  })
}
const tabs = [
  {
    name: 'dashboard',
    url: "/dashboard",
    icon: DesktopComputerIcon
  },
  {
    name: 'immediacy',
    url: '/synchronize',
    icon: ViewGridIcon
  }, {
    name: "real time",
    url: '/asynchronize',
    icon: ViewGridIcon
  },
  {
    name: 'option',
    url: '/config',
    icon: PuzzleIcon
  },
  // {
  //   name: 'console',
  //   url: '/console',
  //   icon: TerminalIcon
  // },

]

const tools: [string, () => void][] = [
  ["forceGc", forceGc],
  ["shutdown", shutdown],
  ["reset class", resetAllClass]
]
const router = useRouter()
const routePath = computed(() => useRoute().path)
const toNext = (url: string) => {
  router.push(url)
}
</script>

<template>
  <nav class=" h-[10vh] border-b-2 navbar">
    <div class=" navbar-start">
      <div class=" indicator mx-3">
        <span class="indicator-item indicator-bottom indicator-end badge badge-ghost -right-3">v{{version}}</span>
        <a class="flex items-center justify-center w-40" href="https://arthas.aliyun.com/doc/commands.html"
          target="_blank">
          <img src="/arthas.png" alt="logo" class=" w-3/4" />
        </a>
      </div>
    </div>
    <div class="navbar-center">
      <ul class="menu menu-horizontal p-0">
        <li v-for="(tab, idx) in tabs" :key="idx" @click="toNext(tab.url)">
          <a class="break-all" :class="{ 'bg-primary text-primary-content': routePath.includes(tab.url), }">
            <component :is="tab.icon" class="w-4 h-4" />
            {{
            tab.name
            }}
          </a>
        </li>
      </ul>
    </div>
    <div class="flex items-center h-20 navbar-end">
      <button v-if="fetchS.jobRunning" @click.prevent="interruptEvent"
        class="btn-error btn h-1/2 p-2">interrupt</button>
      <div class="dropdown dropdown-end mr-2">
        <label tabindex="0" class="btn btn-ghost m-1">
          <MenuIcon class=" w-6 h-6"></MenuIcon>
        </label>
        <ul tabindex="0" class="menu dropdown-content p-2 shadow-xl bg-base-200 rounded-box w-40">
          <li class="" v-for="(v,i) in tools" :key="i">
            <a @click.prevent="v[1]">{{v[0]}}</a>
          </li>
        </ul>
      </div>
      <button class=" btn btn-ghost"
        :class="{ 'btn-primary': !fetchS.online, 'btn-error': fetchS.online }">
        <LogoutIcon class="h-6 w-6" @click="logout" v-if="fetchS.online" />
        <login-icon class="h-6 w-6" @click="login" v-else />
      </button>
      <button class="btn-ghost btn"
        @click="reset">
        <refresh-icon class="h-6 w-6" :class="restBtnclass" />
      </button>
    </div>
  </nav>
</template>