<script setup lang="ts">
import { useMachine } from '@xstate/vue';
import { computed, onBeforeMount, Ref, ref, watchEffect } from 'vue';
import { RefreshIcon, LogoutIcon, LoginIcon, MenuIcon, MenuAlt2Icon } from '@heroicons/vue/outline';
import { fetchStore } from '@/stores/fetch';
import machine from "@/machines/consoleMachine"
import { publicStore } from '@/stores/public';
import { interpret } from 'xstate';
import { PuzzleIcon, TerminalIcon, ViewGridIcon } from "@heroicons/vue/outline"
import { DesktopComputerIcon } from "@heroicons/vue/solid"
import { useRoute, useRouter } from 'vue-router';
import permachine from '@/machines/perRequestMachine';
import pic from "~/assert/arthas.png"
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
    version.value = result.version

  }
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

  fetchS.closeSession()
  restBtnclass.value = "animate-spin-rev-pause"
}

const login = () => {
  fetchS.initSession()
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
  {
    name: 'console',
    url: '/console',
    icon: TerminalIcon
  },
  {

    name: 'terminal',
    url: 'terminal',
    icon: TerminalIcon
  }
]

const tools: [string, () => void][] = [
  ["forceGc", forceGc],
  ["shutdown", shutdown],
  ["reset class", resetAllClass]
]
const router = useRouter()
const routePath = computed(() => useRoute().path)
const toNext = (url: string) => {
  if (url === "terminal") {
    window.open("/", "_blank")
  } else router.push(url)
}

</script>

<template>
  <nav class=" h-[10vh] border-b-2 navbar bg-base-100">
    <div class=" navbar-start flex items-stretch">
      <div class="dropdown dropdown-start hover xl:hidden">
        <label tabindex="0" class="btn btn-ghost m-1">
          <MenuAlt2Icon class="w-6 h-6"></MenuAlt2Icon>
        </label>
        <ul tabindex="0" class="menu menu-vertical dropdown-content bg-base-100 shadow rounded-box">
          <li v-for="(tab, idx) in tabs" :key="idx" @click="toNext(tab.url)">
            <a :class="{ 'bg-primary text-primary-content': routePath.includes(tab.url), }">
              <component :is="tab.icon" class="w-4 h-4" />
              {{
                  tab.name
              }}
            </a>
          </li>
        </ul>
      </div>
      <a class="flex items-center justify-center mx-2" href="https://arthas.aliyun.com/doc/commands.html"
        target="_blank">
        <img :src="pic" alt="logo" class="w-32" />
      </a>
      <span class="badge badge-ghost self-end badge-sm">v{{ version }}</span>
    </div>
    <div class="navbar-center">

      <ul class="menu menu-horizontal hidden xl:flex">
        <li v-for="(tab, idx) in tabs" :key="idx" @click="toNext(tab.url)">
          <a :class="{ 'bg-primary text-primary-content': routePath.includes(tab.url), }">
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
          <li class="" v-for="(v, i) in tools" :key="i">
            <a @click.prevent="v[1]">{{ v[0] }}</a>
          </li>
        </ul>
      </div>
      <button class=" btn btn-ghost" :class="{ 'btn-primary': !fetchS.online, 'btn-error': fetchS.online }">
        <LogoutIcon class="h-6 w-6" @click="logout" v-if="fetchS.online" />
        <login-icon class="h-6 w-6" @click="login" v-else />
      </button>
      <button class="btn-ghost btn" @click="reset">
        <refresh-icon class="h-6 w-6" :class="restBtnclass" />
      </button>
    </div>
  </nav>
</template>