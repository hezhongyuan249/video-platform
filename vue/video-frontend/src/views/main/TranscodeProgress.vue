<template>
  <div class="transcode-page" :class="{ dark: isDark }">
    <div class="header-bar">
      <button class="back-btn" @click="goBack">← 返回</button>
      <h2>视频转码进度</h2>
      <div class="header-actions">
        <button class="theme-toggle" @click="toggleTheme">
          {{ isDark ? '浅色' : '深色' }}
        </button>
      </div>
    </div>
    
    <div class="transcode-container" v-if="loading">
      <div class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
    </div>
    
    <div class="transcode-container" v-else-if="!progressData">
      <div class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        </div>
        <p>未找到转码任务</p>
        <button class="btn-primary" @click="goBack">返回上传页面</button>
      </div>
    </div>
    
    <div class="transcode-container" v-else>
      <div class="video-info">
        <h3>视频ID: {{ videoId }}</h3>
        <p class="status-text" :class="progressData.status">
          状态: {{ getStatusText(progressData.status) }}
        </p>
      </div>
      
      <div class="progress-list">
        <div class="progress-item" :class="{ completed: progressData.progress480p >= 100 }">
          <div class="progress-header">
            <span class="resolution">480P</span>
            <span class="task-status">{{ progressData.task480p || '等待中' }}</span>
            <span class="progress-value">{{ progressData.progress480p || 0 }}%</span>
          </div>
          <div class="progress-bar">
            <div class="progress-fill p480" :style="{ width: (progressData.progress480p || 0) + '%' }"></div>
          </div>
        </div>
        
        <div class="progress-item" :class="{ completed: progressData.progress720p >= 100 }">
          <div class="progress-header">
            <span class="resolution">720P</span>
            <span class="task-status">{{ progressData.task720p || '等待中' }}</span>
            <span class="progress-value">{{ progressData.progress720p || 0 }}%</span>
          </div>
          <div class="progress-bar">
            <div class="progress-fill p720" :style="{ width: (progressData.progress720p || 0) + '%' }"></div>
          </div>
        </div>
        
        <div class="progress-item" :class="{ completed: progressData.progress1080p >= 100 }">
          <div class="progress-header">
            <span class="resolution">1080P</span>
            <span class="task-status">{{ progressData.task1080p || '等待中' }}</span>
            <span class="progress-value">{{ progressData.progress1080p || 0 }}%</span>
          </div>
          <div class="progress-bar">
            <div class="progress-fill p1080" :style="{ width: (progressData.progress1080p || 0) + '%' }"></div>
          </div>
        </div>
      </div>
      
      <div class="overall-progress" v-if="progressData.status !== 'completed'">
        <div class="overall-header">
          <span>总体进度</span>
          <span>{{ progressData.progress || 0 }}%</span>
        </div>
        <div class="progress-bar">
          <div class="progress-fill overall" :style="{ width: (progressData.progress || 0) + '%' }"></div>
        </div>
      </div>
      
      <div class="countdown-tip" v-if="countdown > 0">
        <span class="countdown-number">{{ countdown }}</span>
        <span>秒后自动跳转到我的投稿...</span>
      </div>
      
      <div class="action-buttons">
        <button v-if="progressData.status === 'completed'" class="btn-primary" @click="goToMyVideos">
          查看我的视频
        </button>
        <button class="btn-secondary" @click="goBack">
          {{ progressData.status === 'completed' ? '返回' : '取消' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import request from '../../utils/request';

const route = useRoute();
const router = useRouter();

const videoId = ref(route.params.videoId);
const loading = ref(true);
const progressData = ref(null);
const isDark = ref(false);
let pollTimer = null;

const toggleTheme = () => {
  isDark.value = !isDark.value;
  document.documentElement.classList.toggle('dark', isDark.value);
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light');
};

const getStatusText = (status) => {
  const statusMap = {
    'pending': '等待中',
    'transcoding': '转码中',
    'completed': '已完成'
  };
  return statusMap[status] || '未知';
};

const countdown = ref(0)
let countdownTimer = null
let hasCompleted = false

const fetchProgress = async () => {
  try {
    const res = await fetch(`/api/transcode/progress/${videoId.value}`, { 
      credentials: 'include' 
    });
    const data = await res.json();
    if (data.code === 200 && data.data) {
      progressData.value = data.data;
      // 检查进度是否100%
      if ((progressData.value.progress480p >= 100 || progressData.value.progress480p === undefined) &&
          (progressData.value.progress720p >= 100 || progressData.value.progress720p === undefined) &&
          (progressData.value.progress1080p >= 100 || progressData.value.progress1080p === undefined)) {
        if (!hasCompleted && countdown.value === 0) {
          hasCompleted = true
          clearInterval(pollTimer)
          ElMessage.success('转码完成！')
          countdown.value = 5
          countdownTimer = setInterval(() => {
            countdown.value--
            if (countdown.value <= 0) {
              clearInterval(countdownTimer)
              router.push('/my-videos')
            }
          }, 1000)
        }
      }
    } else if (data.code === 200 && !data.data) {
      // 后端返回null说明转码已完成（进度对象被删除）
      // 直接认为转码完成
      if (!hasCompleted && countdown.value === 0) {
        hasCompleted = true
        clearInterval(pollTimer)
        ElMessage.success('转码完成！')
        countdown.value = 5
        countdownTimer = setInterval(() => {
          countdown.value--
          if (countdown.value <= 0) {
            clearInterval(countdownTimer)
            router.push('/my-videos')
          }
        }, 1000)
      }
    }
  } catch (e) {
    console.error('获取转码进度失败', e);
  } finally {
    loading.value = false;
  }
};

const goBack = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
  }
  router.push('/upload-video');
};

const goToMyVideos = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
  }
  router.push('/my-videos');
};

onMounted(() => {
  const savedTheme = localStorage.getItem('theme');
  if (savedTheme === 'dark' || (!savedTheme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    isDark.value = true;
    document.documentElement.classList.add('dark');
  }
  fetchProgress();
  pollTimer = setInterval(fetchProgress, 2000);
});

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer);
  }
});
</script>

<style scoped>
.transcode-page {
  min-height: 100vh;
  background: var(--bg-primary, #f5f5f5);
  color: var(--text-primary, #333);
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-secondary, #fff);
  border-bottom: 1px solid var(--border-color, #e0e0e0);
}

.back-btn {
  padding: 8px 12px;
  background: none;
  border: none;
  color: var(--text-primary, #333);
  font-size: 14px;
  cursor: pointer;
}

.back-btn:hover {
  color: #737373;
}

.header-bar h2 {
  margin: 0;
  font-size: 18px;
}

.header-actions {
  width: 80px;
  display: flex;
  justify-content: flex-end;
}

.theme-toggle {
  padding: 8px 12px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
}

.theme-toggle:hover {
  color: var(--text-primary);
  background: var(--bg-hover);
}

.transcode-container {
  max-width: 600px;
  margin: 20px auto;
  padding: 20px;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e0e0e0;
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-icon {
  color: #999;
  margin-bottom: 16px;
}

.video-info {
  background: var(--bg-secondary, #fff);
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.video-info h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

.status-text {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.status-text.transcoding {
  color: #409eff;
}

.status-text.completed {
  color: #67c23a;
}

.progress-list {
  background: var(--bg-secondary, #fff);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}

.progress-item {
  margin-bottom: 16px;
}

.progress-item:last-child {
  margin-bottom: 0;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.resolution {
  font-weight: bold;
  font-size: 14px;
}

.task-status {
  flex: 1;
  text-align: center;
  font-size: 13px;
  color: #666;
}

.progress-value {
  font-size: 14px;
  color: #737373;
  min-width: 45px;
  text-align: right;
}

.progress-bar {
  height: 8px;
  background: #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-fill.p480 {
  background: linear-gradient(90deg, #a3a3a3, #a3a3a3);
}

.progress-fill.p720 {
  background: linear-gradient(90deg, #737373, #737373);
}

.progress-fill.p1080 {
  background: linear-gradient(90deg, #525252, #525252);
}

.progress-fill.overall {
  background: linear-gradient(90deg, #a3a3a3, #737373);
}

.progress-item.completed .progress-fill {
  background: #67c23a;
}

.progress-item.completed .task-status {
  color: #67c23a;
}

.overall-progress {
  background: var(--bg-secondary, #fff);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}

.overall-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: bold;
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 16px;
}

.countdown-tip {
  text-align: center;
  color: #67c23a;
  font-size: 16px;
  margin-top: 20px;
}

.countdown-number {
  font-size: 28px;
  font-weight: bold;
  margin-right: 8px;
}

.btn-primary {
  padding: 10px 24px;
  background: #737373;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.btn-primary:hover {
  background: #525252;
}

.btn-secondary {
  padding: 10px 24px;
  background: #fff;
  color: #666;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.btn-secondary:hover {
  color: #737373;
  border-color: #737373;
}

@media (prefers-color-scheme: dark) {
  .transcode-page {
    --bg-primary: #1a1a1a;
    --bg-secondary: #2d2d2d;
    --text-primary: #e0e0e0;
    --border-color: #404040;
  }
}

.transcode-page.dark {
  --bg-primary: #1a1a1a;
  --bg-secondary: #2d2d2d;
  --text-primary: #e0e0e0;
  --border-color: #404040;
}

.transcode-page.dark .header-bar {
  background: var(--bg-secondary);
  border-color: var(--border-color);
}

.transcode-page.dark .back-btn,
.transcode-page.dark h2,
.transcode-page.dark h3,
.transcode-page.dark p {
  color: var(--text-primary);
}

.transcode-page.dark .video-info,
.transcode-page.dark .progress-list,
.transcode-page.dark .overall-progress {
  background: var(--bg-secondary);
}

.transcode-page.dark .progress-bar {
  background: #404040;
}

.transcode-page.dark .btn-secondary {
  background: var(--bg-secondary);
  color: var(--text-primary);
  border-color: var(--border-color);
}

.transcode-page.dark .spinner {
  border-color: #404040;
}
</style>
