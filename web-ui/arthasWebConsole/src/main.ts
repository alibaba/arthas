import { createApp } from "vue";
import App from "./App.vue";
const app = createApp(App);
import "xterm/css/xterm.css"
import "./main.css"
app
  .mount("#app");