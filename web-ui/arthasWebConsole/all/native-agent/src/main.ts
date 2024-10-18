import { createApp, h } from "vue";
// import App from "~/component/Console.vue";
import NativeAgnet from "./Agent.vue"; 
// const app = createApp(h(App, { isTunnel: true }));
const app = createApp(h(NativeAgnet))
import "xterm/css/xterm.css";
import "~/main.css";
app
  .mount("#app");
