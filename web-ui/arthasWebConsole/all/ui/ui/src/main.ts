import { createApp } from "vue";
import "~/main.css"
import router from "./router/index";
import { createPinia } from "pinia";
import App from "./App.vue";
import "highlight.js/styles/stackoverflow-light.css";
import timezone from "dayjs/plugin/timezone"
import utc from "dayjs/plugin/utc"
import dayjs from "dayjs";
dayjs.extend(utc)
dayjs.extend(timezone)

const app = createApp(App);

app.use(router)
  .use(createPinia())
  .mount("#app");
