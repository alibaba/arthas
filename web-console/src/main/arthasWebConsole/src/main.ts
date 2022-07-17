import { createApp } from 'vue'
import './index.css'
import router from './router/index'
import { createPinia } from 'pinia'
import App from './App.vue'

const app = createApp(App)

app.use(router)
  .use(createPinia())
  .mount('#app')

// router.isReady().then(() => app.mount('#app'))