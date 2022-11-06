import { createApp } from "vue";
import App from "~/component/Console.vue";
const app = createApp(App);
import "xterm/css/xterm.css"
import "~/main.css"
app
  .mount("#app");