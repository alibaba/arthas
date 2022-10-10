import { createApp } from "vue";
import "./index.css";
import router from "./router/index";
import { createPinia } from "pinia";
import App from "./App.vue";
import "highlight.js/styles/stackoverflow-light.css";
const app = createApp(App);

app.use(router)
  .use(createPinia())
  .mount("#app");
