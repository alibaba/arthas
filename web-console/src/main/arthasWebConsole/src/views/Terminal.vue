<script setup lang="ts">
import { onMounted } from "vue";
import { Terminal } from "xterm"
import { FitAddon } from "xterm-addon-fit"
import { createMachine } from "xstate"
import { useMachine } from "@xstate/vue"
import "xterm/css/xterm.css";
import { readSync } from "fs";
import { toUnicode } from "punycode";
// import "xterm/lib/xterm.js";


const term = new Terminal({
  fontSize: 14,
  cursorBlink: true,
});
// const termMachine = createMachine({
//   id: 'terminal',
//   initial: 'input',
//   states: {
//     input: {
//       // entry: () => {
//       //     term.write('Hello from \x1B[1;3;31mxterm.js\x1B[0m $ ')
//       // },
//       on: {
//         // if input 'enter'
//         ENTER: {
//           target:'output',
//           actions:[()=>{
//             // send http request
//           }]
//         }
//       }
//     },
//     ouput: {
//       on:{
//         SHOW: {
//           target:'input',
//           actions:[
//             ()=>{
              
//             }
//           ]
//         }
//       }
//     },
//   }
// })

// const { state, send } = useMachine(termMachine)

const initTerm = () => {
  // const attachAddon = new AttachAddon(this.socket);
  const fitAddon = new FitAddon();
  // term.loadAddon(attachAddon);
  term.loadAddon(fitAddon);
  term.open(document.getElementById('xterm')!);
  term.onData((val) => {
    if(val == '\r'){
      term.write('\r\n')
    } else if(val == '\b'){
      term.write('\b \b')
    } else {
      term.write(val)
    }
  })
  fitAddon.fit();
  // term.focus();
  term.write('Hello from \x1B[1;3;31mxterm.js\x1B[0m $ ')
}
onMounted(() => {
  initTerm()
})
</script>

<template>
  <!-- <div class=" bg-slate-400 w-full h-full "> -->
  <div id="xterm" class="xterm "></div>
  <!-- </div> -->
</template>

<style scoped>
</style>