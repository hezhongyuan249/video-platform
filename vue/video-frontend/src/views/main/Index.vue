<template>
  <div class="index-page">
    <!-- 顶部导航栏 -->
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
            v-if="user.status === 0 && user.delFlag === 0"
            class="btn-text"
            @click="goMyFollowing"
          >
            我的关注
          </button>

          <!-- 我的主页 -->
          <button 
            v-if="user.status === 0 && user.delFlag === 0"
            class="btn-text"
            @click="goMyProfile"
          >
            我的主页
          </button>

          <!-- 我的投稿 -->
          <button 
            v-if="user.status === 0 && user.delFlag === 0"
            class="btn-text"
            @click="goMyVideos"
          >
            我的投稿
          </button>

          <!-- 观看历史 -->
          <button 
            v-if="user.status === 0 && user.delFlag === 0"
            class="btn-text"
            @click="goWatchHistory"
          >
            观看历史
          </button>

          <!-- 我的收藏 -->
          <button 
            v-if="user.status === 0 && user.delFlag === 0"
            class="btn-text"
            @click="goMyFavorites"
          >
            我的收藏
          </button>

          <!-- 后台管理 -->
          <button 
            v-if="user.role === 'admin' && user.status === 0 && user.delFlag === 0"
            class="btn-text"
            @click="goPage('admin')"
          >
            后台管理
          </button>

          <!-- 上传视频 -->
          <button 
            v-if="user.status === 0 && user.delFlag === 0"
            class="btn-primary-sm"
            @click="router.push('/upload-video')"
          >
            上传视频
          </button>

          <!-- 收件箱 -->
          <div class="inbox-btn" @click="router.push('/notifications')">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
              <polyline points="22,6 12,13 2,6"/>
            </svg>
            <span class="badge" v-if="unreadCount > 0">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </div>

          <!-- 用户菜单 -->
          <div class="user-menu">
            <button class="btn-text user-btn">
              <img :src="user.avatar || defaultAvatar" class="nav-avatar" />
              {{ user.username || '用户' }}
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M6 9l6 6 6-6"/>
              </svg>
            </button>
            <div class="dropdown-menu">
              <a @click="router.push('/user-settings')">个人设置</a>
              <a v-if="user.delFlag === 0 && user.status === 0" @click="cancelAccount" class="danger">注销账号</a>
              <a @click="logout" class="danger">退出登录</a>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- 内容区域 -->
    <div class="main-content">
      <!-- 账号封禁提示 -->
      <div class="notice baned" v-if="user.status === 1">
        <h3>您的账号已被管理员封禁</h3>
        <p>无法使用平台核心功能，允许操作：登录、退出、修改个人信息</p>
      </div>

      <!-- 账号注销冷静期提示 -->
      <div class="notice delete" v-else-if="user.delFlag === 1 && user.deleteType === 0">
        <h3>账号已进入注销冷静期</h3>
        <p>剩余 {{ getRemainingTime(user.deleteTime) }} 后，账号将被永久清除</p>
      </div>

      <!-- 正常内容区 -->
      <div class="content normal" v-else>
        <!-- 分类标签 -->
        <div class="category-bar">
          <button
            v-for="category in categories"
            :key="category.id"
            class="category-tag"
            :class="{ active: selectedCategory === category.id }"
            @click="selectCategory(category.id)"
          >
            {{ category.name }}
          </button>
        </div>

        <!-- 视频列表 -->
        <div class="video-section">
          <div v-if="loading" class="loading">
            <span>加载中...</span>
          </div>
          <div v-else class="video-grid">
            <VideoCard
              v-for="item in filteredVideoList"
              :key="item.id"
              :video="item"
              @click="goPlay"
            />
          </div>

          <div v-if="!loading && filteredVideoList.length === 0" class="empty">
            <p>暂无视频内容</p>
            <button class="btn-primary-sm" @click="router.push('/upload-video')">上传视频</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { useTheme } from '../../composables/useTheme'
import { useCategories } from '../../composables/useCategories'
import { getUserInfo, clearUserInfo } from '../../utils/userStorage'
import VideoCard from '../../components/VideoCard.vue'
import { formatViews, formatDuration } from '../../utils/format'

const router = useRouter()
const user = ref(getUserInfo() || {})
const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI1MCIgZmlsbD0iI2UzZTNlMyIvPjx0ZXh0IHg9IjUwIiB5PSI1NSIgZm9udC1zaXplPSIyMCIgZmlsbD0iIzY2NiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPjwvdGV4dD48L3N2Zz4='
const unreadCount = ref(0)
let timer = null

const loadUnreadCount = async () => {
  if (!user.value.id) {
    unreadCount.value = 0
    return
  }
  try {
    const res = await fetch(`/api/notification/unreadCount?userId=${user.value.id}`, { credentials: 'include' })
    const result = await res.json()
    if (result.code === 200) {
      unreadCount.value = result.data || 0
    }
  } catch (e) {
    // 获取未读消息数失败时不输出日志
  }
}

const { isDark, toggleTheme } = useTheme()
const { categories, videoTypes, loadCategories, saveCategories, syncVideoTypes } = useCategories()

const searchKeyword = ref('')
const filteredVideoList = ref([])

const searchVideo = () => {
  if (!searchKeyword.value.trim()) {
    filteredVideoList.value = videoList.value
    return
  }
  
  filteredVideoList.value = videoList.value.filter(video => 
    video.title.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
    video.intro.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
    video.username.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
}

watch(searchKeyword, () => {
  searchVideo()
})

const selectedCategory = ref('all')

const selectCategory = (categoryId) => {
  selectedCategory.value = categoryId
  
  if (categoryId === 'all' || categoryId === undefined) {
    filteredVideoList.value = videoList.value
  } else {
    // 转换为字符串比较，因为 videoType 是字符串类型
    const categoryIdStr = String(categoryId)
    filteredVideoList.value = videoList.value.filter(video => {
      return video.videoType === categoryIdStr
    })
  }
}

const goPage = (url) => {
  if (user.value.delFlag === 1) {
    ElMessage.warning('账号处于注销冷静期，无法使用该功能');
    return;
  }
  router.push(`/${url}`)
}

const cancelAccount = async () => {
  if (!confirm('确定注销账号吗？7天后数据将永久清除，无法恢复！')) return
  
  try {
    
    const res = await fetch('/api/user/cancel', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id: user.value.id }),
      credentials: 'include'
    })
    const result = await res.json()
    
    if(result.code === 200){
      ElMessage.success('注销申请已提交！')
      sessionStorage.removeItem('userInfo')
      user.value = {}
      router.push('/login')
    }else{
      ElMessage.error(result.msg || '注销失败')
    }
  } catch (e) {
    ElMessage.info('已取消注销')
  }
}

const goMyProfile = () => {
  router.push('/my-profile')
}

const goMyFollowing = () => {
  router.push('/my-following')
}

const goMyVideos = () => {
  router.push('/my-videos')
}

const goMyFavorites = () => {
  router.push('/my-favorites')
}

const goWatchHistory = () => {
  router.push('/watch-history')
}

const logout = () => {
  if (!confirm('确定要退出登录吗？')) return
  clearUserInfo()
  user.value = {}
  clearInterval(timer)
  router.push('/login')
  ElMessage.success('退出登录成功')
}

const checkUserStatus = async () => {
  if (!user.value.id) {
    clearInterval(timer)
    return
  }

  try {
    const res = await fetch('/api/user/info?id=' + user.value.id, {
      credentials: 'include'
    })
    const result = await res.json()
    
    if (result.code === 200) {
      Object.assign(user.value, result.data)
      sessionStorage.setItem('userInfo', JSON.stringify(result.data))
    }

  } catch (e) {
    // 状态检测失败时不输出日志
  }
}

const getRemainingTime = (deleteTime) => {
  if (!deleteTime) return '已过期'
  const delTime = new Date(deleteTime).getTime()
  const expireTime = delTime + 7 * 24 * 60 * 60 * 1000
  const leftTime = expireTime - Date.now()
  if (leftTime <= 0) return '已到期'
  const d = Math.floor(leftTime / (24 * 3600000))
  const h = Math.floor((leftTime % (24 * 3600000)) / 3600000)
  const m = Math.floor((leftTime % 3600000) / 60000)
  return `${d}天${h}小时${m}分钟`
}

const videoList = ref([])
const loading = ref(false)

const getVideoList = async () => {
  loading.value = true
  try {
    const res = await fetch('/api/video/list', {
      credentials: 'include'
    })
    const result = await res.json()
    
    videoList.value = result.data || []
    filteredVideoList.value = result.data || []
    
    // 打印视频数据结构
    console.log('视频列表数据:', result.data)
  } catch (e) {
    ElMessage.error('获取视频列表失败：' + e.message)
  } finally {
    loading.value = false
  }
}

const goPlay = (id) => {
  router.push(`/video-play/${id}`)
}

onMounted(() => {
  checkUserStatus()
  timer = setInterval(checkUserStatus, 3000)
  loadUnreadCount()
  
  if (user.value.delFlag === 1 && user.deleteType === 0) {
    timer = setInterval(() => { getRemainingTime(user.value.deleteTime) }, 60000)
  }
  
  getVideoList()
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style scoped>
/* 基础样式 */
.index-page {
  min-height: 100vh;
  background: var(--bg-primary);
  color: var(--text-primary);
}

/* 导航栏 */
.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border);
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
  display: flex;
  align-items: center;
}

.navbar-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.navbar-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 搜索框 */
.search-box {
  display: flex;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 4px;
  overflow: hidden;
}

.search-input {
  width: 200px;
  padding: 8px 12px;
  border: none;
  outline: none;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 14px;
}

.search-input::placeholder {
  color: var(--text-tertiary);
}

.search-btn {
  padding: 8px 12px;
  border: none;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  cursor: pointer;
}

.search-btn:hover {
  background: var(--bg-hover);
}

/* 按钮 */
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

.btn-primary-sm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary-sm {
  padding: 8px 16px;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text-primary);
  font-size: 14px;
  border-radius: 4px;
  cursor: pointer;
}

.btn-secondary-sm:hover {
  background: var(--bg-hover);
}

/* 用户菜单 */
.user-menu {
  position: relative;
}

.user-menu:hover .dropdown-menu {
  display: block;
}

.dropdown {
  position: relative;
  display: inline-block;
}

.dropdown-trigger {
  cursor: pointer;
}

.dropdown-menu {
  display: none;
  position: absolute;
  top: 100%;
  right: 0;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 4px;
  min-width: 120px;
  padding: 8px 0;
  box-shadow: var(--shadow-md);
  z-index: 100;
}

.dropdown:hover .dropdown-menu {
  display: block;
}

.dropdown-item {
  display: block;
  padding: 10px 16px;
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
}

.dropdown-item:hover {
  background: var(--bg-hover);
}

.dropdown-menu a {
  display: block;
  padding: 10px 16px;
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
}

.dropdown-menu a:hover {
  background: var(--bg-hover);
}

.dropdown-menu a.danger {
  color: var(--error-500);
}

/* 主内容区 */
.main-content {
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px;
}

/* 提示 */
.notice {
  padding: 24px;
  border: 1px solid var(--border);
  border-radius: 8px;
  margin-bottom: 24px;
}

.notice.baned {
  border-color: var(--error-500);
  background: var(--bg-secondary);
}

.notice.delete {
  border-color: var(--warning-500);
  background: var(--bg-secondary);
}

.notice h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
}

.notice p {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 分类标签 */
.category-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.category-tag {
  padding: 8px 16px;
  border: 1px solid var(--border);
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 14px;
  border-radius: 4px;
  cursor: pointer;
}

.category-tag:hover {
  background: var(--bg-hover);
}

.category-tag.active {
  background: var(--text-primary);
  color: var(--bg-primary);
  border-color: var(--text-primary);
}

/* 视频列表 */
.video-section {
  width: 100%;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
}

.video-item {
  cursor: pointer;
}

.video-cover {
  position: relative;
  aspect-ratio: 16/9;
  background: var(--bg-tertiary);
  border-radius: 8px;
  overflow: hidden;
}

.video-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-cover .duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0,0,0,0.75);
  color: #fff;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
}

.video-meta {
  padding: 12px 0;
}

.video-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.video-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-tertiary);
}

/* 加载和空状态 */
.loading {
  padding: 60px;
  text-align: center;
  color: var(--text-tertiary);
}

.empty {
  padding: 60px;
  text-align: center;
}

.empty p {
  margin-bottom: 16px;
  color: var(--text-tertiary);
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: var(--bg-primary);
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
}

.modal-header h3 {
  font-size: 16px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: var(--text-tertiary);
}

.modal-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
}

.form-group .input,
.form-group .textarea,
.form-group .select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 14px;
}

.form-group .input:focus,
.form-group .textarea:focus,
.form-group .select:focus {
  outline: none;
  border-color: var(--text-primary);
}

.form-group .textarea {
  resize: vertical;
}

.form-group .file-input {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background: var(--bg-primary);
  font-size: 14px;
}

.form-group .hint {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid var(--border);
}

/* 深色模式 */
.dark .index-page {
  background: var(--dark-bg-primary);
  color: var(--dark-text-primary);
}

.dark .navbar {
  background: var(--dark-bg-primary);
  border-color: var(--dark-border);
}

.dark .navbar-title {
  color: var(--dark-text-primary);
}

.dark .search-input {
  background: var(--dark-bg-primary);
  color: var(--dark-text-primary);
}

.dark .search-btn {
  background: var(--dark-bg-tertiary);
}

.dark .btn-text {
  color: var(--dark-text-secondary);
}

.dark .btn-text:hover {
  background: var(--dark-bg-hover);
  color: var(--dark-text-primary);
}

.dark .dropdown-menu {
  background: var(--dark-bg-primary);
  border-color: var(--dark-border);
}

.dark .dropdown-menu a {
  color: var(--dark-text-primary);
}

.dark .dropdown-menu a:hover {
  background: var(--dark-bg-hover);
}

.dark .notice {
  background: var(--dark-bg-secondary);
}

.dark .notice h3,
.dark .notice p {
  color: var(--dark-text-primary);
}

.dark .category-tag {
  background: var(--dark-bg-primary);
  border-color: var(--dark-border);
  color: var(--dark-text-secondary);
}

.dark .category-tag:hover {
  background: var(--dark-bg-hover);
}

.dark .modal {
  background: var(--dark-bg-primary);
}

.dark .modal-header,
.dark .modal-footer {
  border-color: var(--dark-border);
}

.dark .modal-header h3,
.dark .close-btn {
  color: var(--dark-text-primary);
}

.dark .form-group .input,
.dark .form-group .textarea,
.dark .form-group .select,
.dark .form-group .file-input {
  background: var(--dark-bg-primary);
  border-color: var(--dark-border);
  color: var(--dark-text-primary);
}

/* 响应式 */
@media (max-width: 768px) {
  .navbar-container {
    padding: 0 16px;
    flex-wrap: wrap;
    height: auto;
    padding: 12px 16px;
    gap: 12px;
  }

  .search-box {
    order: 3;
    width: 100%;
  }

  .search-input {
    width: 100%;
  }

  .main-content {
    padding: 16px;
  }

  .video-grid {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 16px;
  }

  .category-bar {
    gap: 6px;
  }

  .category-tag {
    padding: 6px 12px;
    font-size: 13px;
  }

  .modal {
    width: 95%;
    margin: 16px;
  }
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

.inbox-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  color: var(--text-primary);
  transition: background 0.2s;
}

.inbox-btn:hover {
  background: var(--bg-hover);
}

.inbox-btn .badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
  border-radius: 8px;
}

.danger-btn {
  background-color: #f56c6c !important;
  border-color: #f56c6c !important;
}
</style>
