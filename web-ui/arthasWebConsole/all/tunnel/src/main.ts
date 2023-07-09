import { createApp, h } from "vue";
import App from "./App.vue";
const app = createApp(h(App, { isTunnel: true }));
import "xterm/css/xterm.css";
import "~/main.css";
import '@fortawesome/fontawesome-free/css/all.css'
import router from "./router/router";

app.use(router).mount("#app");
