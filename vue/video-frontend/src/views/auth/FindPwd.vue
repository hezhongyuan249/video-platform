<template>
  <div class="find-page" :class="{ dark: isDark }">
    <div class="find-container">
      <h1 class="title">视频平台</h1>
      
      <div class="card">
        <h2>找回密码</h2>
        
        <div class="form-group">
          <label>用户名</label>
          <input v-model="form.username" type="text" placeholder="请输入用户名" class="input" @blur="getQuestion" />
        </div>

        <div class="form-group">
          <label>密保问题</label>
          <div class="question-text">{{ questionText || '请输入用户名后自动显示' }}</div>
        </div>

        <div class="form-group">
          <label>密保答案</label>
          <input v-model="form.answer" type="text" placeholder="请输入答案" class="input" />
        </div>

        <div class="form-group">
          <label>新密码</label>
          <input v-model="form.newPassword" type="password" placeholder="请输入新密码" class="input" />
        </div>

        <button class="btn-primary" @click="findPwd" :disabled="loading">
          {{ loading ? '重置中...' : '重置密码' }}
        </button>

        <div class="links">
          <a @click="toLogin">返回登录</a>
        </div>
      </div>

      <button class="btn-text" @click="toggleTheme">
        {{ isDark ? '浅色模式' : '深色模式' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

const router = useRouter();
const loading = ref(false);
const questionText = ref('');
const form = ref({
  username: '',
  answer: '',
  newPassword: ''
});

const isDark = ref(false);

const toggleTheme = function() {
  isDark.value = !isDark.value;
  document.documentElement.classList.toggle('dark', isDark.value);
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light');
};

onMounted(function() {
  const savedTheme = localStorage.getItem('theme');
  if (savedTheme === 'dark') {
    isDark.value = true;
    document.documentElement.classList.add('dark');
  }
});

const getQuestion = async function() {
  const username = form.value.username.trim();
  if (!username) return;
  
  try {
    const res = await fetch('/api/user/question?username=' + encodeURIComponent(username));
    const result = await res.json();
    
    if (result.code === 200 && result.data) {
      questionText.value = result.data;
    } else {
      questionText.value = result.msg || '用户不存在，请检查用户名';
    }
  } catch (e) {
    questionText.value = '获取密保问题失败';
  }
};

const findPwd = async function() {
  if (loading.value) return;
  if (!form.value.username || !form.value.answer || !form.value.newPassword) {
    ElMessage.warning('请完善表单信息！');
    return;
  }
  
  const userSecurity = sessionStorage.getItem('userSecurity');
  if (userSecurity) {
    const security = JSON.parse(userSecurity);
    if (security.username === form.value.username) {
      if (form.value.answer !== security.answer) {
        ElMessage.error('密保答案错误！');
        return;
      }
    }
  }

  try {
    loading.value = true;
    
    // 调用后端密码重置API
    const res = await fetch('/api/user/resetPwd', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: form.value.username,
        answer: form.value.answer,
        newPassword: form.value.newPassword
      })
    });
    const result = await res.json();
    
    if (result.code !== 200) {
      ElMessage.error(result.msg || '重置失败');
      loading.value = false;
      return;
    }
    
    ElMessage.success('密码重置成功！');
    router.push('/login');
  } catch (e) {
    ElMessage.error('重置失败');
  } finally {
    loading.value = false;
  }
};

const toLogin = function() {
  router.push('/login');
};
</script>

<style scoped>
.find-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  padding: 24px;
}

.find-container {
  width: 100%;
  max-width: 400px;
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
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.input {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 14px;
}

.input:focus {
  outline: none;
  border-color: var(--text-primary);
}

.input::placeholder {
  color: var(--text-tertiary);
}

.question-text {
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: 4px;
  font-size: 14px;
  color: var(--text-secondary);
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
  text-align: center;
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
.dark .find-page {
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

.dark .input {
  background: var(--dark-bg-primary);
  border-color: var(--dark-border);
  color: var(--dark-text-primary);
}

.dark .input::placeholder {
  color: var(--dark-text-tertiary);
}

.dark .question-text {
  background: var(--dark-bg-secondary);
  color: var(--dark-text-secondary);
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
