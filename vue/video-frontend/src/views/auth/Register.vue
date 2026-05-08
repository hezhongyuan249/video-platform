<template>
  <div class="register-page" :class="{ dark: isDark }">
    <div class="register-container">
      <h1 class="title">视频平台</h1>
      
      <div class="card">
        <h2>注册账号</h2>
        
        <div class="form-group">
          <label>用户名</label>
          <input v-model="form.username" type="text" placeholder="请输入用户名" class="input" />
        </div>

        <div class="form-group">
          <label>密码</label>
          <input v-model="form.password" type="password" placeholder="8位以上+大小写+数字+特殊字符" class="input" @input="checkPasswordRequirements" />
          <div class="password-requirements" v-if="form.password">
            <div class="req-item" :class="{ met: passwordRequirements.length }">
              <span class="req-icon">{{ passwordRequirements.length ? '✓' : '○' }}</span>
              <span>至少8位</span>
            </div>
            <div class="req-item" :class="{ met: passwordRequirements.lower }">
              <span class="req-icon">{{ passwordRequirements.lower ? '✓' : '○' }}</span>
              <span>小写字母</span>
            </div>
            <div class="req-item" :class="{ met: passwordRequirements.upper }">
              <span class="req-icon">{{ passwordRequirements.upper ? '✓' : '○' }}</span>
              <span>大写字母</span>
            </div>
            <div class="req-item" :class="{ met: passwordRequirements.number }">
              <span class="req-icon">{{ passwordRequirements.number ? '✓' : '○' }}</span>
              <span>数字</span>
            </div>
            <div class="req-item" :class="{ met: passwordRequirements.special }">
              <span class="req-icon">{{ passwordRequirements.special ? '✓' : '○' }}</span>
              <span>特殊字符</span>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label>确认密码</label>
          <input v-model="form.confirmPwd" type="password" placeholder="请再次输入密码" class="input" />
        </div>

        <div class="form-group">
          <label>密保问题</label>
          <input v-model="form.question" type="text" placeholder="设置密保问题（找回密码用）" class="input" />
        </div>

        <div class="form-group">
          <label>密保答案</label>
          <input v-model="form.answer" type="text" placeholder="设置密保答案" class="input" />
        </div>

        <div class="form-group">
          <label>注册身份</label>
          <select v-model="form.role" class="select" @change="handleRoleChange">
            <option value="user">普通用户</option>
            <option value="admin">管理员</option>
          </select>
        </div>

        <div class="form-group" v-if="form.role === 'admin'">
          <label>管理员注册码</label>
          <input v-model="form.adminCode" type="text" placeholder="请输入管理员注册码" class="input" />
        </div>

        <button class="btn-primary" @click="register" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>

        <div class="links">
          <a @click="$router.push('/login')">已有账号？去登录</a>
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
const form = ref({
  username: '',
  password: '',
  confirmPwd: '',
  question: '',
  answer: '',
  role: 'user',
  adminCode: ''
});

const passwordRequirements = ref({
  length: false,
  lower: false,
  upper: false,
  number: false,
  special: false
});

const checkPasswordRequirements = function() {
  const pwd = form.value.password;
  
  passwordRequirements.value = {
    length: pwd.length >= 8,
    lower: /[a-z]/.test(pwd),
    upper: /[A-Z]/.test(pwd),
    number: /[0-9]/.test(pwd),
    special: /[^a-zA-Z0-9]/.test(pwd)
  };
};

const validatePassword = function(password) {
  if (password.length < 8) {
    return '密码长度至少8位';
  }
  if (!/[a-z]/.test(password)) {
    return '密码必须包含小写字母';
  }
  if (!/[A-Z]/.test(password)) {
    return '密码必须包含大写字母';
  }
  if (!/[0-9]/.test(password)) {
    return '密码必须包含数字';
  }
  if (!/[^a-zA-Z0-9]/.test(password)) {
    return '密码必须包含特殊字符';
  }
  return '';
};

const register = async function() {
  if (loading.value) return;
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请完善表单信息！');
    return;
  }
  
  const pwdError = validatePassword(form.value.password);
  if (pwdError) {
    ElMessage.warning(pwdError);
    return;
  }
  
  if (form.value.password !== form.value.confirmPwd) {
    ElMessage.warning('两次密码输入不一致！');
    return;
  }
  if (!form.value.question || !form.value.answer) {
    ElMessage.warning('请设置密保问题和答案！');
    return;
  }
  if (form.value.role === 'admin' && !form.value.adminCode) {
    ElMessage.warning('请输入管理员注册码！');
    return;
  }

  try {
    loading.value = true;
    
    // 调用后端注册API
    const res = await fetch('/api/user/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: form.value.username,
        password: form.value.password,
        question: form.value.question,
        answer: form.value.answer,
        role: form.value.role,
        adminCode: form.value.adminCode
      })
    });
    const result = await res.json();
    
    if (result.code !== 200) {
      ElMessage.error(result.msg || '注册失败');
      loading.value = false;
      return;
    }
    
    sessionStorage.setItem('userSecurity', JSON.stringify({
      username: form.value.username,
      question: form.value.question,
      answer: form.value.answer
    }));
    
    ElMessage.success('注册成功！请登录');
    router.push('/login');
  } catch (e) {
    ElMessage.error('注册失败');
  } finally {
    loading.value = false;
  }
};

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

const handleRoleChange = function() {
  if (form.value.role !== 'admin') {
    form.value.adminCode = '';
  }
};

</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  padding: 24px;
}

.register-container {
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
.dark .register-page {
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

.password-strength {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.strength-bar {
  flex: 1;
  height: 4px;
  background: var(--border);
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  transition: all 0.3s ease;
  border-radius: 2px;
}

.strength-fill.weak { background: #ff4d4f; }
.strength-fill.medium { background: #faad14; }
.strength-fill.strong { background: #52c41a; }

.strength-text {
  font-size: 12px;
  min-width: 30px;
}

.strength-text.weak { color: #ff4d4f; }
.strength-text.medium { color: #faad14; }
.strength-text.strong { color: #52c41a; }

.password-requirements {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.req-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.req-item.met {
  color: #52c41a;
}

.req-icon {
  font-size: 10px;
}
</style>
