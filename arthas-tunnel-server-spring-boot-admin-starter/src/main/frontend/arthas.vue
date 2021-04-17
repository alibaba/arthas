<template>
  <!--  <pre v-text="stringify(applications, null, 4)" />-->
  <div id="arthas" class="section" style="padding: 3rem 0rem">
    <nav class="navbar navbar-expand navbar-light bg-light flex-column flex-md-row bd-navbar">
      <a href="https://github.com/alibaba/arthas" target="_blank" title="" class="navbar-brand">
        <img src="data:image/png;base64,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"
             alt="Arthas" title="Welcome to Arthas web console" class="img-responsive"></a>

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
            <a class="nav-link" href="https://arthas.aliyun.com/doc/arthas-tutorials.html" target="_blank">Tutorials</a>
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
              <span class="input-group-text" id="ip-addon">Host</span>
            </div>
            <input type="text" v-model="host"  class="form-control" name="host" id="host"
                   placeholder="please enter host" aria-label="host" aria-describedby="host-addon">
          </div>
        </div>
        <div class="col">
          <div class="input-group ">
            <div class="input-group-prepend">
              <span class="input-group-text" id="port-addon">Port</span>
            </div>
            <input v-model="port" type="text" class="form-control" name="port" id="port"
                   placeholder="please enter port" aria-label="port" aria-describedby="port-addon">
          </div>
        </div>
        <div class="col">
          <div class="input-group ">
            <div class="input-group-prepend">
              <span class="input-group-text" id="service-addon">Service</span>
            </div>
            <select id="service" class="form-control" v-on:change="changeService" v-model="currentServiceName">
              <option value="">Select Service</option>
              <option :value="item" v-for="(item) in serviceNames">{{ item }}</option>
            </select>
          </div>
        </div>

        <div class="col">
          <div class="input-group ">
            <div class="input-group-prepend">
              <span class="input-group-text" id="instance-addon">Instance</span>
            </div>
            <select id="instance" class="form-control" v-model="currentInstanceAgentId">
              <option value="">Select Instance</option>
              <option :value="instance.value" v-for="instance in instanceAgent">{{ instance.name }}</option>
            </select>
          </div>
        </div>

        <div class="col-inline">
          <button class="btn btn-info form-control" @click="startConnect" type="button" style="margin-right: 5px;">Connect</button>
          <button class="btn btn-info form-control" @click="disconnect" type="button">Disconnect</button>
        </div>
      </form>
    </nav>
    <div class="container-fluid px-0">
      <div class="col px-0" id="terminal-card">
        <div id="terminal"></div>
      </div>
    </div>
    <div title="fullscreen" id="fullSc" class="fullSc">
      <button id="fullScBtn" @click="xtermFullScreen"><img
          src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAB5UlEQVRYR+2XQU8TURSFz2lLCHa6QJopbExcmHSmaogbd8oPENeucW1YWxcCCbAnbA2/AfEHwM7EBYmGRxP2hj6MLphaDWWu6ZQhdBjo9IURSPq28+49X857b+69xDUvhvp/fzxwjo5z0wLeSZOJkN9D2dbGcHFvt60TAHi6MgnIdprC0dwEnuRttR0ANLRbFWBRIFsQbqYKQpki+JzAu7ytlgKAw7o7R+K9COYLJTWXJkBU6+YCeHXnM8inXW6I/GGWr/JFtR7n0qF2Ntv2XhaT2AFPu3JOxASgk2TZslU17rgvPIIQwLLV6VPt926El3sAYOZA3d1vW26V1Hi/1of7G9qdEeCDALMFW630eQcqgbBl7wQgJksEbPwqP8yP1nZI+H0BmAgmiUn8DJMkM9kzABg4cHsc8LTzqfMf2H1hctuDRuegPO37XMtk+TqsoIkdGBSjG+NA0KieLIJNAtV2NxvbEdWdBRDPTvcL7oG8b14NiVJUSOC/Kdi11TgATzvfAU6cjzGshsLj8tlkWWaaI2PqS1jZokKeroz3ikn8CkyfXq+4WABPu28BLP3PwQRA1bLVcmc0+1l+hFbmay/6K/2e8x9bd2vfuodTP/dShCNXKhRJRkpzKNP62DWcpinYK/c/z0ExP3rClK8AAAAASUVORK5CYII=">
      </button>
    </div>
  </div>
</template>

<script>
import $ from 'jquery';
import {Terminal} from 'xterm';
import "bootstrap/dist/css/bootstrap.min.css"
import "bootstrap/dist/css/bootstrap-grid.min.css"
import "bootstrap/dist/css/bootstrap-reboot.min.css"
import "xterm/css/xterm.css"

export default {
  data() {
    return {
      ws: null,
      xterm: null,
      arthasAgents: null,
      applications: null,
      host:window.location.host.split(":")[0],
      port:"7777",
      serviceNames: [],
      instanceAgent: [],
      currentInstanceAgentId: "",
      currentServiceName: "",
    }
  },
  mounted() {
    this.reloadRegisterArthasAgent();
    window.addEventListener('resize', this.handleResize)
  },
  methods: {
    handleResize() {
      var terminalSize = this.getTerminalSize();
      if (this.ws) {
        this.ws.send(JSON.stringify({action: 'resize', cols: terminalSize.cols, rows: terminalSize.rows}));
      }
      if (this.xterm) {
        this.xterm.resize(terminalSize.cols, terminalSize.rows);
      }
    },
    getCharSize() {
      let tempDiv = $('<div />').attr({'role': 'listitem'});
      let tempSpan = $('<div />').html('qwertyuiopasdfghjklzxcvbnm');
      tempDiv.append(tempSpan);
      $("html body").append(tempDiv);
      let size = {
        width: tempSpan.outerWidth() / 26,
        height: tempSpan.outerHeight(),
        left: tempDiv.outerWidth() - tempSpan.outerWidth(),
        top: tempDiv.outerHeight() - tempSpan.outerHeight(),
      };
      tempDiv.remove();
      return size;
    },
    getWindowSize() {
      let e = window;
      let a = 'inner';
      if (!('innerWidth' in window)) {
        a = 'client';
        e = document.documentElement || document.body;
      }
      let terminalDiv = document.getElementById("terminal-card");
      let terminalDivRect = terminalDiv.getBoundingClientRect();
      return {
        width: terminalDivRect.width,
        height: e[a + 'Height'] - terminalDivRect.top
      };
    },
    getTerminalSize() {
      let charSize = this.getCharSize();
      let windowSize = this.getWindowSize();
      console.log('charsize');
      console.log(charSize);
      console.log('windowSize');
      console.log(windowSize);
      return {
        cols: Math.floor((windowSize.width - charSize.left) / 10),
        rows: Math.floor((windowSize.height - charSize.top) / 9.6)
      };
    },

    /** init websocket **/
    initWs(host, port, agentId) {
      let protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
      let path = protocol + host + ':' + port + '/ws?method=connectArthas&id=' + agentId;
      this.ws = new WebSocket(path);
    },

    /** init xterm **/
    initXterm(cols, rows) {
      this.xterm = new Terminal({
        cols: cols,
        rows: rows,
        screenReaderMode: true,
        rendererType: 'canvas',
        convertEol: true
      });
    },
    /** 有修改 begin connect **/
    startConnect() {
      let host = this.host;
      let port =this.port;
      let agentId = this.currentInstanceAgentId;
      if (host === '' ) {
        host=window.location.host.split(":")[0];
      }
      if (port === '') {
        port="7777"
      }
      if (agentId === '') {
        alert('AgentId can not be empty');
        return;
      }
      if (this.ws != null) {
        alert('Already connected');
        return;
      }
      // init webSocket
      this.initWs(host, port, agentId);
      this.ws.onerror = () => {
        this.ws.close();
        this.ws = null;
        alert('Connect error');
      };
      this.ws.onclose = (message) => {
        if (message.code === 2000) {
          alert(message.reason);
        }
      };
      this.ws.onopen = () => {
        console.log('open');
        $('#fullSc').show();
        let terminalSize = this.getTerminalSize()
        console.log('terminalSize')
        console.log(terminalSize)
        // init xterm
        this.initXterm(terminalSize.cols, terminalSize.rows)
        this.ws.onmessage = (event) => {
          if (event.type === 'message') {
            let data = event.data;
            this.xterm.write(data);
          }
        }

        this.xterm.open(document.getElementById('terminal'));
        this.xterm.onData((data) => {
          this.ws.send(JSON.stringify({action: 'read', data: data}))
        })

        this.ws.send(JSON.stringify({action: 'resize', cols: terminalSize.cols, rows: terminalSize.rows}));
        window.setInterval(() => {
          if (this.ws != null && this.ws.readyState === 1) {
            this.ws.send(JSON.stringify({action: 'read', data: ""}));
          }
        }, 30000);
      }
    },
    disconnect() {
      try {
        this.ws.close();
        this.ws.onmessage = null;
        this.ws.onclose = null;
        this.ws = null;
        this.xterm.dispose();
        $('#fullSc').hide();
        alert('Connection was closed successfully!');
      } catch (e) {
        alert('No connection, please start connect first.');
        console.log(e)
      }
    },
    /** full screen show **/
    xtermFullScreen() {
      let ele = document.getElementById('terminal-card');
      this.requestFullScreen(ele);
    },

    requestFullScreen(element) {
      let requestMethod = element.requestFullScreen || element.webkitRequestFullScreen || element.mozRequestFullScreen || element.msRequestFullScreen;
      if (requestMethod) {
        requestMethod.call(element);
      } else if (typeof window.ActiveXObject !== "undefined") {
        let wscript = new ActiveXObject("WScript.Shell");
        if (wscript !== null) {
          wscript.SendKeys("{F11}");
        }
      }
    },
    /**
     * 获取注册的arthas客户端
     */
    reloadRegisterArthasAgent() {
      this.arthasAgents = this.reqSync("/api/arthas/clients", "get");
      this.serviceNames = []
      for (let key in this.arthasAgents) {
        this.serviceNames.push(key)
      }
    },
    changeService() {
      this.instanceAgent = []
      this.getOrDefault(this.arthasAgents[this.currentServiceName], []).forEach(ins => {
        let agentId = ins.agentId
        let serviceId = this.getOrDefault(ins.serviceId, "NoneId")
        let serviceIp = this.getOrDefault(ins.serviceIp, "NoneIp")
        let servicePort = this.getOrDefault(ins.servicePort, "NonePort")
        this.instanceAgent.push({
          name: serviceIp + ':' + servicePort+ ' ' + serviceId,
          value: agentId
        })
      })
    },
    getOrDefault(value, defaultValue) {
      return value ? value : defaultValue
    },
    reqSync(url, method) {
      let result = null;
      $.ajax({
        url: url,
        type: method,
        async: false, //使用同步的方式,true为异步方式
        headers: {
          'Content-Type': 'application/json;charset=utf8;',
          'Accept': 'application/json'
        },
        success: function (data) {
          // console.log(data);
          result = data;
        },
        error: function (data) {
          console.log("error");
        }
      });
      return result;
    }
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
  }
};
</script>

<style>

#terminal:-webkit-full-screen {
  background-color: rgb(255, 255, 12);
}

.fullSc {
  z-index: 10000;
  position: fixed;
  top: 25%;
  left: 90%;
  display: none;
}

#fullScBtn {
  border-radius: 17px;
  border: 0;
  cursor: pointer;
  background-color: black;
}

</style>