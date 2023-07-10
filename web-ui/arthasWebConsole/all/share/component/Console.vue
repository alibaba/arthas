<script setup lang="ts">
import {computed, onMounted, reactive, ref, watchEffect} from "vue";
import {Terminal} from "xterm"
import {FitAddon} from 'xterm-addon-fit';
import {WebglAddon} from "xterm-addon-webgl"
import {MenuAlt2Icon} from "@heroicons/vue/outline"
import fullPic from "~/assert/fullsc.png"
import arthasLogo from "~/assert/arthas.png"
import {useRouter} from 'vue-router'

// const { isTunnel = false } = defineProps<{
//   isTunnel?: boolean
// }>()
const isTunnel = import.meta.env.MODE === 'tunnel';
let ws: WebSocket | undefined;
let intervalReadKey = -1
const DEFAULT_SCROLL_BACK = 1000
const MAX_SCROLL_BACK = 9999999
const MIN_SCROLL_BACK = 1
const ARTHAS_PORT = isTunnel ? "7777" : "8563"
const ip = ref("")
const port = ref('')
const iframe = ref(true)
const fullSc = ref(true)
const agentID = ref('')
const outputHerf = computed(() => {
  console.log(agentID.value)
  return isTunnel ? `proxy/${agentID.value}/arthas-output/` : `/arthas-output/`
})
const fitAddon = new FitAddon();
const webglAddon = new WebglAddon();
const isConnected = ref(false)
let xterm = new Terminal({allowProposedApi: true})

onMounted(() => {
  ip.value = getUrlParam('ip') ?? window.location.hostname;
  port.value = getUrlParam('port') ?? ARTHAS_PORT;
  if (isTunnel) agentID.value = getUrlParam("agentId") ?? ""
  let _iframe = getUrlParam('iframe')
  if (_iframe && _iframe.trim() !== 'false') iframe.value = false

  startConnect(true);
  window.addEventListener('resize', function () {
    if (ws !== undefined && ws !== null) {
      const {cols, rows} = fitAddon.proposeDimensions()!
      ws.send(JSON.stringify({action: 'resize', cols, rows: rows}));
      fitAddon.fit();
    }
  });
});

/** get params in url **/
function getUrlParam(name: string) {
  const urlparam = new URLSearchParams(window.location.search)
  return urlparam.get(name)
}

function getWsUri() {
  const host = `${ip.value}:${port.value}`
  if (!isTunnel) return `ws://${host}/ws`;
  const path = getUrlParam("path") ?? 'ws'
  const _targetServer = getUrlParam("targetServer")
  let protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
  const uri = `${protocol}${host}/${encodeURIComponent(path)}?method=connectArthas&id=${agentID.value}`
  if (_targetServer != null) {
    return uri + '&targetServer=' + encodeURIComponent(_targetServer);
  }
  return uri
}

/** init websocket **/
function initWs(silent: boolean) {
  let uri = getWsUri()
  ws = new WebSocket(uri);
  ws.onerror = function () {
    ws ?? ws!.close();
    ws = undefined;
    !silent && alert('连接错误');
  };
  ws.onopen = function () {
    fullSc.value = true

    let scrollback = getUrlParam('scrollback') ?? '0';

    const {cols, rows} = initXterm(scrollback)
    xterm.onData(function (data) {
      ws?.send(JSON.stringify({action: 'read', data: data}))
    });
    ws!.onmessage = function (event: MessageEvent) {
      if (event.type === 'message') {
        var data = event.data;
        xterm.write(data);
      }
    };
    ws?.send(JSON.stringify({action: 'resize', cols, rows}));
    intervalReadKey = window.setInterval(function () {
      if (ws != null && ws.readyState === 1) {
        ws.send(JSON.stringify({action: 'read', data: ""}));
      }
    }, 30000);
  }
  ws.onclose = function (message) {
    if (intervalReadKey != -1) {
      window.clearInterval(intervalReadKey)
      intervalReadKey = -1
    }
    if (message.code === 2000) {
      alert(message.reason);
    }
  };
  isConnected.value = true
}

/** init xterm **/
function initXterm(scrollback: string) {
  let scrollNumber = parseInt(scrollback, 10)
  xterm = new Terminal({
    screenReaderMode: false,
    convertEol: true,
    allowProposedApi: true,
    scrollback: isValidNumber(scrollNumber) ? scrollNumber : DEFAULT_SCROLL_BACK
  });
  xterm.loadAddon(fitAddon)

  xterm.open(document.getElementById('terminal')!);

  xterm.loadAddon(webglAddon)
  fitAddon.fit()
  return {
    cols: xterm.cols,
    rows: xterm.rows
  }
}

function isValidNumber(scrollNumber: number) {
  return scrollNumber >= MIN_SCROLL_BACK &&
      scrollNumber <= MAX_SCROLL_BACK;
}

const connectGuard = (silent: boolean): boolean => {
  if (ip.value.trim() === '' || port.value.trim() === '') {
    alert('Ip 或者端口不能为空');
    return false;
  }
  if (isTunnel && agentID.value == '') {
    if (silent) {
      return false;
    }
    alert('请选择 Agent 实例');
    return false;
  }
  if (ws) {
    alert('Agent 已连接');
    return false;
  }
  return true
}

/** begin connect **/
function startConnect(silent: boolean = false) {
  if (connectGuard(silent)) {
    // init webSocket
    initWs(silent);
  }
}

function disconnect() {
  try {
    ws!.close();
    ws!.onmessage = null;
    ws!.onclose = null;
    ws = undefined;
    xterm.dispose();
    fitAddon.dispose()
    webglAddon.dispose()
    fullSc.value = false
    isConnected.value = false
    // alert('Connection was closed successfully!');
  } catch {
    // alert('No connection, please start connect first.');
    alert('关闭连接异常');
  }
}

/** full screen show **/
function xtermFullScreen() {
  var ele = document.getElementById('terminal-card')!;
  requestFullScreen(ele);
  ele.onfullscreenchange = (e: Event) => {
    fitAddon.fit()
  }
}

function requestFullScreen(element: HTMLElement) {
  let requestMethod = element.requestFullscreen;
  if (requestMethod) {
    requestMethod.call(element);
    //@ts-ignore
  } else if (window.ActiveXObject) {
    // @ts-ignore
    var wscript = new ActiveXObject("WScript.Shell");
    if (wscript !== null) {
      wscript.SendKeys("{F11}");
    }
  }
}

const router = useRouter()

function logout() {
  if (ws) {
    disconnect();
  }
  sessionStorage.removeItem('username');
  sessionStorage.removeItem('token');
  router.push('/login');
}

const apps: string[] = reactive([])
const services = ref([])
const selectedService = ref('')
const selectedAgents = ref([])

const loadServices = async () => {
  services.value = await fetchAgentGroup()
      .then(data => data.map((item: { service: any; agents: any; }) => ({service: item.service, agents: item.agents})))
}

const getSelectedAgents = () => {
  const selected = services.value.find(item => item['service'] === selectedService.value)
  if (selected) {
    selectedAgents.value = selected['agents']
  } else {
    selectedAgents.value = []
  }
  agentID.value = ''
}

const onServiceChange = () => {
  getSelectedAgents()
}

watchEffect(() => {
  if (selectedService.value) {
    getSelectedAgents()
  } else {
    selectedAgents.value = []
    agentID.value = ''
  }
})

const fetchAgentGroup = async () => {
  const token = sessionStorage.getItem('token');
  try {
    const response = await fetch('/api/arthas/agents',
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
          }
        }
    );
    return await response.json();
  } catch (error) {
    console.error(error);
  }
}

onMounted(() => {
  loadServices()
})
</script>

<template>
  <div class="flex flex-col h-[100vh] w-[100vw] resize-none">
    <nav v-if="iframe" class="navbar bg-base-100 md:flex-row flex-col w-[100vw]">
      <div class="navbar-start">
        <div class="dropdown dropdown-start 2xl:hidden">
          <label tabindex="0" class="btn btn-ghost btn-sm">
            <MenuAlt2Icon class="w-6 h-6"></MenuAlt2Icon>
          </label>
          <ul tabindex="0" class="dropdown-content menu shadow bg-base-100">
            <li>
              <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm" href="https://arthas.aliyun.com/doc"
                 target="_blank">文档
                <span class="sr-only">(current)</span></a>
            </li>
            <li>
              <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm"
                 href="https://arthas.aliyun.com/doc/arthas-tutorials.html" target="_blank">教程</a>
            </li>
            <li>
              <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm" href="https://github.com/alibaba/arthas"
                 target="_blank">Github</a>
            </li>
          </ul>
        </div>
        <a href="https://github.com/alibaba/arthas" target="_blank" title="" class="mr-2 w-20"><img
            :src="arthasLogo" alt="Arthas" title="Welcome to Arthas web console"></a>
        <span class="navbar-version" style="font-size: 18px;margin-right: 30px">v3.6.7</span>
        <ul class="menu menu-vertical 2xl:menu-horizontal hidden">
          <li>
            <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm" href="https://arthas.aliyun.com/doc"
               target="_blank">文档
              <span class="sr-only">(current)</span></a>
          </li>
          <li>
            <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm"
               href="https://arthas.aliyun.com/doc/arthas-tutorials.html" target="_blank">教程</a>
          </li>
          <li>
            <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm" href="https://github.com/alibaba/arthas"
               target="_blank">Github</a>
          </li>
        </ul>

      </div>
      <div class="navbar-end" style="min-width: 500px;">
        <div class="xl:flex-row form-control">
      <!--<label class="input-group input-group-sm mr-2">
            <span>IP</span>
            <input type="text" placeholder="please enter ip address" class="input input-bordered input-sm "
                   v-model="ip"/>
          </label>
          <label class="input-group input-group-sm mr-2">
            <span>Port</span>
            <input type="text" placeholder="please enter port" class="input input-sm input-bordered" v-model="port"/>
          </label>
          <label v-if="isTunnel" class="input-group input-group-sm mr-2">
            <span>AgentId</span>
            <input type="text" placeholder="please enter AgentId" class="input input-sm input-bordered"
                               v-model="agentID" />
          </label>-->

          <label class="input-group input-group-md" style="white-space: nowrap;">
            <span class="bg-transparent font-bold" style="font-size: 16px;">应用</span>
            <select v-model="selectedService" @change="onServiceChange"
                    class="mr-3 form-select border border-gray-300" style="min-width: 140px;height:32px;">
              <option value="" disabled>请选择应用</option>
              <option v-for="service in services" :key="service.service" :value="service.service">{{
                  service['service']
                }}
              </option>
            </select>
          </label>
          <label class="input-group input-group-md" style="white-space: nowrap;">
            <span class="bg-transparent font-bold" style="font-size: 16px;">实例</span>
            <select v-model="agentID" class="mr-5 form-select border border-gray-300"
                    style="min-width: 140px;height:32px;">
              <option value="" disabled>请选择实例</option>
              <option v-for="agent in selectedAgents" :key="agent.id" :value="selectedService + '@' + agent.id">
                {{ agent.info.host }}:{{ agent.info.port }}
              </option>
            </select>
          </label>
        </div>
        <div class="btn-group 2xl:btn-group-horizontal btn-group-horizontal">
          <button v-if="!isConnected"
                  class="mr-2 btn btn-sm bg-green-500 hover:bg-green-700 focus:bg-green-700 border-none normal-case"
                  @click.prevent="startConnect(false)">
            <span class="mr-2">
              <i class="fas fa-window-restore"></i>
            </span>连接
          </button>
          <button v-if="isConnected"
                  class="mr-2 btn btn-sm bg-orange-500 hover:bg-orange-700 focus:bg-orange-700 border-none normal-case"
                  @click.prevent="disconnect">
            <span class="mr-2">
              <i class="fas fa-window-close"></i>
            </span>断开
          </button>
          <a class="mr-2 btn btn-sm bg-red-500 hover:bg-red-700 focus:bg-red-700 border-none normal-case"
             :href="outputHerf" target="_blank"><span class="mr-2">
              <i class="fas fa-fire"></i>
            </span>火焰图
          </a>

          <button class="btn btn-sm bg-gray-500 hover:bg-gray-700 focus:bg-gray-700 border-none normal-case"
                  @click.prevent="logout">
            <span class="mr-2">
              <i class="fas fa-power-off"></i>
            </span>注销
          </button>
        </div>
      </div>
    </nav>
    <div class="w-full h-0 flex-auto bg-black overscroll-auto" id="terminal-card">
      <div id="terminal" class="w-full h-full"></div>
    </div>

    <div title="fullscreen" id="fullSc" class="fullSc" v-if="fullSc">
      <button id="fullScBtn" @click="xtermFullScreen"><img :src="fullPic"></button>
    </div>
  </div>
</template>

<style>
#terminal:-webkit-full-screen {
  background-color: rgb(255, 255, 12);
}

.fullSc {
  z-index: 10000;
  position: fixed;
  top: 25%;
  left: 90%;
}

#fullScBtn {
  border-radius: 17px;
  border: 0;
  cursor: pointer;
  background-color: black;
}
</style>