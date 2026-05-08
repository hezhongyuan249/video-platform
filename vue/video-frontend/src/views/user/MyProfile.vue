<template>
  <div class="profile-page" :class="{ dark: isDark }">
    <NavBar />

    <div class="content-wrapper">
      <div class="page-header-bar">
        <button class="btn-text" @click="goHome">← 返回首页</button>
      </div>
      
      <!-- 用户信息卡片 -->
      <div class="profile-card">
        <div class="profile-header">
          <img :src="userInfo.avatar || defaultAvatar" class="profile-avatar" />
          <div class="profile-info">
            <h1 class="username">{{ userInfo.username }}</h1>
            <p class="description" v-if="userInfo.description">{{ userInfo.description }}</p>
            <p class="description empty" v-else @click="showEditDesc">点击添加简介</p>
          </div>
          <button class="edit-btn" @click="showEditDesc">编辑资料</button>
        </div>
        
        <div class="stats-row">
          <div class="stat-item">
            <span class="stat-value">{{ userInfo.videosCount || 0 }}</span>
            <span class="stat-label">作品</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ userInfo.followersCount || 0 }}</span>
            <span class="stat-label">粉丝</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ userInfo.likesCount || 0 }}</span>
            <span class="stat-label">获赞</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ userInfo.favoritesCount || 0 }}</span>
            <span class="stat-label">收藏</span>
          </div>
        </div>
      </div>

      <!-- 视频列表 -->
      <div class="videos-section">
        <h2 class="section-title">我的视频</h2>
        <div class="video-grid" v-if="videoList.length > 0">
          <VideoCard
            v-for="video in videoList"
            :key="video.id"
            :video="video"
            @click="playVideo"
          >
            <template #info>
              <span>{{ video.views || 0 }}播放</span>
            </template>
          </VideoCard>
        </div>
        <div class="empty-state" v-else>
          <p>还没有上传任何视频</p>
          <button class="upload-btn" @click="goUpload">上传视频</button>
        </div>
      </div>
    </div>

    <!-- 编辑简介弹窗 -->
    <div class="modal-overlay" v-if="editModalVisible" @click="editModalVisible = false">
      <div class="modal" @click.stop>
        <h3>编辑资料</h3>
        <div class="form-group">
          <label>简介</label>
          <textarea 
            v-model="editForm.description" 
            placeholder="介绍一下自己吧"
            class="textarea"
            maxlength="100"
          ></textarea>
          <span class="char-count">{{ editForm.description.length }}/100</span>
        </div>
        <div class="form-group">
          <label>头像链接</label>
          <input 
            v-model="editForm.avatar" 
            type="text" 
            placeholder="输入头像图片URL"
            class="input"
          />
        </div>
        <div class="modal-actions">
          <button class="btn-cancel" @click="editModalVisible = false">取消</button>
          <button class="btn-save" @click="saveProfile" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import VideoCard from '../../components/VideoCard.vue';
import NavBar from '../../components/NavBar.vue';

const router = useRouter();
const route = useRoute();
const isDark = ref(false);
const userInfo = ref({});
const videoList = ref([]);
const editModalVisible = ref(false);
const saving = ref(false);

const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI1MCIgZmlsbD0iI2UzZTNlMyIvPjx0ZXh0IHg9IjUwIiB5PSI1NSIgZm9udC1zaXplPSIyMCIgZmlsbD0iIzY2NiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPjwvdGV4dD48L3N2Zz4=';
const defaultCover = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjE4MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMzAwIiBoZWlnaHQ9IjE4MCIgZmlsbD0iI2YzZjRmNiIvPjx0ZXh0IHg9IjE1MCIgeT0iOTAiIGZvb250LXNpemU9IjIwIiBmaWxsPSIjOTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiPk5vIENvdmVyPC90ZXh0Pjwvc3ZnPg==';

const editForm = ref({
  description: '',
  avatar: ''
});

const currentUser = JSON.parse(sessionStorage.getItem('userInfo') || '{}');
const profileUserId = ref(route.params.userId || currentUser.id);

const goBack = () => router.back();
const goHome = () => router.push('/index');
const goUpload = () => router.push('/upload-video');

watch(() => route.params.userId, (newUserId) => {
  profileUserId.value = newUserId || currentUser.id;
  loadProfile();
  loadUserStats();
  loadVideos();
}, { immediate: true });

const toggleTheme = () => {
  isDark.value = !isDark.value;
  document.documentElement.classList.toggle('dark', isDark.value);
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light');
};

const formatDuration = (seconds) => {
  if (!seconds) return '0:00';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return `${date.getMonth() + 1}-${date.getDate()}`;
};

const showEditDesc = () => {
  editForm.value = {
    description: userInfo.value.description || '',
    avatar: userInfo.value.avatar || ''
  };
  editModalVisible.value = true;
};

const loadProfile = async () => {
  try {
    const res = await fetch('/api/user/profile?userId=' + profileUserId.value, {
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      userInfo.value = result.data || {};
      sessionStorage.setItem('userInfo', JSON.stringify(userInfo.value));
    }
  } catch (e) {
    console.error('加载用户信息失败', e);
  }
};

const loadUserStats = async () => {
  try {
    const res = await fetch('/api/video/userStats/' + profileUserId.value, {
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      userInfo.value.videosCount = result.data.videosCount || 0;
      userInfo.value.likesCount = result.data.likesCount || 0;
      userInfo.value.favoritesCount = result.data.favoritesCount || 0;
      userInfo.value.followersCount = result.data.followersCount || 0;
    }
  } catch (e) {
    console.error('加载用户统计失败', e);
  }
};

const loadVideos = async () => {
  try {
    const res = await fetch('/api/video/myList?userId=' + profileUserId.value, {
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      videoList.value = result.data || [];
    }
  } catch (e) {
    console.error('加载视频列表失败', e);
  }
};

const saveProfile = async () => {
  try {
    saving.value = true;
    const res = await fetch('/api/user/updateProfile', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: currentUser.id,
        description: editForm.value.description,
        avatar: editForm.value.avatar
      }),
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      ElMessage.success('保存成功');
      userInfo.value.description = editForm.value.description;
      userInfo.value.avatar = editForm.value.avatar;
      sessionStorage.setItem('userInfo', JSON.stringify(userInfo.value));
      editModalVisible.value = false;
    } else {
      ElMessage.error(result.msg || '保存失败');
    }
  } catch (e) {
    ElMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
};

const playVideo = (id) => {
  router.push('/video-play/' + id);
};

onMounted(() => {
  const savedTheme = localStorage.getItem('theme');
  if (savedTheme === 'dark') {
    isDark.value = true;
    document.documentElement.classList.add('dark');
  }
  loadProfile();
  loadUserStats();
  loadVideos();
});
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: var(--bg-primary);
  color: var(--text-primary);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
}

.header-right {
  display: flex;
  gap: 12px;
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

.content-wrapper {
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px;
}

.page-header-bar {
  margin-bottom: 16px;
}

.profile-card {
  background: var(--bg-secondary);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
}

.profile-header {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 20px;
}

.profile-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.profile-info {
  flex: 1;
}

.username {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
}

.description {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.5;
}

.description.empty {
  cursor: pointer;
  color: var(--text-tertiary);
}

.description.empty:hover {
  color: var(--primary-500);
}

.edit-btn {
  padding: 8px 16px;
  font-size: 14px;
  color: var(--text-primary);
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  cursor: pointer;
}

.edit-btn:hover {
  background: var(--border-color);
}

.stats-row {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.videos-section .section-title {
  font-size: 20px;
  margin: 0 0 20px;
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
  border-radius: 8px;
  overflow: hidden;
}

.video-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
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

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-secondary);
}

.upload-btn {
  margin-top: 16px;
  padding: 10px 24px;
  font-size: 15px;
  color: #fff;
  background: var(--primary-500);
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  width: 90%;
  max-width: 480px;
  background: var(--bg-secondary);
  border-radius: 12px;
  padding: 24px;
}

.modal h3 {
  margin: 0 0 20px;
  font-size: 18px;
}

.form-group {
  margin-bottom: 16px;
  position: relative;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

.input, .textarea {
  width: 100%;
  padding: 12px;
  font-size: 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-primary);
  box-sizing: border-box;
}

.textarea {
  min-height: 100px;
  resize: vertical;
}

.input:focus, .textarea:focus {
  outline: none;
  border-color: var(--primary-500);
}

.char-count {
  position: absolute;
  right: 8px;
  bottom: 8px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 20px;
}

.btn-cancel, .btn-save {
  padding: 10px 20px;
  font-size: 14px;
  border-radius: 8px;
  cursor: pointer;
}

.btn-cancel {
  color: var(--text-primary);
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
}

.btn-save {
  color: #fff;
  background: var(--primary-500);
  border: none;
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
