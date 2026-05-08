<template>
  <div class="login-page" :class="{ dark: isDark }">
    <div class="login-container">
      <h1 class="title">视频平台</h1>
      
      <div class="card">
        <h2>登录</h2>
        
        <div class="form-group">
          <label>用户名</label>
          <input 
            v-model="form.username" 
            type="text" 
            placeholder="请输入用户名"
            class="input"
          />
        </div>

        <div class="form-group">
          <label>密码</label>
          <input 
            v-model="form.password" 
            type="password" 
            placeholder="请输入密码"
            class="input"
          />
        </div>

        <div class="form-group">
          <label>登录身份</label>
          <select v-model="form.role" class="select">
            <option value="user">普通用户</option>
            <option value="admin">管理员</option>
          </select>
        </div>

        <button class="btn-primary" @click="login" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <div class="links">
          <a @click="$router.push('/register')">注册账号</a>
          <a @click="$router.push('/find-pwd')">找回密码</a>
        </div>
      </div>

      <button class="btn-text" @click="toggleTheme">
        {{ isDark ? '浅色模式' : '深色模式' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { setUserInfo, clearUserInfo } from '../../utils/userStorage'

const router = useRouter()
const loading = ref(false)
const form = ref({
  username: '',
  password: '',
  role: 'user'
})

const isDark = ref(false)

const toggleTheme = () => {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'dark') {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }

  const userInfo = sessionStorage.getItem('userInfo')
  if (userInfo) {
    const user = JSON.parse(userInfo)
    if (!user.id || !user.role) sessionStorage.removeItem('userInfo')
  }
})

const login = async () => {
  if (loading.value) return
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请完善表单信息！')
    return
  }

  try {
    loading.value = true

    // 调用后端登录API
    const res = await fetch('/api/user/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: form.value.username,
        password: form.value.password,
        role: form.value.role
      })
    })
    const result = await res.json()

    if (result.code !== 200) {
      ElMessage.error(result.msg || '登录失败')
      loading.value = false
      return
    }

    let userInfo = result.data.user
    let token = result.data.token
    const isRecover = result.data && result.data.recover
    if (isRecover) {
      userInfo = result.data.user
    }
    userInfo.token = token
    setUserInfo(userInfo)

    if (userInfo.status === 1) {
      ElMessage.error('账号已被封禁')
      clearUserInfo()
      loading.value = false
      return
    }

    if (isRecover) {
      if (confirm('您的账号正在注销流程中，登录后将取消注销，是否继续？')) {
        userInfo.delFlag = 0
        setUserInfo(userInfo)
        ElMessage.success('登录成功！已恢复账号')
        router.push('/index')
      } else {
        clearUserInfo()
        loading.value = false
        return
      }
    } else {
      ElMessage.success('登录成功！')
      router.push('/index')
    }
    
  } catch (e) {
    if (e === 'cancel') {
      ElMessage.info('已取消')
      sessionStorage.removeItem('userInfo')
    } else {
      ElMessage.error('登录失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  padding: 24px;
}

.login-container {
  width: 100%;
  max-width: 360px;
}

.title {
  font-size: 24px;
  font-weight: 600;
  text-align: center;
  margin-bottom: 24px;
  color: var(--text-primary);
}

.card {
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 32px 24px;
}

.card h2 {
  font-size: 20px;
  font-weight: 600;
  text-align: center;
  margin-bottom: 24px;
  color: var(--text-primary);
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.input,
.select {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 14px;
}

.input:focus,
.select:focus {
  outline: none;
  border-color: var(--text-primary);
}

.input::placeholder {
  color: var(--text-tertiary);
}

.btn-primary {
  width: 100%;
  padding: 12px;
  border: none;
  background: var(--text-primary);
  color: var(--bg-primary);
  font-size: 14px;
  font-weight: 500;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 8px;
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.links {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 20px;
}

.links a {
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
}

.links a:hover {
  color: var(--text-primary);
}

.btn-text {
  display: block;
  width: 100%;
  padding: 12px;
  margin-top: 16px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  text-align: center;
  cursor: pointer;
}

.btn-text:hover {
  color: var(--text-primary);
}

/* 深色模式 */
.dark .login-page {
  background: var(--dark-bg-secondary);
}

.dark .title {
  color: var(--dark-text-primary);
}

.dark .card {
  background: var(--dark-bg-primary);
  border-color: var(--dark-border);
}

.dark .card h2 {
  color: var(--dark-text-primary);
}

.dark .form-group label {
  color: var(--dark-text-primary);
}

.dark .input,
.dark .select {
  background: var(--dark-bg-primary);
  border-color: var(--dark-border);
  color: var(--dark-text-primary);
}

.dark .input::placeholder {
  color: var(--dark-text-tertiary);
}

.dark .links a {
  color: var(--dark-text-secondary);
}

.dark .btn-text {
  color: var(--dark-text-secondary);
}

/* 响应式 */
@media (max-width: 480px) {
  .card {
    padding: 24px 16px;
  }
  
  .title {
    font-size: 20px;
  }
  
  .card h2 {
    font-size: 18px;
  }
}
</style>
