<template>
  <div class="following-page" :class="{ dark: isDark }">
    <NavBar />

    <div class="content">
      <div class="page-header">
        <h2>我的关注</h2>
        <button class="btn-text" @click="goHome">返回首页</button>
      </div>

      <!-- 关注的作者列表 -->
      <div class="authors-section" v-if="followingList.length > 0">
        <div 
          class="author-card" 
          :class="{ active: selectedAuthorId === null }"
          @click="selectAuthor(null)"
        >
          <div class="author-avatar all-avatar">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="7"></rect>
              <rect x="14" y="3" width="7" height="7"></rect>
              <rect x="14" y="14" width="7" height="7"></rect>
              <rect x="3" y="14" width="7" height="7"></rect>
            </svg>
          </div>
          <span class="author-name">全部</span>
        </div>
        <div 
          class="author-card" 
          v-for="author in followingList" 
          :key="author.id"
          :class="{ active: selectedAuthorId === author.id }"
          @click="selectAuthor(author.id)"
        >
          <img :src="author.avatar || defaultAvatar" class="author-avatar" />
          <span class="author-name">{{ author.username }}</span>
        </div>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="followingList.length === 0" class="empty">
        <p>还没有关注任何作者</p>
        <button class="btn-primary-sm" @click="goHome">去首页发现作者</button>
      </div>
      <div v-else-if="videoList.length === 0" class="empty">
        <p>{{ selectedAuthorId ? '该作者还没有发布视频' : '暂无视频' }}</p>
      </div>
      <div v-else class="video-grid">
        <VideoCard
          v-for="item in videoList"
          :key="item.id"
          :video="item"
          @click="goPlay"
        >
          <template #info>
            <span>{{ item.username }}</span>
            <span>{{ formatViews(item.views) }}播放</span>
          </template>
        </VideoCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import request from '../../utils/request';
import { getUserInfo, setUserInfo, clearUserInfo } from '../../utils/userStorage';
import VideoCard from '../../components/VideoCard.vue';
import NavBar from '../../components/NavBar.vue';
import { formatViews, formatDuration } from '../../utils/format';

const router = useRouter();
const user = ref(getUserInfo() || {});
const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI1MCIgZmlsbD0iI2UzZTNlMyIvPjx0ZXh0IHg9IjUwIiB5PSI1NSIgZm9udC1zaXplPSIyMCIgZmlsbD0iIzY2NiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPjwvdGV4dD48L3N2Zz4=';
let timer = null;
const isDark = ref(false);
const loading = ref(false);
const followingList = ref([]);
const videoList = ref([]);
const selectedAuthorId = ref(null);

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
  checkUserStatus().then(() => {
    if (user.value.id) {
      loadFollowingList();
    }
  });
  timer = setInterval(() => {
    checkUserStatus().then(() => {
      if (user.value.id) {
        loadFollowingList();
      }
    });
  }, 3000);
});

onUnmounted(function() {
  clearInterval(timer);
});

const goHome = function() {
  router.push('/index');
};

const goPage = function(url) {
  router.push('/' + url);
};

const logout = function() {
  if (!confirm('确定要退出登录吗？')) return
  clearUserInfo();
  user.value = {};
  clearInterval(timer);
  router.push('/login');
  ElMessage.success('退出登录成功');
};

const cancelAccount = async function() {
  try {
    await ElMessageBox.confirm('确定注销账号吗？7天后数据将永久清除！', '警告', { type: 'warning' });
    const res = await fetch('/api/user/delete', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id: user.value.id }),
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      ElMessage.success('注销申请已提交');
      clearUserInfo();
      user.value = {};
      router.push('/login');
    } else {
      ElMessage.error(result.msg || '注销失败');
    }
  } catch (e) {
    ElMessage.info('已取消');
  }
};

const checkUserStatus = async function() {
  if (!user.value.id) return;
  try {
    const res = await fetch('/api/user/info?id=' + user.value.id, {
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      user.value = result.data;
      setUserInfo(result.data);
    }
  } catch (e) {
    console.error('检查用户状态失败');
  }
};

const loadFollowingList = async function() {
  try {
    const res = await fetch('/api/video/myFollowingList?userId=' + user.value.id, {
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      followingList.value = result.data || [];
      loadVideos();
    }
  } catch (e) {
    console.error('获取关注列表失败', e);
  }
};

const selectAuthor = function(authorId) {
  selectedAuthorId.value = authorId;
  loadVideos();
};

const loadVideos = async function() {
  loading.value = true;
  try {
    let url = '/api/video/followingVideos?userId=' + user.value.id;
    if (selectedAuthorId.value) {
      url += '&authorId=' + selectedAuthorId.value;
    }
    const res = await fetch(url, {
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      videoList.value = result.data || [];
    }
  } catch (e) {
    console.error('获取视频列表失败', e);
    videoList.value = [];
  } finally {
    loading.value = false;
  }
};

const goPlay = function(id) {
  router.push('/video-play/' + id);
};
</script>

<style scoped>
.following-page { min-height: 100vh; background: var(--bg-primary); }

.header { height: 56px; background: var(--bg-primary); display: flex; justify-content: space-between; align-items: center; padding: 0 24px; border-bottom: 1px solid var(--border); position: sticky; top: 0; z-index: 100; }
.header-left { display: flex; align-items: center; }
.title { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.header-right { display: flex; align-items: center; gap: 16px; }

.btn-text { padding: 8px 12px; border: none; background: transparent; color: var(--text-secondary); font-size: 14px; cursor: pointer; }
.btn-text:hover { color: var(--text-primary); background: var(--bg-hover); }

.btn-primary-sm { padding: 8px 16px; background: var(--primary-500); color: #fff; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; }
.btn-primary-sm:hover { opacity: 0.9; }

.user-menu { position: relative; }
.user-menu:hover .dropdown { display: block; }

.user-btn { display: flex; align-items: center; gap: 8px; }

.dropdown { display: none; position: absolute; top: 100%; right: 0; background: var(--bg-primary); border: 1px solid var(--border); border-radius: 4px; min-width: 150px; padding: 8px 0; box-shadow: var(--shadow-md); }
.dropdown a { display: block; padding: 10px 16px; color: var(--text-primary); font-size: 14px; cursor: pointer; }
.dropdown a:hover { background: var(--bg-hover); }
.dropdown a.danger { color: var(--error-500); }

.content { max-width: 1200px; margin: 0 auto; padding: 24px; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: var(--text-primary); }

.authors-section {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.author-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  min-width: 80px;
  transition: background 0.2s;
}

.author-card:hover {
  background: var(--bg-hover);
}

.author-card.active {
  background: var(--bg-tertiary);
}

.author-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
}

.all-avatar {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.author-name {
  font-size: 13px;
  color: var(--text-primary);
  text-align: center;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.loading { padding: 60px; text-align: center; color: var(--text-tertiary); }
.empty { padding: 60px; text-align: center; }
.empty p { color: var(--text-tertiary); margin-bottom: 16px; }

.video-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 24px; }
.video-item { cursor: pointer; }
.video-cover { position: relative; aspect-ratio: 16/9; background: var(--bg-tertiary); border-radius: 8px; overflow: hidden; }
.video-cover img { width: 100%; height: 100%; object-fit: cover; }
.video-cover .duration { position: absolute; bottom: 8px; right: 8px; background: rgba(0,0,0,0.75); color: #fff; font-size: 12px; padding: 2px 6px; border-radius: 4px; }
.video-meta { padding: 12px 0; }
.video-title { font-size: 14px; font-weight: 500; margin-bottom: 8px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.4; }
.video-info { display: flex; justify-content: space-between; font-size: 12px; color: var(--text-tertiary); }

/* 深色模式 */
.dark .following-page { background: var(--dark-bg-primary); }
.dark .header { background: var(--dark-bg-primary); border-color: var(--dark-border); }
.dark .title { color: var(--dark-text-primary); }
.dark .btn-text { color: var(--dark-text-secondary); }
.dark .btn-text:hover { background: var(--dark-bg-hover); color: var(--dark-text-primary); }
.dark .dropdown { background: var(--dark-bg-primary); border-color: var(--dark-border); }
.dark .dropdown a { color: var(--dark-text-primary); }
.dark .dropdown a:hover { background: var(--dark-bg-hover); }
.dark .author-card:hover { background: var(--dark-bg-hover); }
.dark .author-card.active { background: var(--dark-bg-tertiary); }
.dark .author-name { color: var(--dark-text-primary); }
.dark .all-avatar { background: var(--dark-bg-tertiary); color: var(--dark-text-secondary); }
.dark .video-cover { background: var(--dark-bg-tertiary); }
.dark .video-title { color: var(--dark-text-primary); }

.nav-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}
</style>
