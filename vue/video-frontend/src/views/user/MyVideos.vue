<template>
  <div class="my-videos-page" :class="{ dark: isDark }">
    <NavBar />

    <div class="content">
      <div class="page-header">
        <h2>我的投稿</h2>
        <button class="btn-text" @click="goHome">返回首页</button>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="myVideos.length === 0" class="empty">
        <p>暂无投稿内容</p>
        <button class="btn-primary-sm" @click="router.push('/upload-video')">上传视频</button>
      </div>
      <div v-else class="video-grid">
        <VideoCard
          v-for="item in myVideos"
          :key="item.id"
          :video="item"
          @click="goPlay"
        >
          <template #info>
            <span>{{ formatViews(item.views) }}播放</span>
            <div class="video-actions">
              <button class="btn-secondary-sm" @click.stop="openEditModal(item)">编辑</button>
              <button class="btn-danger-sm" @click.stop="deleteVideo(item.id)">删除</button>
            </div>
          </template>
        </VideoCard>
      </div>

      <!-- 编辑弹窗 -->
      <div class="modal-overlay" v-if="openEdit" @click="openEdit = false">
        <div class="modal" @click.stop>
          <div class="modal-header">
            <h3>编辑视频</h3>
            <button class="close-btn" @click="openEdit = false">&times;</button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>标题 *</label>
              <input class="input" v-model="editForm.title" placeholder="输入视频标题" />
            </div>
            <div class="form-group">
              <label>简介</label>
              <textarea class="textarea" v-model="editForm.intro" placeholder="输入视频简介" rows="3"></textarea>
            </div>
            <div class="form-group">
              <label>分类 *</label>
              <select class="select" v-model="editForm.videoType">
                <option value="">选择分类</option>
                <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
              </select>
            </div>
            <div class="form-group">
              <label>封面图片</label>
              <input class="file-input" type="file" accept="image/*" @change="handleEditCoverChange" />
              <img v-if="editForm.coverPreview" :src="editForm.coverPreview" class="cover-preview" />
            </div>
            <div class="form-group">
              <label>更换视频</label>
              <input class="file-input" type="file" accept="video/*" @change="handleEditVideoChange" />
              <span class="file-name" v-if="editForm.videoFile">{{ editForm.videoFile.name }}</span>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" @click="openEdit = false">取消</button>
            <button class="btn-primary" @click="submitEdit" :disabled="editLoading">
              {{ editLoading ? '提交中...' : '提交审核' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
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
const categories = ref([
  { id: '1', name: '编程教程' },
  { id: '2', name: '技术分享' },
  { id: '3', name: '移动开发' },
  { id: '4', name: '后端技术' },
  { id: '5', name: '前端设计' }
]);

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
    if (user.value.id) getMyVideos();
  });
  timer = setInterval(() => {
    checkUserStatus().then(() => {
      if (user.value.id) getMyVideos();
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
  sessionStorage.removeItem('userInfo');
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
      sessionStorage.removeItem('userInfo');
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
      sessionStorage.setItem('userInfo', JSON.stringify(result.data));
    }
  } catch (e) {
    console.error('检查用户状态失败');
  }
};

const myVideos = ref([]);
const loading = ref(false);

const getMyVideos = async function() {
  loading.value = true;
  try {
    const res = await request.get('/video/myList', {
      params: { userId: user.value.id }
    });
    if (res.code === 200) {
      myVideos.value = res.data || [];
    }
  } catch (e) {
    console.error('获取视频列表失败', e);
    myVideos.value = [];
  } finally {
    loading.value = false;
  }
};

const goPlay = function(id) {
  router.push('/video-play/' + id);
};

const deleteVideo = async function(id) {
  try {
    await ElMessageBox.confirm('删除后无法恢复，确定要删除这个视频吗？', '确认删除', { 
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    const res = await request.post('/video/delete', { id: id, userId: user.value.id });
    if (res.code === 200) {
      ElMessage.success('删除成功');
      getMyVideos();
    } else {
      ElMessage.error(res.msg || '删除失败');
    }
  } catch (e) {
    ElMessage.info('已取消删除');
  }
};

const openUpload = ref(false);
const uploadForm = ref({ 
  title: '', 
  intro: '', 
  videoType: '',
  videoFile: null,
  coverFile: null
});
const uploadLoading = ref(false);

// 编辑弹窗
const openEditModal = function(video) {
  editForm.value = {
    id: video.id,
    title: video.title || '',
    intro: video.intro || '',
    videoType: video.videoType || '',
    coverFile: null,
    coverPreview: video.coverUrl || ''
  };
  openEdit.value = true;
};

const openEdit = ref(false);
const editForm = ref({
  id: null,
  title: '',
  intro: '',
  videoType: '',
  coverFile: null,
  coverPreview: '',
  videoFile: null
});
const editLoading = ref(false);

const handleEditVideoChange = function(e) {
  const file = e.target.files[0];
  if (file) {
    if (file.size > 500 * 1024 * 1024) {
      ElMessage.warning('视频文件不能超过500MB');
      e.target.value = '';
      return;
    }
    editForm.value.videoFile = file;
  }
};

const handleEditCoverChange = function(e) {
  const file = e.target.files[0];
  if (file) {
    if (file.size > 10 * 1024 * 1024) {
      ElMessage.warning('封面图片不能超过10MB');
      e.target.value = '';
      return;
    }
    editForm.value.coverFile = file;
    editForm.value.coverPreview = URL.createObjectURL(file);
  }
};

const submitEdit = async function() {
  if (!editForm.value.title || !editForm.value.videoType) {
    ElMessage.warning('请填写标题和分类');
    return;
  }
  
  editLoading.value = true;
  try {
    const formData = new FormData();
    formData.append('id', editForm.value.id);
    formData.append('title', editForm.value.title);
    formData.append('intro', editForm.value.intro || '');
    formData.append('videoType', editForm.value.videoType);
    formData.append('userId', user.value.id);
    if (editForm.value.coverFile) {
      formData.append('coverFile', editForm.value.coverFile);
    }
    if (editForm.value.videoFile) {
      formData.append('videoFile', editForm.value.videoFile);
    }
    
    const res = await fetch('/api/video/edit', {
      method: 'POST',
      body: formData,
      credentials: 'include'
    });
    
    const data = await res.json();
    if (data.code === 200) {
      ElMessage.success('提交成功，等待审核');
      openEdit.value = false;
      getMyVideos();
    } else {
      ElMessage.error(data.msg || '编辑失败');
    }
  } catch (e) {
    console.error('编辑失败', e);
    ElMessage.error('编辑失败，请稍后重试');
  } finally {
    editLoading.value = false;
  }
};

const canUpload = computed(function() {
  const result = uploadForm.value.title && 
         uploadForm.value.videoType && 
         uploadForm.value.videoFile && 
         uploadForm.value.coverFile;
  console.log('canUpload:', result);
  console.log('title:', uploadForm.value.title);
  console.log('videoType:', uploadForm.value.videoType);
  console.log('videoFile:', uploadForm.value.videoFile);
  console.log('coverFile:', uploadForm.value.coverFile);
  return result;
});

const handleVideoChange = function(e) {
  const file = e.target.files[0];
  if (file) {
    if (file.size > 500 * 1024 * 1024) {
      ElMessage.warning('视频文件不能超过500MB');
      e.target.value = '';
      return;
    }
    uploadForm.value.videoFile = file;
  }
};

const handleCoverChange = function(e) {
  const file = e.target.files[0];
  if (file) {
    if (file.size > 10 * 1024 * 1024) {
      ElMessage.warning('封面图片不能超过10MB');
      e.target.value = '';
      return;
    }
    uploadForm.value.coverFile = file;
  }
};

const submitUpload = async function() {
  if (!canUpload.value) {
    ElMessage.warning('请填写完整信息并选择视频和封面');
    return;
  }
  
  uploadLoading.value = true;
  try {
    console.log('开始上传');
    console.log('用户信息:', user.value);
    console.log('上传表单:', uploadForm.value);
    
    const formData = new FormData();
    formData.append('title', uploadForm.value.title);
    formData.append('intro', uploadForm.value.intro || '');
    formData.append('video', uploadForm.value.videoFile);
    formData.append('cover', uploadForm.value.coverFile);
    formData.append('userId', user.value.id);
    formData.append('username', user.value.username);
    formData.append('videoType', uploadForm.value.videoType);

    console.log('FormData 准备完成');
    console.log('准备发送请求到 /api/video/upload');
    
    // 手动发送请求，添加详细的错误处理
    const res = await fetch('/api/video/upload', {
      method: 'POST',
      body: formData,
      credentials: 'include'
    });
    
    console.log('响应状态:', res.status);
    console.log('响应头:', res.headers);
    
    const data = await res.json();
    console.log('收到响应:', data);

    if (data.code === 200) {
      ElMessage.success('上传成功');
      openUpload.value = false;
      uploadForm.value = { title: '', intro: '', videoType: '', videoFile: null, coverFile: null };
      getMyVideos();
    } else {
      ElMessage.error(data.msg || '上传失败');
    }
  } catch (e) {
    console.error('上传失败', e);
    ElMessage.error('上传失败，请稍后重试');
  } finally {
    uploadLoading.value = false;
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
.btn-primary-sm:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-secondary-sm { padding: 6px 12px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); font-size: 13px; border-radius: 4px; cursor: pointer; }
.btn-secondary-sm:hover { background: var(--bg-hover); }

.video-actions { display: flex; gap: 8px; }

.btn-danger-sm { padding: 6px 12px; border: 1px solid var(--error-500); background: transparent; color: var(--error-500); font-size: 13px; border-radius: 4px; cursor: pointer; }
.btn-danger-sm:hover { background: var(--error-500); color: #fff; }

.cover-preview { width: 100px; height: 60px; object-fit: cover; margin-top: 8px; border-radius: 4px; }

.btn-secondary { padding: 10px 20px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); font-size: 14px; border-radius: 4px; cursor: pointer; }

.btn-primary { padding: 10px 20px; border: none; background: var(--text-primary); color: var(--bg-primary); font-size: 14px; border-radius: 4px; cursor: pointer; }
.btn-primary:hover { opacity: 0.9; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary:hover { background: var(--bg-hover); }

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

.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: var(--bg-primary); border-radius: 8px; width: 90%; max-width: 500px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid var(--border); }
.modal-header h3 { font-size: 16px; font-weight: 600; }
.close-btn { background: none; border: none; font-size: 24px; cursor: pointer; color: var(--text-tertiary); }
.modal-body { padding: 20px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 16px 20px; border-top: 1px solid var(--border); }

.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 8px; font-size: 14px; font-weight: 500; }
.input, .textarea, .select { width: 100%; padding: 10px 12px; border: 1px solid var(--border); border-radius: 4px; background: var(--bg-primary); color: var(--text-primary); font-size: 14px; }
.input:focus, .textarea:focus, .select:focus { outline: none; border-color: var(--text-primary); }
.textarea { resize: vertical; }
.file-input { width: 100%; padding: 8px; border: 1px solid var(--border); border-radius: 4px; background: var(--bg-primary); color: var(--text-primary); font-size: 14px; }
.file-name { display: block; margin-top: 8px; font-size: 13px; color: var(--text-secondary); }

/* 深色模式 */
.dark .my-videos-page { background: var(--dark-bg-primary); }
.dark .header { background: var(--dark-bg-primary); border-color: var(--dark-border); }
.dark .title { color: var(--dark-text-primary); }
.dark .btn-text { color: var(--dark-text-secondary); }
.dark .btn-text:hover { background: var(--dark-bg-hover); color: var(--dark-text-primary); }
.dark .dropdown { background: var(--dark-bg-primary); border-color: var(--dark-border); }
.dark .dropdown a { color: var(--dark-text-primary); }
.dark .dropdown a:hover { background: var(--dark-bg-hover); }
.dark .video-card { border-color: var(--dark-border); }
.dark .video-cover { background: var(--dark-bg-tertiary); }
.dark .video-title { color: var(--dark-text-primary); }
.dark .modal { background: var(--dark-bg-primary); }
.dark .modal-header, .modal-footer { border-color: var(--dark-border); }
.dark .modal-header h3, .close-btn { color: var(--dark-text-primary); }
.dark .input, .dark .textarea, .dark .select, .dark .file-input { background: var(--dark-bg-primary); border-color: var(--dark-border); color: var(--dark-text-primary); }

.nav-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}
</style>
