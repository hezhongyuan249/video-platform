<template>
  <header class="navbar">
    <div class="navbar-container">
      <!-- 左侧：标题 -->
      <div class="navbar-brand">
        <span class="navbar-title">视频平台</span>
      </div>

      <!-- 右侧：功能按钮 -->
      <div class="navbar-actions">
        <!-- 搜索 -->
        <div class="search-box">
          <input
            v-model="searchKeyword"
            placeholder="搜索视频..."
            class="search-input"
            @keyup.enter="searchVideo"
          />
          <button class="search-btn" @click="searchVideo">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="M21 21l-4.35-4.35"/>
            </svg>
          </button>
        </div>

        <!-- 主题切换 -->
        <button class="btn-text" @click="toggleTheme">
          {{ isDark ? '浅色' : '深色' }}
        </button>

        <!-- 我的关注 -->
        <button 
          v-if="user.status === 0"
          class="btn-text"
          @click="goMyFollowing"
        >
          我的关注
        </button>

        <!-- 我的主页 -->
        <button 
          v-if="user.status === 0"
          class="btn-text"
          @click="goMyProfile"
        >
          我的主页
        </button>

        <!-- 我的投稿 -->
        <button 
          v-if="user.status === 0"
          class="btn-text"
          @click="goMyVideos"
        >
          我的投稿
        </button>

        <!-- 观看历史 -->
        <button 
          v-if="user.status === 0"
          class="btn-text"
          @click="goWatchHistory"
        >
          观看历史
        </button>

        <!-- 我的收藏 -->
        <button 
          v-if="user.status === 0"
          class="btn-text"
          @click="goMyFavorites"
        >
          我的收藏
        </button>

        <!-- 后台管理 -->
        <button 
          v-if="user.role === 'admin' && user.status === 0"
          class="btn-text"
          @click="goPage('admin')"
        >
          后台管理
        </button>

        <!-- 上传视频 -->
        <button 
          v-if="user.status === 0"
          class="btn-primary-sm"
          @click="goUpload"
        >
          上传视频
        </button>

        <!-- 用户菜单 -->
        <div class="user-menu" v-if="user.id">
          <button class="btn-text user-btn">
            <img :src="user.avatar || defaultAvatar" class="nav-avatar" />
            {{ user.username || '用户' }}
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 9l6 6 6-6"/>
            </svg>
          </button>
          <div class="dropdown">
            <a @click="goPage('user-settings')">个人设置</a>
            <a @click="goPage('my-profile')">我的主页</a>
            <a @click="logout" class="danger">退出登录</a>
          </div>
        </div>
        <button v-else class="btn-text" @click="goLogin">登录</button>
      </div>
    </div>
  </header>
  <div class="navbar-spacer"></div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfo, clearUserInfo } from '../utils/userStorage'

const router = useRouter()

const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

const getUser = () => {
  const info = sessionStorage.getItem('userInfo')
  return info ? JSON.parse(info) : null
}

const user = ref(getUser() || {})
const isDark = ref(localStorage.getItem('theme') === 'dark')
const searchKeyword = ref('')

const toggleTheme = () => {
  isDark.value = !isDark.value
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  document.documentElement.classList.toggle('dark', isDark.value)
}

const goLogin = () => router.push('/login')
const goMyFollowing = () => router.push('/my-following')
const goMyProfile = () => router.push('/my-profile')
const goMyVideos = () => router.push('/my-videos')
const goWatchHistory = () => router.push('/watch-history')
const goMyFavorites = () => router.push('/my-favorites')
const goUpload = () => router.push('/upload-video')

const goPage = (page) => {
  if (page === 'admin') {
    router.push('/admin')
  } else if (page === 'user-settings') {
    router.push('/user-settings')
  } else if (page === 'my-profile') {
    router.push('/my-profile')
  }
}

const searchVideo = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/', query: { search: searchKeyword.value.trim() } })
  }
}

const logout = () => {
  if (!confirm('确定要退出登录吗？')) return
  clearUserInfo()
  sessionStorage.removeItem('userInfo')
  router.push('/login')
}
</script>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border);
}

.navbar-spacer {
  height: 56px;
}

.navbar-container {
  max-width: 1600px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 24px;
}

.navbar-brand {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.navbar-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.navbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-box {
  display: flex;
  align-items: center;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 0 12px;
}

.search-input {
  border: none;
  background: transparent;
  padding: 8px 0;
  font-size: 14px;
  color: var(--text-primary);
  width: 180px;
  outline: none;
}

.search-input::placeholder {
  color: var(--text-tertiary);
}

.search-btn {
  background: none;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 4px;
  display: flex;
}

.btn-text {
  padding: 8px 12px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn-text:hover {
  color: var(--text-primary);
  background: var(--bg-hover);
}

.btn-primary-sm {
  padding: 8px 16px;
  background: var(--primary-500);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

.btn-primary-sm:hover {
  opacity: 0.9;
}

.user-menu {
  position: relative;
}

.user-btn {
  gap: 6px;
}

.nav-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

.dropdown {
  display: none;
  position: absolute;
  top: 100%;
  right: 0;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 4px;
  min-width: 150px;
  padding: 8px 0;
  box-shadow: var(--shadow-md);
}

.user-menu:hover .dropdown {
  display: block;
}

.dropdown a {
  display: block;
  padding: 10px 16px;
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
}

.dropdown a:hover {
  background: var(--bg-hover);
}

.dropdown a.danger {
  color: var(--error-500);
}

@media (max-width: 768px) {
  .search-box {
    display: none;
  }
  
  .navbar-actions {
    gap: 4px;
  }
  
  .btn-text {
    padding: 8px;
  }
}
</style>
