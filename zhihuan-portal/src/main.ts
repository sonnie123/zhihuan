import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// ⚠️ setupMock 必须在 import App/router 之前!
// 因为 router 内部 import 的 view 会 import api/client.ts,
// client.ts 通过 axios.create() 读取 axios.defaults.adapter。
// setupMock 修改了 axios.defaults.adapter,必须先执行。
import { setupMock } from './mocks/browser'
setupMock()

import App from './App.vue'
import router from './router'
import './design/styles/global.scss'

const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPersistedstate)

app.use(pinia)
app.use(router)
app.use(ElementPlus)

app.mount('#app')

console.log('%c智换 Zhihuan%c v0.1.0', 'color: #7C3AED; font-weight: bold; font-size: 18px;', 'color: #06B6D4; font-weight: bold; font-size: 12px;')
