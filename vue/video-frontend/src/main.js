import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'
import './assets/styles/main.scss'

const app = createApp(App)

app.config.errorHandler = (err) => {
  if (err.message && err.message.includes('ResizeObserver loop limit exceeded')) {
    return
  }
  console.error('Vue Runtime Error:', err)
}

if (!window.ResizeObserver) {
  window.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
}

app.use(ElementPlus)
app.use(router)

const isDark = localStorage.getItem('theme') === 'dark'
if (isDark) {
  document.documentElement.classList.add('dark')
}

app.mount('#app')
