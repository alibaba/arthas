<script setup lang="ts">
import { onMounted, ref } from "vue";
import { Terminal } from "xterm"
import { FitAddon } from 'xterm-addon-fit';
import {WebglAddon} from "xterm-addon-webgl"
// import $ from "jquery"
let ws: WebSocket | undefined;
let xterm = new Terminal({allowProposedApi: true})
const DEFAULT_SCROLL_BACK = 1000
const MAX_SCROLL_BACK = 9999999
const MIN_SCROLL_BACK = 1
const webglAddon = new WebglAddon();
const ip = ref("127.0.0.1")
const port = ref('3568')
const iframe = ref(true)
const fullSc = ref(true)
let fitAddon = new FitAddon();

onMounted(() => {
  // var url = window.location.href;
  ip.value = getUrlParam('ip') ?? window.location.hostname;
  port.value = getUrlParam('port') ?? '8563';
  let _iframe = getUrlParam('iframe')
  if (_iframe && _iframe.trim() !== 'false') iframe.value = false

  startConnect(true);
  window.addEventListener('resize', function () {
    if (ws !== undefined && ws !== null) {
      // let terminalSize = getTerminalSize();
      const {cols, rows} = fitAddon.proposeDimensions()!
      // console.log(cols, rows)
      ws.send(JSON.stringify({ action: 'resize', cols, rows: rows }));
      // xterm.resize(terminalSize.cols, terminalSize.rows);
      fitAddon.fit();
    }
  });
});

/** get params in url **/
// 可以使用urlparam类取代
function getUrlParam(name: string, url = window.location.href) {
  name = name.replace(/[\[\]]/g, '\\$&');
  const regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
    results = regex.exec(url);
  if (!results) return null;
  if (!results[2]) return '';
  return decodeURIComponent(results[2].replace(/\+/g, ' '));
}


/** init websocket **/
function initWs(silent: boolean) {
  let path = 'ws://' + ip.value + ':' + port.value + '/ws';
  console.log(path)
  ws = new WebSocket(path);
  ws.onerror = function () {
    ws ?? ws!.close();
    ws = undefined;
    !silent && alert('Connect error');
  };
  ws.onopen = function () {
    console.log('open');
    // $('#fullSc').show();
    fullSc.value = true
    // var terminalSize = getTerminalSize()

    let scrollback = getUrlParam('scrollback') ?? '0';

    const {cols, rows} = initXterm(scrollback)
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
    window.setInterval(function () {
      if (ws != null && ws.readyState === 1) {
        ws.send(JSON.stringify({ action: 'read', data: "" }));
      }
    }, 30000);
  }
}

/** init xterm **/
function initXterm(scrollback: string) {
  let scrollNumber = parseInt(scrollback, 10)
  xterm = new Terminal({
    // cols,
    // rows,
    screenReaderMode: false,
    // rendererType: 'canvas',
    convertEol: true,
    allowProposedApi: true,
    scrollback: isValidNumber(scrollNumber) ? scrollNumber : DEFAULT_SCROLL_BACK
  });
  xterm.loadAddon(fitAddon)
  
  xterm.open(document.getElementById('terminal')!);

  xterm.loadAddon(webglAddon)
  // setTimeout(()=>fitAddon.fit(),50);
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

/** begin connect **/
function startConnect(silent: boolean = false) {
  if (ip.value.trim() === '' || port.value.trim() === '') {
    alert('Ip or port can not be empty');
    return;
  }
  if (ws) {
    alert('Already connected');
    return;
  }
  // init webSocket
  initWs(silent);
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
    // $('#fullSc').hide();
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
  ele.onfullscreenchange = (e:Event)=>{
    // if(!document.fullscreenElement) {
      fitAddon.fit()
      console.log()
    // }
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
  <!-- <nav v-if="iframe" class="
  navbar navbar-expand navbar-light flex-column flex-md-row bd-navbar bg-light
  ">
    <a href="https://github.com/alibaba/arthas" target="_blank" title="" class="navbar-brand"><img src="/logo.png"
        alt="Arthas" title="Welcome to Arthas web console" style="height: 25px;" class="img-responsive"></a>

    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent"
      aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>

    <div class="collapse navbar-collapse" id="navbarSupportedContent">
      <ul class="navbar-nav mr-auto">
        <li class="nav-item active">
          <a class="nav-link" href="https://arthas.aliyun.com/doc" target="_blank">Documentation
            <span class="sr-only">(current)</span></a>
        </li>
        <li class="nav-item">
          <a class="nav-link" href="https://arthas.aliyun.com/doc/arthas-tutorials.html" target="_blank">Online
            Tutorials</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" href="https://github.com/alibaba/arthas" target="_blank">Github</a>
        </li>
      </ul>
    </div>

    <form class="form-inline my-2 my-lg-0">
      <div class="col">
        <div class="input-group ">
          <div class="input-group-prepend">
            <span class="input-group-text" id="ip-addon">IP</span>
          </div>
          <input v-model="ip" type="text" class="form-control" name="ip" id="ip" placeholder="please enter ip address"
            aria-label="ip" aria-describedby="ip-addon">
        </div>
      </div>

      <div class="col">
        <div class="input-group ">
          <div class="input-group-prepend">
            <span class="input-group-text" id="port-addon">Port</span>
          </div>
          <input v-model="port" type="text" class="form-control" name="port" id="port" placeholder="please enter port"
            aria-label="port" aria-describedby="port-addon">
        </div>
      </div>

      <div class="col-inline">
        <button title="connect" type="button" class="btn btn-info form-control mr-1"
          @click="startConnect(true)">Connect</button>
        <button title="disconnect" type="button" class="btn btn-info form-control mr-1"
          @click="disconnect">Disconnect</button>
        <a target="_blank" href="arthas-output/" class="btn btn-info" role="button">Arthas Output</a>
      </div>
    </form>

  </nav> -->
  <div class="flex flex-col h-[100vh] resize-none">
    <nav v-if="iframe" class="navbar bg-base-100 flex-row">
      <div class="flex-1">
        <a href="https://github.com/alibaba/arthas" target="_blank" title="" class="mr-2 w-20"><img
            src="/logo.png" alt="Arthas" title="Welcome to Arthas web console" class=""></a>
        <ul class="menu menu-horizontal p-0">
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
      <form class="navbar-end">
        <div class="flex">
          <label class="input-group input-group-sm mr-2">
            <span>IP</span>
            <input type="text" placeholder="please enter ip address" class="input input-bordered input-sm "
              v-model="ip" />
          </label>
          <label class="input-group input-group-sm mr-2">
            <span>Port</span>
            <input type="text" placeholder="please enter port" class="input input-sm input-bordered" v-model="port" />
          </label>
        </div>
        <div class="btn-group btn-group-horizontal">
          <button
            class="btn btn-sm bg-secondary hover:bg-secondary-focus border-none text-secondary-content focus:bg-secondary-focus normal-case"
            @click.prevent="startConnect(true)">Connect</button>
          <button
            class="btn btn-sm bg-secondary hover:bg-secondary-focus border-none text-secondary-content focus:bg-secondary-focus normal-case"
            @click.prevent="disconnect">Disconnect</button>
          <a class="btn btn-sm bg-secondary hover:bg-secondary-focus border-none text-secondary-content focus:bg-secondary-focus normal-case"
            href="arthas-output/" target="_blank">Arthas Output</a>
        </div>
      </form>

    </nav>
    <div class="w-full h-0 flex-auto bg-black overscroll-auto" id="terminal-card">
      <!-- <div class="h-full overflow-visible" id="terminal-card"> -->
      <div id="terminal" class="w-full h-full"></div>
      <!-- </div> -->
    </div>

    <div title="fullscreen" id="fullSc" class="fullSc" v-if="fullSc">
      <button id="fullScBtn" @click="xtermFullScreen"><img src="/fullsc.png"></button>
    </div>
  </div>
</template>

<style>
#terminal:-webkit-full-screen {
  background-color: rgb(255, 255, 12);
}

/* .container {
  width: 100%;
  min-height: 600px;
} */

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