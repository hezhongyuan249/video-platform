<template>
  <div class="update-page" :class="{ dark: isDark }">
    <header class="header">
      <div class="header-left">
        <span class="title">视频平台</span>
      </div>
      <div class="header-right">
        <button class="btn-text" @click="toggleTheme">{{ isDark ? '浅色' : '深色' }}</button>
        <button class="btn-text" @click="goHome">返回首页</button>
      </div>
    </header>

    <div class="content-wrapper">
      <div class="settings-container">
        <h2>个人设置</h2>
        
        <div class="settings-grid">
          <!-- 左侧列 -->
          <div class="settings-column">
            <!-- 头像设置 -->
            <div class="setting-card">
              <h3>头像设置</h3>
              <div class="avatar-section">
                <div class="avatar-wrapper" @click="triggerAvatarUpload">
                  <img :src="avatarPreview || defaultAvatar" alt="头像" class="avatar-img" />
                  <div class="avatar-overlay">
                    <span>更换头像</span>
                  </div>
                </div>
                <input 
                  type="file" 
                  ref="avatarInput"
                  accept="image/*" 
                  style="display: none" 
                  @change="handleAvatarChange"
                />
              </div>
              <div class="form-group">
                <label>头像链接</label>
                <input v-model="form.avatar" type="text" placeholder="输入头像图片URL或点击上方上传" class="input" @blur="updateAvatarPreview" />
              </div>
              <div class="form-group">
                <label>当前密码</label>
                <input v-model="form.password" type="password" placeholder="请输入当前密码验证" class="input" />
              </div>
              <button class="btn-primary" @click="updateAvatar" :disabled="loading">
                {{ loading ? '保存中...' : '保存头像' }}
              </button>
            </div>

            <!-- 修改密码 -->
            <div class="setting-card">
              <h3>修改密码</h3>
              <div class="form-group">
                <label>旧密码</label>
                <input v-model="form.oldPassword" type="password" placeholder="请输入旧密码" class="input" />
              </div>
              <div class="form-group">
                <label>新密码</label>
                <input v-model="form.newPassword" type="password" placeholder="8位以上+大小写+数字+特殊字符" class="input" @input="checkPasswordRequirements" />
                <div class="password-requirements" v-if="form.newPassword">
                  <div class="req-item" :class="{ met: passwordRequirements.length }">
                    <span class="req-icon">{{ passwordRequirements.length ? '✓' : '○' }}</span>8位
                  </div>
                  <div class="req-item" :class="{ met: passwordRequirements.lower }">
                    <span class="req-icon">{{ passwordRequirements.lower ? '✓' : '○' }}</span>小写
                  </div>
                  <div class="req-item" :class="{ met: passwordRequirements.upper }">
                    <span class="req-icon">{{ passwordRequirements.upper ? '✓' : '○' }}</span>大写
                  </div>
                  <div class="req-item" :class="{ met: passwordRequirements.number }">
                    <span class="req-icon">{{ passwordRequirements.number ? '✓' : '○' }}</span>数字
                  </div>
                  <div class="req-item" :class="{ met: passwordRequirements.special }">
                    <span class="req-icon">{{ passwordRequirements.special ? '✓' : '○' }}</span>特殊
                  </div>
                </div>
              </div>
              <button class="btn-primary" @click="updatePassword" :disabled="loading4">
                {{ loading4 ? '保存中...' : '修改密码' }}
              </button>
            </div>
          </div>

          <!-- 右侧列 -->
          <div class="settings-column">
            <!-- 修改用户名 -->
            <div class="setting-card">
              <h3>修改用户名</h3>
              <div class="form-group">
                <label>新用户名</label>
                <input v-model="form.username" type="text" placeholder="请输入新用户名" class="input" />
              </div>
              <div class="form-group">
                <label>当前密码</label>
                <input v-model="form.password2" type="password" placeholder="请输入当前密码验证" class="input" />
              </div>
              <button class="btn-primary" @click="updateUsername" :disabled="loading2">
                {{ loading2 ? '保存中...' : '修改用户名' }}
              </button>
            </div>

            <!-- 修改密保 -->
            <div class="setting-card">
              <h3>修改密保</h3>
              <div class="form-group">
                <label>新密保问题</label>
                <input v-model="form.question" type="text" placeholder="请输入新密保问题" class="input" />
              </div>
              <div class="form-group">
                <label>密保答案</label>
                <input v-model="form.answer" type="text" placeholder="请输入密保答案" class="input" />
              </div>
              <div class="form-group">
                <label>当前密码</label>
                <input v-model="form.password3" type="password" placeholder="请输入当前密码验证" class="input" />
              </div>
              <button class="btn-primary" @click="updateSecurity" :disabled="loading3">
                {{ loading3 ? '保存中...' : '修改密保' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

const router = useRouter();
const loading = ref(false);
const loading2 = ref(false);
const loading3 = ref(false);
const loading4 = ref(false);
const passwordRequirements = ref({
  length: false,
  lower: false,
  upper: false,
  number: false,
  special: false
});

const checkPasswordRequirements = function() {
  const pwd = form.value.newPassword || '';
  passwordRequirements.value = {
    length: pwd.length >= 8,
    lower: /[a-z]/.test(pwd),
    upper: /[A-Z]/.test(pwd),
    number: /[0-9]/.test(pwd),
    special: /[^a-zA-Z0-9]/.test(pwd)
  };
};

const avatarInput = ref(null);
const avatarPreview = ref('');
const isDark = ref(false);

const user = JSON.parse(sessionStorage.getItem('userInfo') || '{}');

const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI1MCIgZmlsbD0iI2UzZTNlMyIvPjx0ZXh0IHg9IjUwIiB5PSI1NSIgZm9udC1zaXplPSIyMCIgZmlsbD0iIzY2NiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPjwvdGV4dD48L3N2Zz4=';

const form = ref({ 
  avatar: user.avatar || '',
  username: '',
  password: '',
  password2: '',
  password3: '',
  password4: '',
  oldPassword: '',
  newPassword: '',
  question: '',
  answer: ''
});

const toggleTheme = function() {
  isDark.value = !isDark.value;
  document.documentElement.classList.toggle('dark', isDark.value);
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light');
};

const goHome = function() {
  router.push('/index');
};

const updateAvatar = async function() {
  if (!form.value.password) {
    ElMessage.warning('请输入密码验证');
    return;
  }
  if (!form.value.avatar) {
    ElMessage.warning('请选择头像');
    return;
  }

  try {
    loading.value = true;
    
    const res = await fetch('/api/user/updateAvatar', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: user.id,
        avatar: form.value.avatar,
        password: form.value.password
      }),
      credentials: 'include'
    });
    const result = await res.json();
    
    if (result.code === 200) {
      ElMessage.success('头像更新成功！');
      const updatedUser = { ...user, avatar: form.value.avatar };
      sessionStorage.setItem('userInfo', JSON.stringify(updatedUser));
      form.value.password = '';
    } else {
      ElMessage.error(result.msg || '更新失败');
    }
  } catch (e) {
    ElMessage.error('更新失败');
  } finally {
    loading.value = false;
  }
};

const updateUsername = async function() {
  if (!form.value.username) {
    ElMessage.warning('请输入新用户名');
    return;
  }
  if (!form.value.password2) {
    ElMessage.warning('请输入密码验证');
    return;
  }

  try {
    loading2.value = true;
    
    const res = await fetch('/api/user/updateUsername', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: user.id,
        username: form.value.username,
        password: form.value.password2
      }),
      credentials: 'include'
    });
    const result = await res.json();
    
    if (result.code === 200) {
      ElMessage.success('用户名更新成功！请重新登录');
      sessionStorage.removeItem('userInfo');
      router.push('/login');
    } else {
      ElMessage.error(result.msg || '更新失败');
    }
  } catch (e) {
    ElMessage.error('更新失败');
  } finally {
    loading2.value = false;
  }
};

const updateSecurity = async function() {
  if (!form.value.question) {
    ElMessage.warning('请输入密保问题');
    return;
  }
  if (!form.value.answer) {
    ElMessage.warning('请输入密保答案');
    return;
  }
  if (!form.value.password3) {
    ElMessage.warning('请输入密码验证');
    return;
  }

  try {
    loading3.value = true;
    
    const res = await fetch('/api/user/updateSecurity', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: user.id,
        question: form.value.question,
        answer: form.value.answer,
        password: form.value.password3
      }),
      credentials: 'include'
    });
    const result = await res.json();
    
    if (result.code === 200) {
      ElMessage.success('密保更新成功！');
      form.value.question = '';
      form.value.answer = '';
      form.value.password3 = '';
    } else {
      ElMessage.error(result.msg || '更新失败');
    }
  } catch (e) {
    ElMessage.error('更新失败');
  } finally {
    loading3.value = false;
  }
};

const updatePassword = async function() {
  if (!form.value.oldPassword) {
    ElMessage.warning('请输入旧密码');
    return;
  }
  if (!form.value.newPassword) {
    ElMessage.warning('请输入新密码');
    return;
  }
  
  const pwd = form.value.newPassword;
  if (pwd.length < 8 || !/[a-z]/.test(pwd) || !/[A-Z]/.test(pwd) || !/[0-9]/.test(pwd) || !/[^a-zA-Z0-9]/.test(pwd)) {
    ElMessage.warning('密码需8位以上+大小写+数字+特殊字符');
    return;
  }

  try {
    loading4.value = true;
    
    const res = await fetch('/api/user/updatePwd', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: user.id,
        oldPassword: form.value.oldPassword,
        password: form.value.newPassword
      }),
      credentials: 'include'
    });
    const result = await res.json();
    
    if (result.code === 200) {
      ElMessage.success('密码修改成功！请重新登录');
      sessionStorage.removeItem('userInfo');
      router.push('/login');
    } else {
      ElMessage.error(result.msg || '修改失败');
    }
  } catch (e) {
    ElMessage.error('修改失败');
  } finally {
    loading4.value = false;
  }
};

const triggerAvatarUpload = function() {
  avatarInput.value.click();
};

const handleAvatarChange = function(e) {
  const file = e.target.files[0];
  if (!file) return;
  
  const reader = new FileReader();
  reader.onload = function(event) {
    form.value.avatar = event.target.result;
    avatarPreview.value = event.target.result;
  };
  reader.readAsDataURL(file);
};

const updateAvatarPreview = function() {
  if (form.value.avatar) {
    avatarPreview.value = form.value.avatar;
  }
};

onMounted(function() {
  const savedTheme = localStorage.getItem('theme');
  if (savedTheme === 'dark') {
    isDark.value = true;
    document.documentElement.classList.add('dark');
  }
  
  if (user.avatar) {
    avatarPreview.value = user.avatar;
  }
});
</script>

<style scoped>
.update-page {
  min-height: 100vh;
  background: var(--bg-primary);
  color: var(--text-primary);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
}

.header-left .title {
  font-size: 20px;
  font-weight: bold;
}

.header-right {
  display: flex;
  gap: 12px;
}

.content-wrapper {
  padding: 40px 20px;
}

.settings-container {
  max-width: 1000px;
  margin: 0 auto;
}

h2 {
  margin: 0 0 24px;
  font-size: 24px;
  text-align: center;
}

.settings-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.settings-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.setting-card {
  background: var(--bg-secondary);
  padding: 24px;
  border-radius: 12px;
}

.setting-card h3 {
  margin: 0 0 20px;
  font-size: 18px;
  color: var(--text-primary);
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.avatar-wrapper {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  border: 3px solid var(--border-color);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-overlay span {
  color: #fff;
  font-size: 12px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

.input {
  width: 100%;
  padding: 12px;
  font-size: 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-primary);
  box-sizing: border-box;
}

.input:focus {
  outline: none;
  border-color: var(--primary-500);
}

.btn-primary {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  font-weight: 500;
  color: #fff;
  background: var(--primary-500);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
}

.btn-primary:hover:not(:disabled) {
  background: var(--primary-600);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-text {
  padding: 8px 12px;
  font-size: 14px;
  color: var(--text-secondary);
  background: none;
  border: none;
  cursor: pointer;
}

.btn-text:hover {
  color: var(--text-primary);
}

.password-requirements {
  margin-top: 8px;
  font-size: 12px;
}

.req-item {
  display: inline-block;
  margin-right: 10px;
  color: var(--text-tertiary);
}

.req-item.met {
  color: #10b981;
}

.req-icon {
  margin-right: 4px;
}

@media (max-width: 768px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>
