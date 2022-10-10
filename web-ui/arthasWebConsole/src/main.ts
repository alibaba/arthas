import { createApp } from "vue";
import App from "./App.vue";
const app = createApp(App);
import 'bootstrap/dist/css/bootstrap.min.css';
// import $ from "jquery"
import "xterm/css/xterm.css"
import "./main.css"
app
  .mount("#app");