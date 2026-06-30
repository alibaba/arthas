<script setup lang="ts">
import { onMounted, ref, computed  } from "vue";
import { Terminal } from "xterm"
import { FitAddon } from 'xterm-addon-fit';
import { WebglAddon } from "xterm-addon-webgl"
import { MenuAlt2Icon } from "@heroicons/vue/outline"
import fullPic from "~/assert/fullsc.png"
import arthasLogo from "~/assert/arthas.png"
const { isTunnel = false, isNativeAgent = false} = defineProps<{
  isTunnel?: boolean
  isNativeAgent?: boolean
}>()

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
const nativeAgentAddress = ref('')
const outputHerf = computed(() => {
  console.log(agentID.value)
  return isTunnel?`proxy/${agentID.value}/arthas-output/`:`/arthas-output/`
})
// const isTunnel = import.meta.env.MODE === 'tunnel'
const fitAddon = new FitAddon();
const webglAddon = new WebglAddon();
let xterm = new Terminal({ allowProposedApi: true })

onMounted(() => {
  ip.value = getUrlParam('ip') ?? window.location.hostname;
  port.value = getUrlParam('port') ?? ARTHAS_PORT;
  if (isNativeAgent) nativeAgentAddress.value = getUrlParam("nativeAgentAddress") ?? ""
  if (isTunnel) agentID.value = getUrlParam("agentId") ?? ""
  let _iframe = getUrlParam('iframe')
  if (_iframe && _iframe.trim() !== 'false') iframe.value = false

  startConnect(true);
  window.addEventListener('resize', function () {
    if (ws !== undefined && ws !== null) {
      const { cols, rows } = fitAddon.proposeDimensions()!
      ws.send(JSON.stringify({ action: 'resize', cols, rows: rows }));
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
  if (isNativeAgent) return `ws://${host}/ws?nativeAgentAddress=${nativeAgentAddress.value}`;
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
    !silent && alert('Connect error');
  };
  ws.onopen = function () {
    fullSc.value = true

    let scrollback = getUrlParam('scrollback') ?? '0';

    const { cols, rows } = initXterm(scrollback)
    xterm.onData(function (data) {
      ws?.send(JSON.stringify({ action: 'read', data: data }))
    });
    ws!.onmessage = function (event: MessageEvent) {
      if (event.type === 'message') {
        var data = event.data;
        xterm.write(data);
      }
    };
    ws?.send(JSON.stringify({ action: 'resize', cols, rows }));
    intervalReadKey = window.setInterval(function () {
      if (ws != null && ws.readyState === 1) {
        ws.send(JSON.stringify({ action: 'read', data: "" }));
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
    alert('Ip or port can not be empty');
    return false;
  }
  if (isTunnel && agentID.value == '') {
    if (silent) {
      return false;
    }
    alert('AgentId can not be empty');
    return false;
  }
  if (ws) {
    alert('Already connected');
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
    alert('Connection was closed successfully!');
  } catch {
    alert('No connection, please start connect first.');
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
                target="_blank">Documentation
                <span class="sr-only">(current)</span></a>
            </li>
            <li>
              <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm"
                href="https://arthas.aliyun.com/doc/arthas-tutorials.html" target="_blank">Online
                Tutorials</a>
            </li>
            <li>
              <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm" href="https://github.com/alibaba/arthas"
                target="_blank">Github</a>
            </li>
          </ul>
        </div>
        <a href="https://github.com/alibaba/arthas" target="_blank" title="" class="mr-2 w-20"><img
            :src="arthasLogo" alt="Arthas" title="Welcome to Arthas web console"></a>

        <ul class="menu menu-vertical 2xl:menu-horizontal hidden">
          <li>
            <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm" href="https://arthas.aliyun.com/doc"
              target="_blank">Documentation
              <span class="sr-only">(current)</span></a>
          </li>
          <li>
            <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm"
              href="https://arthas.aliyun.com/doc/arthas-tutorials.html" target="_blank">Online
              Tutorials</a>
          </li>
          <li>
            <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm" href="https://github.com/alibaba/arthas"
              target="_blank">Github</a>
          </li>
        </ul>

      </div>
      <div class="navbar-center ">
        <div class=" xl:flex-row form-control"        
        :class="{
          'xl:flex-row':isTunnel,
          'lg:flex-row':!isTunnel
        }">
          <label class="input-group input-group-sm mr-2">
            <span>IP</span>
            <input type="text" placeholder="please enter ip address" class="input input-bordered input-sm "
              v-model="ip" />
          </label>
          <label class="input-group input-group-sm mr-2">
            <span>Port</span>
            <input type="text" placeholder="please enter port" class="input input-sm input-bordered" v-model="port" />
          </label>
          <label v-if="isTunnel" class="input-group input-group-sm mr-2">
            <span>AgentId</span>
            <input type="text" placeholder="please enter AgentId" class="input input-sm input-bordered"
              v-model="agentID" />
          </label>
        </div>
      </div>
      <div class="navbar-end">
        <div class="btn-group   2xl:btn-group-horizontal btn-group-horizontal"
        :class="{
          'md:btn-group-vertical':isTunnel
        }">
          <button
            class="btn btn-sm bg-secondary hover:bg-secondary-focus border-none text-secondary-content focus:bg-secondary-focus normal-case"
            @click.prevent="startConnect(true)">Connect</button>
          <button
            class="btn btn-sm bg-secondary hover:bg-secondary-focus border-none text-secondary-content focus:bg-secondary-focus normal-case"
            @click.prevent="disconnect">Disconnect</button>
          <a class="btn btn-sm bg-secondary hover:bg-secondary-focus border-none text-secondary-content focus:bg-secondary-focus normal-case"
            :href="outputHerf" target="_blank">Arthas Output</a>
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