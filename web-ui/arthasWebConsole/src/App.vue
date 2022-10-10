<script setup lang="ts">
import { onMounted, ref } from "vue";
import { Terminal } from "xterm"
// import $ from "jquery"
let ws: WebSocket | undefined;
let xterm = new Terminal()
const DEFAULT_SCROLL_BACK = 1000
const MAX_SCROLL_BACK = 9999999
const MIN_SCROLL_BACK = 1

const ip = ref("127.0.0.1")
const port = ref('3568')
const iframe = ref(true)
const fullSc = ref(true)
onMounted(() => {
  // var url = window.location.href;
  ip.value = getUrlParam('ip') ?? window.location.hostname;
  port.value = getUrlParam('port') ?? '8563';
  let _iframe = getUrlParam('iframe')
  if (_iframe && _iframe !== 'false') iframe.value = false
  // if (ip != '' && ip != null) {
  //     $('#ip').val(ip);
  // } else {
  //     $('#ip').val(window.location.hostname);
  // }
  // if (port != '' && port != null) {
  //     $('#port').val(port);
  // }
  // if (port == null && location.port == "8563") {
  //     $('#port').val(8563);
  // }

  // var iframe = getUrlParam('iframe');
  // if (iframe != null && iframe != 'false') {
  //     $("nav").hide()
  // }

  startConnect(true);
  window.addEventListener('resize', function () {
    if (ws !== undefined && ws !== null) {
      let terminalSize = getTerminalSize();
      ws.send(JSON.stringify({ action: 'resize', cols: terminalSize.cols, rows: terminalSize.rows }));
      xterm.resize(terminalSize.cols, terminalSize.rows);
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

function getCharSize() {
  const tempDiv = document.createElement('div')!
  tempDiv.setAttribute('role', 'listitem')
  const tempSpan = document.createElement('div')!
  tempSpan.innerHTML = 'qwertyuiopasdfghjklzxcvbnm'
  tempDiv.append(tempSpan);
  document.body.append(tempDiv)
  let tempSpanWidth = tempSpan.offsetWidth
  let tempDivHeight = tempDiv.offsetHeight
  let tempDivWidth = tempDiv.offsetWidth
  let tempSpanHeight = tempSpan.offsetHeight
  const size = {
    width: tempSpanWidth / 26,
    height: tempSpanHeight,
    left: tempDivWidth - tempSpanWidth,
    top: tempDivHeight - tempSpanHeight,
  };
  tempDiv.remove();
  return size;
}

function getWindowSize() {
  let e: Window | HTMLElement = window;
  let a: 'innerHeight' | 'clientHeight' = 'innerHeight';
  let terminalDiv = document.getElementById("terminal-card")!;
  let terminalDivRect = terminalDiv.getBoundingClientRect();

  if (!('innerWidth' in window)) {
    a = 'clientHeight';
    e = document.documentElement ?? document.body;
    return {
      width: terminalDivRect.width,
      height: e[a] - terminalDivRect.top
    };
  }
  return {
    width: terminalDivRect.width,
    height: e[a] - terminalDivRect.top
  };
}

function getTerminalSize() {
  const charSize = getCharSize();
  const windowSize = getWindowSize();
  console.log('charsize');
  console.log(charSize);
  console.log('windowSize');
  console.log(windowSize);
  return {
    cols: Math.floor((windowSize.width - charSize.left) / 10),
    rows: Math.floor((windowSize.height - charSize.top) / 17)
  };
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
    var terminalSize = getTerminalSize()
    let scrollback = getUrlParam('scrollback') ?? '0';
    console.group('terminalSize')
    console.log(terminalSize)
    console.groupEnd()
    // init xterm
    initXterm(terminalSize.cols, terminalSize.rows, scrollback)
    ws!.onmessage = function (event: MessageEvent) {
      if (event.type === 'message') {
        var data = event.data;
        xterm.write(data);
      }
    };
    xterm.open(document.getElementById('terminal')!);
    console.log(xterm)
    // xterm = new Terminal()
    xterm.onData(function (data) {
      ws?.send(JSON.stringify({ action: 'read', data: data }))
    });
    ws?.send(JSON.stringify({ action: 'resize', cols: terminalSize.cols, rows: terminalSize.rows }));
    window.setInterval(function () {
      if (ws != null && ws.readyState === 1) {
        ws.send(JSON.stringify({ action: 'read', data: "" }));
      }
    }, 30000);
  }
}

/** init xterm **/
function initXterm(cols: number, rows: number, scrollback: string) {
  let scrollNumber = parseInt(scrollback, 10)
  xterm = new Terminal({
    cols,
    rows,
    screenReaderMode: false,
    // rendererType: 'canvas',
    convertEol: true,
    scrollback: isValidNumber(scrollNumber) ? scrollNumber : DEFAULT_SCROLL_BACK
  });
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
  <nav v-if="iframe" class="
  navbar navbar-expand navbar-light flex-column flex-md-row bd-navbar bg-light
  ">
    <a href="https://github.com/alibaba/arthas" target="_blank" title="" class="navbar-brand"><img src="/logo.png"
        alt="Arthas" title="Welcome to Arthas web console" style="height: 25px;" class="img-responsive"></a>

    <!-- <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent"
      aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button> -->

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
    <div class="flex-none">
      <ul class="menu menu-horizontal p-0">
        <li><a>Item 1</a></li>
        <li tabindex="0">
          <a>
            Parent
            <svg class="fill-current" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24">
              <path d="M7.41,8.58L12,13.17L16.59,8.58L18,10L12,16L6,10L7.41,8.58Z" />
            </svg>
          </a>
          <ul class="p-2 bg-base-100">
            <li><a>Submenu 1</a></li>
            <li><a>Submenu 2</a></li>
          </ul>
        </li>
        <li><a>Item 3</a></li>
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
      <div class="btn-group">
        <button class="btn btn-md" @click.prevent="startConnect(true)">Connect</button>
        <button class="btn" @click.prevent="disconnect">Disconnect</button>
        <a class="btn" href="arthas-output/">Arthas Output</a>
      </div>
    </form>

  </nav>
  <div class="container-fluid px-0">
    <div class="col px-0" id="terminal-card">
      <div id="terminal"></div>
    </div>
  </div>

  <div title="fullscreen" id="fullSc" class="fullSc" v-if="fullSc">
    <button id="fullScBtn" @click="xtermFullScreen"><img src="/fullsc.png"></button>
  </div>
</template>

<style>
#terminal:-webkit-full-screen {
  background-color: rgb(255, 255, 12);
}

.container {
  width: 100%;
  min-height: 600px;
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