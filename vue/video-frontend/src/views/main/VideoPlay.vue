<template>
  <div class="video-play-page" :class="{ dark: isDark }">
    <header class="header">
      <div class="header-left">
        <button class="btn-text" @click="$router.back()">← 返回</button>
      </div>
      <div class="header-right">
        <div class="user-menu" v-if="currentUser && currentUser.id">
          <button class="btn-text user-btn">
            <img :src="currentUser.avatar || defaultAvatar" class="user-avatar" />
            {{ currentUser.username }}
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 9l6 6 6-6"/>
            </svg>
          </button>
          <div class="dropdown-menu">
            <a @click="router.push('/my-profile')">我的主页</a>
            <a @click="router.push('/user-settings')">个人设置</a>
            <a @click="logout" class="danger">退出登录</a>
          </div>
        </div>
        <button v-else class="btn-text" @click="router.push('/login')">登录</button>
        <button class="btn-text" @click="toggleTheme">{{ isDark ? '浅色' : '深色' }}</button>
      </div>
    </header>

    <div class="content-wrapper" v-if="video && video.id">
      <div class="left-column">
        <div class="video-container">
          <div class="video-player-wrapper">
            <video 
              ref="videoRef"
              class="video-player"
              controls
              controlsList="nodownload nofullscreen"
              oncontextmenu="return false;"
              @loadstart="onVideoLoadStart"
              @play="onVideoPlay"
              @pause="onVideoPause"
              @ended="onVideoEnded"
            ></video>
            <div class="quality-switch" v-if="hasMultipleQuality">
              <select v-model="currentQuality" @change="switchQuality" class="quality-select">
                <option value="1080p" v-if="video.videoUrl1080p">1080P</option>
                <option value="720p" v-if="video.videoUrl720p">720P</option>
                <option value="480p" v-if="video.videoUrl480p">480P</option>
              </select>
            </div>
          </div>
        </div>
        <div class="video-info">
          <h1 class="video-title">{{ video.title }}</h1>
          <div class="video-meta">
            <span v-if="video.categoryName" class="video-category">{{ video.categoryName }}</span>
            <span>{{ formatViews(video.views) }}播放</span>
            <span>{{ formatDate(video.createTime) }}</span>
          </div>
          <div class="video-actions">
            <button 
              class="action-btn" 
              :class="{ active: videoStatus.liked }"
              @click="handleLike"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
              {{ videoStatus.liked ? '已赞' : '点赞' }} {{ video.likes || 0 }}
            </button>
            <button 
              class="action-btn"
              :class="{ active: videoStatus.favorited }"
              @click="handleFavorite"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
              </svg>
              {{ videoStatus.favorited ? '已收藏' : '收藏' }} {{ video.favorites || 0 }}
            </button>
          </div>
        </div>
        <div class="creator-section" v-if="video && video.id">
          <div class="creator-card">
            <img :src="video.userAvatar || defaultAvatar" class="creator-avatar" @click="goToCreator(video.userId)"/>
            <div class="creator-info">
              <div class="creator-name-row">
                <div class="creator-name">{{ video.username }}</div>
                <button class="btn-follow" :class="{ following: video.following }" v-if="currentUser && currentUser.id && video.userId != currentUser.id" @click="handleFollow">
                  {{ video.following ? '已关注' : '关注' }}
                </button>
              </div>
              <div class="creator-stats">
                <span>作品 {{ video.videosCount || 0 }}</span>
                <span>粉丝 {{ video.followersCount || 0 }}</span>
                <span>获赞 {{ video.likesCount || 0 }}</span>
                <span>收藏 {{ video.favoritesCount || 0 }}</span>
              </div>
              <div class="creator-desc">{{ video.userDescription || '暂无简介' }}</div>
            </div>
          </div>
        </div>

        <!-- 评论模块 -->
        <div class="comments-section">
          <h3>评论 {{ comments.length }}</h3>
          <div class="comment-input">
            <img :src="currentUser?.avatar || defaultAvatar" class="comment-avatar" />
            <div class="input-wrapper">
              <textarea v-model="newComment" placeholder="发表你的看法..." rows="2" @keydown.ctrl.enter="submitComment"></textarea>
              <button class="btn-submit" @click="submitComment" :disabled="!newComment.trim()">评论</button>
            </div>
          </div>
          <div class="comment-list">
            <div class="comment-item" v-for="comment in comments" :key="comment.id">
              <img :src="comment.avatar || defaultAvatar" class="comment-avatar" />
              <div class="comment-content">
                <div class="comment-header">
                  <span class="comment-username">{{ comment.username }}</span>
                  <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                </div>
                <div class="comment-text">{{ comment.content }}</div>
                <div class="comment-actions">
                  <span class="like-btn" :class="{ liked: comment.liked }" @click="likeComment(comment)">
                    <svg class="like-icon" viewBox="0 0 24 24" width="14" height="14">
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" fill="none" stroke="currentColor" stroke-width="2"/>
                    </svg>
                    {{ comment.likeCount || 0 }}
                  </span>
                  <span class="reply-btn" @click="showReplyInput(comment, null)">回复</span>
                  <span class="delete-btn" v-if="currentUser && currentUser.id === comment.userId" @click="deleteComment(comment.id)">删除</span>
                </div>
                <div class="reply-list" v-if="comment.replies && comment.replies.length > 0">
                  <div class="reply-item" v-for="reply in comment.replies" :key="reply.id">
                    <img :src="reply.avatar || defaultAvatar" class="reply-avatar" />
                    <div class="reply-content">
                      <span class="reply-username">{{ reply.username }}</span>
                      <span v-if="reply.replyToUsername" class="reply-to">@{{ reply.replyToUsername }}</span>
                      <span class="reply-text">: {{ reply.content }}</span>
                      <div class="reply-actions">
                        <span class="like-btn" :class="{ liked: reply.liked }" @click="likeComment(reply)">
                          <svg class="like-icon" viewBox="0 0 24 24" width="12" height="12">
                            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" fill="none" stroke="currentColor" stroke-width="2"/>
                          </svg>
                          {{ reply.likeCount || 0 }}
                        </span>
                        <span class="reply-btn" @click="showReplyInput(reply, comment)">回复</span>
                        <span class="delete-btn" v-if="currentUser && currentUser.id === reply.userId" @click="deleteComment(reply.id)">删除</span>
                      </div>
                      <div class="reply-input" v-if="replyingTo === reply.id">
                        <input v-model="replyContent" :placeholder="`回复 @${reply.username}`" @keydown.enter="submitReply(reply.id, reply.userId)" @keydown.ctrl.enter="submitReply(reply.id, reply.userId)" />
                        <button @click="submitReply(reply.id, reply.userId)">发送</button>
                        <button @click="cancelReply">取消</button>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="reply-input" v-if="replyingTo === comment.id">
                  <input v-model="replyContent" :placeholder="`回复 @${comment.username}`" @keydown.enter="submitReply(comment.id, comment.userId)" @keydown.ctrl.enter="submitReply(comment.id, comment.userId)" />
                  <button @click="submitReply(comment.id, comment.userId)">发送</button>
                  <button @click="cancelReply">取消</button>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="creator-videos" v-if="creatorVideos.length > 0">
          <h3>更多视频</h3>
          <div class="video-list">
            <div class="video-item" v-for="item in creatorVideos" :key="item.id" @click="goToVideo(item.id)">
              <div class="video-cover">
                <img :src="item.coverUrl" />
                <span class="duration">{{ formatDuration(item.duration) }}</span>
              </div>
              <div class="video-title">{{ item.title }}</div>
              <div class="video-views">{{ formatViews(item.views) }}播放</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { formatViews, formatDuration, formatDate } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const video = ref({})
const videoRef = ref(null)
const currentQuality = ref('1080p')

const hasMultipleQuality = computed(() => {
  return !!(video.value.videoUrl480p || video.value.videoUrl720p || video.value.videoUrl1080p)
})

const currentVideoUrl = computed(() => {
  if (!video.value) return ''
  if (currentQuality.value === '1080p' && video.value.videoUrl1080p) {
    return video.value.videoUrl1080p
  } else if (currentQuality.value === '720p' && video.value.videoUrl720p) {
    return video.value.videoUrl720p
  } else if (currentQuality.value === '480p' && video.value.videoUrl480p) {
    return video.value.videoUrl480p
  }
  return video.value.videoUrl || video.value.videoUrl1080p || video.value.videoUrl720p || video.value.videoUrl480p
})

const onVideoLoadStart = () => {
  loadAndSetProgress()
}

const switchQuality = () => {
  if (videoRef.value) {
    const currentTime = videoRef.value.currentTime
    
    // 保存当前播放状态
    const wasPlaying = !videoRef.value.paused
    
    // 清除之前的事件监听器
    videoRef.value.onloadedmetadata = null
    videoRef.value.oncanplay = null
    
    // 设置新的视频源
    videoRef.value.src = currentVideoUrl.value
    
    // 监听视频加载完成后设置时间
    videoRef.value.onloadedmetadata = function() {
      if (currentTime > 0) {
        videoRef.value.currentTime = currentTime
        if (wasPlaying) {
          // 只在之前正在播放时才播放
          videoRef.value.play().catch(error => {
            // 播放失败时不输出日志
          })
        }
      }
    }
  }
}
const isDark = ref(false)

// 进度保存定时器
let progressTimer = null

// 保存播放进度
const saveProgress = () => {
  if (!videoRef.value || !currentUser.id || !video.value.id) return
  const currentTime = videoRef.value.currentTime
  if (currentTime > 0) {
    request.post('/video/history/progress', {
      userId: currentUser.id,
      videoId: video.value.id,
      progress: Math.floor(currentTime)
    })
  }
}

// 启动进度保存定时器
const startTimer = () => {
  stopTimer()
  progressTimer = setInterval(saveProgress, 5000)
}

// 停止进度保存定时器
const stopTimer = () => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

// 加载并设置播放进度
const loadAndSetProgress = async () => {
  if (!currentUser.id || !video.value.id || !videoRef.value) return
  try {
    const res = await request.get('/video/history/progress', {
      params: {
        userId: currentUser.id,
        videoId: video.value.id
      }
    })
    if (res.code === 200 && res.data !== undefined && videoRef.value) {
      videoRef.value.currentTime = res.data
    }
  } catch (e) {
    // 加载进度失败时不输出日志
  }
}

// 监听视频播放事件
const onVideoPlay = () => {
  startTimer()
}

// 监听视频暂停事件
const onVideoPause = () => {
  stopTimer()
  saveProgress() // 暂停时立即保存进度
}

// 监听视频结束事件
const onVideoEnded = () => {
  stopTimer()
  // 视频结束时清除进度
  if (currentUser.id && video.value.id) {
    request.post('/video/history/progress', {
      userId: currentUser.id,
      videoId: video.value.id,
      progress: 0
    })
  }
}

const currentUser = JSON.parse(sessionStorage.getItem('userInfo') || '{}')

const logout = () => {
  if (!confirm('确定要退出登录吗？')) return
  sessionStorage.removeItem('userInfo')
  router.push('/login')
}
const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI1MCIgZmlsbD0iI2UzZTNlMyIvPjx0ZXh0IHg9IjUwIiB5PSI1NSIgZm9udC1zaXplPSIyMCIgZmlsbD0iIzY2NiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPjwvdGV4dD48L3N2Zz4='

const videoStatus = ref({
  liked: false,
  favorited: false
})

const creatorVideos = ref([])

const comments = ref([])
const newComment = ref('')
const replyingTo = ref(null)
const replyToParentId = ref(null)
const replyContent = ref('')

const getComments = async () => {
  if (!video.value.id) return
  try {
    const res = await fetch(`/api/comment/list?videoId=${video.value.id}&currentUserId=${currentUser.id || ''}`, { credentials: 'include' })
    const result = await res.json()
    if (result.code === 200) {
      comments.value = result.data?.records || []
    }
  } catch (e) {
    // 获取评论失败时不输出日志
  }
}

const submitComment = async () => {
  if (!newComment.value.trim() || !currentUser.id) return
  try {
    const res = await fetch('/api/comment/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        videoId: video.value.id,
        userId: currentUser.id,
        content: newComment.value.trim()
      }),
      credentials: 'include'
    })
    const result = await res.json()
    if (result.code === 200) {
      newComment.value = ''
      getComments()
    }
  } catch (e) {
    // 评论失败时不输出日志
  }
}

const submitReply = async (parentId, replyToUserId) => {
  if (!replyContent.value.trim() || !currentUser.id) return
  const actualParentId = replyToParentId.value || parentId
  try {
    const res = await fetch('/api/comment/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        videoId: video.value.id,
        userId: currentUser.id,
        content: replyContent.value.trim(),
        parentId: actualParentId,
        replyToUserId: replyToUserId
      }),
      credentials: 'include'
    })
    const result = await res.json()
    if (result.code === 200) {
      replyContent.value = ''
      replyingTo.value = null
      replyToParentId.value = null
      getComments()
    }
  } catch (e) {
    // 回复失败时不输出日志
  }
}

const showReplyInput = (comment, parentComment = null) => {
  replyingTo.value = comment.id
  replyContent.value = ''
  if (parentComment) {
    replyToParentId.value = parentComment.id
  } else {
    replyToParentId.value = comment.id
  }
}

const cancelReply = () => {
  replyingTo.value = null
  replyContent.value = ''
  replyToParentId.value = null
}

const deleteComment = async (id) => {
  if (!confirm('确定删除这条评论？')) return
  try {
    const res = await fetch(`/api/comment/delete?id=${id}&userId=${currentUser.id}`, {
      method: 'POST',
      credentials: 'include'
    })
    const result = await res.json()
    if (result.code === 200) {
      getComments()
    }
  } catch (e) {
    // 删除失败时不输出日志
  }
}

const likeComment = async (comment) => {
  if (!currentUser.id) return
  try {
    const res = await fetch(`/api/comment/like?commentId=${comment.id}&userId=${currentUser.id}`, {
      method: 'POST',
      credentials: 'include'
    })
    const result = await res.json()
    if (result.code === 200) {
      comment.likeCount = result.data
      comment.liked = !comment.liked
    }
  } catch (e) {
    // 点赞失败时不输出日志
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return date.toLocaleDateString()
}

const toggleTheme = () => {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

const getDetail = async function() {
  const userId = currentUser ? currentUser.id : null
  try {
    const res = await request.get('/video/getById', {
      params: { id: route.params.id, userId: userId }
    })
    if (res.code === 200) {
        video.value = res.data
        checkVideoStatus()
        getComments()
        
        // 等待DOM更新后设置视频源
        nextTick(() => {
          if (videoRef.value) {
            videoRef.value.src = currentVideoUrl.value
            // 加载并设置播放进度
            loadAndSetProgress()
          }
        })
        
        // 保存观看历史
      if (userId) {
        try {
          await request.post('/video/history/add', {
            userId: userId,
            videoId: route.params.id
          })
        } catch (e) {
          // 保存观看历史失败时不输出日志
        }
      }
    } else {
      throw new Error(res.msg)
    }
  } catch (e) {
    ElMessage.error('获取视频详情失败：' + e.message)
  }
}

const checkVideoStatus = async function() {
  if (!currentUser || !currentUser.id) return
  try {
    const res = await request.get('/video/checkStatus/' + video.value.id + '/' + currentUser.id)
    if (res.code === 200) {
      videoStatus.value = res.data
    }
  } catch (e) {
    // 检查状态失败时不输出日志
  }
}

const getCreatorVideos = async function() {
  if (!video.value.userId) return
  try {
    const res = await request.get('/video/myList', {
      params: { userId: video.value.userId }
    })
    if (res.code === 200) {
      creatorVideos.value = (res.data || []).filter(v => v.id !== video.value.id).slice(0, 5)
    }
  } catch (e) {
    // 获取作者视频失败时不输出日志
  }
}

const handleLike = async function() {
  if (!currentUser || !currentUser.id) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await request.post('/video/like', { videoId: video.value.id, userId: currentUser.id })
    if (res.code === 200) {
      videoStatus.value.liked = !videoStatus.value.liked
      video.value.likes = (video.value.likes || 0) + (videoStatus.value.liked ? 1 : -1)
      ElMessage.success(res.data)
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleFavorite = async function() {
  if (!currentUser || !currentUser.id) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await request.post('/video/favorite', { videoId: video.value.id, userId: currentUser.id })
    if (res.code === 200) {
      videoStatus.value.favorited = !videoStatus.value.favorited
      video.value.favorites = (video.value.favorites || 0) + (videoStatus.value.favorited ? 1 : -1)
      ElMessage.success(res.data)
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleFollow = async function() {
  if (!currentUser || !currentUser.id) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await request.post('/video/follow', { followingId: video.value.userId, followerId: currentUser.id })
    if (res.code === 200) {
      video.value.following = !video.value.following
      ElMessage.success(res.data)
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const goToCreator = (userId) => {
  router.push('/user/' + userId)
}

const goToVideo = (id) => {
  router.push('/video-play/' + id)
}

onMounted(function() {
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'dark') {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }
  getDetail()
  getCreatorVideos()
})
</script>

<style scoped>
.video-play-page {
  min-height: 100vh;
  background: var(--bg-primary);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-menu {
  position: relative;
}

.user-menu:hover .dropdown-menu {
  display: block;
}

.dropdown-menu {
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
  z-index: 100;
}

.dropdown-menu a {
  display: block;
  padding: 10px 16px;
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
}

.dropdown-menu a:hover {
  background: var(--bg-secondary);
}

.dropdown-menu a.danger {
  color: #f56c6c;
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

.btn-text {
  padding: 8px 12px;
  background: none;
  border: none;
  color: var(--text-primary);
  cursor: pointer;
  font-size: 14px;
}

.content-wrapper {
  display: flex;
  flex-wrap: wrap;
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  gap: 24px;
  align-items: flex-start;
}

.left-column {
  flex: 1;
  min-width: 600px;
}

.creator-section {
}

.video-container {
  background: #000;
  border-radius: 8px;
  position: relative;
}

.video-player-wrapper {
  position: relative;
  width: 100%;
}

.video-player {
  width: 100%;
  display: block;
}

.video-player::-webkit-media-controls {
  enable-media-time-rail: true;
}

.video-player::-webkit-media-controls-enclosure {
  user-select: none;
}

.quality-switch {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 10;
}

.quality-select {
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 12px;
  cursor: pointer;
  outline: none;
}

.quality-select:hover {
  background: rgba(0, 0, 0, 0.9);
}

.video-info {
  padding: 20px 0;
}

.video-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px;
}

.video-meta {
  display: flex;
  gap: 16px;
  color: var(--text-tertiary);
  font-size: 14px;
  margin-bottom: 16px;
  align-items: center;
}

.video-category {
  background: var(--primary-color, #ff4757);
  color: #fff;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.video-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 20px;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 14px;
}

.action-btn:hover {
  background: var(--bg-hover);
}

.action-btn.active {
  color: var(--primary-500);
  border-color: var(--primary-500);
}

.creator-section {
}

.creator-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: 8px;
  margin-bottom: 20px;
}

.btn-follow {
  flex-shrink: 0;
  min-width: 70px;
  padding: 6px 16px;
  background: var(--primary-500);
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-follow.following {
  background: var(--bg-primary);
  color: var(--text-secondary);
  border: 1px solid var(--border);
}

.creator-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  cursor: pointer;
}

.creator-info {
  flex: 1;
  min-width: 0;
}

.creator-name {
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.creator-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.creator-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--text-secondary);
  margin: 4px 0;
}

.creator-stats span {
  white-space: nowrap;
}

.creator-desc {
  font-size: 12px;
  color: var(--text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comments-section {
  width: 100%;
  margin-bottom: 24px;
}

.comments-section h3 {
  font-size: 16px;
  color: var(--text-primary);
  margin: 0 0 16px;
}

.comment-input {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.comment-input .comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
}

.input-wrapper {
  flex: 1;
}

.input-wrapper textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  resize: none;
  font-size: 14px;
}

.input-wrapper textarea:focus {
  outline: none;
  border-color: var(--primary-500);
}

.btn-submit {
  margin-top: 8px;
  padding: 6px 16px;
  background: var(--primary-500);
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-username {
  font-weight: 500;
  color: var(--text-primary);
  font-size: 14px;
}

.comment-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.comment-text {
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 8px;
}

.comment-actions {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.like-btn, .reply-btn, .delete-btn {
  cursor: pointer;
}

.like-btn:hover, .reply-btn:hover {
  color: var(--primary-500);
}

.like-btn.liked {
  color: var(--primary-500);
}

.like-icon {
  vertical-align: middle;
  margin-right: 2px;
  margin-top: -1px;
}

.like-btn.liked .like-icon path {
  fill: var(--primary-500);
}

.delete-btn:hover {
  color: var(--error-500);
}

.reply-list {
  margin-top: 12px;
  padding-left: 12px;
  border-left: 2px solid var(--border);
}

.reply-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.reply-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  flex-shrink: 0;
}

.reply-content {
  font-size: 13px;
}

.reply-username {
  font-weight: 500;
  color: var(--text-primary);
}

.reply-to {
  color: var(--text-tertiary);
}

.reply-text {
  color: var(--text-primary);
}

.reply-actions {
  margin-top: 4px;
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.reply-input {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.reply-input input {
  flex: 1;
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 13px;
}

.reply-input input:focus {
  outline: none;
  border-color: var(--primary-500);
}

.reply-input button {
  padding: 4px 12px;
  background: var(--primary-500);
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}

.reply-input button:last-child {
  background: var(--bg-secondary);
  color: var(--text-secondary);
}

.creator-videos h3 {
  font-size: 16px;
  color: var(--text-primary);
  margin: 0 0 16px;
}

.video-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.video-item {
  cursor: pointer;
}

.video-item .video-cover {
  position: relative;
  aspect-ratio: 16/9;
  border-radius: 8px;
  overflow: hidden;
  background: var(--bg-tertiary);
}

.video-item .video-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-item .duration {
  position: absolute;
  bottom: 6px;
  right: 6px;
  background: rgba(0,0,0,0.75);
  color: #fff;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
}

.video-item .video-title {
  font-size: 14px;
  margin: 8px 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.video-item .video-views {
  font-size: 12px;
  color: var(--text-tertiary);
}

@media (max-width: 768px) {
  .content-wrapper {
    flex-direction: column;
  }
  .creator-section {
    width: 100%;
  }
}
</style>
