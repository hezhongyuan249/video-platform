<template>
  <div class="admin-layout" :class="{ dark: isDark }">
    <NavBar />

    <div class="admin-container">
      <div class="sidebar">
        <div class="sidebar-brand" @click="goHome">返回主页</div>
        <router-link to="/admin/user" class="sidebar-item">用户管理</router-link>
        <router-link to="/admin/video/list" class="sidebar-item">视频列表</router-link>
        <router-link to="/admin/video/review" class="sidebar-item">视频审核</router-link>
        <router-link to="/admin/category" class="sidebar-item">分类管理</router-link>
        <router-link to="/admin/video/stat" class="sidebar-item">数据统计</router-link>
      </div>
      <div class="content-area">
        <router-view></router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import NavBar from '../../components/NavBar.vue';

const router = useRouter();
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

const goHome = function() {
  router.push('/index');
  ElMessage.success('返回用户主页');
};

const logout = function() {
  sessionStorage.clear();
  ElMessage.success('退出成功');
  router.push('/login');
};
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-secondary);
}

.header {
  height: 56px;
  background: var(--bg-primary);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.btn-text {
  padding: 8px 12px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
}

.btn-text:hover {
  color: var(--text-primary);
  background: var(--bg-hover);
}

.btn-text.danger {
  color: var(--error-500);
}

.btn-text.danger:hover {
  background: rgba(239, 68, 68, 0.1);
}

.admin-container {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.sidebar {
  width: 160px;
  background: var(--bg-primary);
  border-right: 1px solid var(--border);
  padding: 16px 0;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.sidebar-brand {
  padding: 12px 20px;
  color: var(--primary-500);
  font-size: 14px;
  text-align: center;
  cursor: pointer;
  font-weight: 500;
}

.sidebar-brand:hover {
  color: var(--primary-600);
}

.sidebar-item {
  display: block;
  padding: 12px 20px;
  color: var(--text-secondary);
  font-size: 14px;
  text-decoration: none;
  text-align: center;
}

.sidebar-item:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.sidebar-item.router-link-active {
  background: var(--primary-500);
  color: #fff;
  font-weight: 500;
}

.content-area {
  flex: 1;
  padding: 24px;
  background: var(--bg-secondary);
  overflow-y: auto;
}

/* 深色模式 */
.dark .admin-layout {
  background: var(--dark-bg-secondary);
}

.dark .header {
  background: var(--dark-bg-primary);
  border-color: var(--dark-border);
}

.dark .title {
  color: var(--dark-text-primary);
}

.dark .btn-text {
  color: var(--dark-text-secondary);
}

.dark .btn-text:hover {
  background: var(--dark-bg-hover);
  color: var(--dark-text-primary);
}

.dark .sidebar {
  background: var(--dark-bg-primary);
  border-color: var(--dark-border);
}

.dark .sidebar-item {
  color: var(--dark-text-secondary);
}

.dark .sidebar-item:hover {
  background: var(--dark-bg-hover);
  color: var(--dark-text-primary);
}

.dark .sidebar-item.router-link-active {
  background: var(--primary-500);
  color: #fff;
}

.dark .content-area {
  background: var(--dark-bg-secondary);
}

/* 响应式 */
@media (max-width: 768px) {
  .sidebar {
    width: 140px;
  }
  
  .header {
    padding: 0 16px;
  }
  
  .title {
    font-size: 16px;
  }
  
  .btn-text {
    padding: 6px 10px;
    font-size: 13px;
  }
}
</style>
