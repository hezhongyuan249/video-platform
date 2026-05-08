<template>
  <div class="my-videos-page" :class="{ dark: isDark }">
    <NavBar />

    <div class="content">
      <div class="page-header">
        <h2>我的收藏</h2>
        <button class="btn-text" @click="goHome">返回首页</button>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="favoritedVideos.length === 0" class="empty">
        <p>暂无收藏内容</p>
        <button class="btn-primary-sm" @click="goHome">去首页看看</button>
      </div>
      <div v-else class="video-grid">
        <VideoCard
          v-for="item in favoritedVideos"
          :key="item.id"
          :video="item"
          @click="goPlay"
        >
          <template #info>
            <span>{{ formatViews(item.views) }}播放</span>
            <button class="btn-secondary-sm" @click.stop="unfavoriteVideo(item.id)">取消收藏</button>
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
    if (user.value.id) getFavoritedVideos();
  });
  timer = setInterval(() => {
    checkUserStatus().then(() => {
      if (user.value.id) getFavoritedVideos();
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

const favoritedVideos = ref([]);
const loading = ref(false);

const getFavoritedVideos = async function() {
  loading.value = true;
  try {
    const res = await request.get('/video/favoritedList', {
      params: { userId: user.value.id }
    });
    if (res.code === 200) {
      favoritedVideos.value = res.data || [];
    }
  } catch (e) {
    console.error('获取收藏列表失败', e);
    favoritedVideos.value = [];
  } finally {
    loading.value = false;
  }
};

const goPlay = function(id) {
  router.push('/video-play/' + id);
};

const unfavoriteVideo = async function(videoId) {
  try {
    await ElMessageBox.confirm('确定取消收藏这个视频吗？', '提示', { type: 'warning' });
    const res = await request.post('/video/favorite', { videoId: videoId, userId: user.value.id });
    if (res.code === 200) {
      ElMessage.success('取消收藏成功');
      getFavoritedVideos();
    } else {
      ElMessage.error(res.msg || '取消收藏失败');
    }
  } catch (e) {
    ElMessage.info('已取消操作');
  }
};
</script>

<style scoped>
.my-videos-page { min-height: 100vh; background: var(--bg-primary); }

.header { height: 56px; background: var(--bg-primary); display: flex; justify-content: space-between; align-items: center; padding: 0 24px; border-bottom: 1px solid var(--border); position: sticky; top: 0; z-index: 100; }
.header-left { display: flex; align-items: center; }
.title { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.header-right { display: flex; align-items: center; gap: 16px; }

.btn-text { padding: 8px 12px; border: none; background: transparent; color: var(--text-secondary); font-size: 14px; cursor: pointer; }
.btn-text:hover { color: var(--text-primary); background: var(--bg-hover); }

.btn-primary-sm { padding: 8px 16px; background: var(--primary-500); color: #fff; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; }
.btn-primary-sm:hover { opacity: 0.9; }

.btn-secondary-sm { padding: 6px 12px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); font-size: 13px; border-radius: 4px; cursor: pointer; }
.btn-secondary-sm:hover { background: var(--bg-hover); }

.user-menu { position: relative; }
.user-menu:hover .dropdown { display: block; }

.user-btn {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dropdown { display: none; position: absolute; top: 100%; right: 0; background: var(--bg-primary); border: 1px solid var(--border); border-radius: 4px; min-width: 150px; padding: 8px 0; box-shadow: var(--shadow-md); }
.dropdown a { display: block; padding: 10px 16px; color: var(--text-primary); font-size: 14px; cursor: pointer; }
.dropdown a:hover { background: var(--bg-hover); }
.dropdown a.danger { color: var(--error-500); }

.content { max-width: 1200px; margin: 0 auto; padding: 24px; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: var(--text-primary); }

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
.dark .my-videos-page { background: var(--dark-bg-primary); }
.dark .header { background: var(--dark-bg-primary); border-color: var(--dark-border); }
.dark .title { color: var(--dark-text-primary); }
.dark .btn-text { color: var(--dark-text-secondary); }
.dark .btn-text:hover { background: var(--dark-bg-hover); color: var(--dark-text-primary); }
.dark .dropdown { background: var(--dark-bg-primary); border-color: var(--dark-border); }
.dark .dropdown a { color: var(--dark-text-primary); }
.dark .dropdown a:hover { background: var(--dark-bg-hover); }
.dark .video-cover { background: var(--dark-bg-tertiary); }
.dark .video-title { color: var(--dark-text-primary); }

.nav-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}
</style>
