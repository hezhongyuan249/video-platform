<template>
  <div class="upload-page" :class="{ dark: isDark }">
    <header class="header">
      <div class="header-left">
        <button class="btn-text" @click="goBack">← 返回</button>
      </div>
      <div class="header-right">
        <button class="btn-text" @click="toggleTheme">{{ isDark ? '浅色' : '深色' }}</button>
      </div>
    </header>

    <div class="content-wrapper">
      <div class="upload-card">
        <h2>上传视频</h2>
        
        <div class="form-group">
          <label>视频标题</label>
          <input v-model="uploadForm.title" placeholder="请输入视频标题" maxlength="50" class="input" />
        </div>
        
        <div class="form-group">
          <label>视频简介</label>
          <textarea v-model="uploadForm.intro" placeholder="请输入简介" maxlength="200" class="textarea" rows="3"></textarea>
        </div>
        
        <div class="form-group">
          <label>视频文件</label>
          <input type="file" accept="video/*" @change="handleVideoFile" class="file-input" />
          <span class="hint">支持 MP4/MOV/AVI 格式，最大 1GB</span>
          <div v-if="uploadForm.videoFile" class="file-preview">
            已选择: {{ uploadForm.videoFile.name }}
          </div>
        </div>
        
        <div class="form-group">
          <label>封面图片</label>
          <input type="file" accept="image/*" @change="handleCoverFile" class="file-input" />
          <span class="hint">支持 JPG/PNG/WEBP 格式，最大 10MB</span>
          <div v-if="uploadForm.coverFile" class="file-preview">
            已选择: {{ uploadForm.coverFile.name }}
          </div>
        </div>
        
        <div class="form-group">
          <label>视频分类</label>
          <select v-model="uploadForm.videoType" class="select">
            <option value="">请选择分类</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
          </select>
        </div>
        
        <button class="btn-primary" @click="doUpload" :disabled="loading">
          {{ loading ? '上传中...' : '确认上传' }}
        </button>
        
        <div v-if="isUploading" class="progress-container">
          <div class="progress-info">
            <span>上传进度</span>
            <span>{{ uploadProgress }}%</span>
          </div>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import request from '../../utils/request';

const router = useRouter();
const loading = ref(false);
const isDark = ref(false);
const categories = ref([]);
const uploadProgress = ref(0);
const isUploading = ref(false);

const uploadForm = ref({
  title: '',
  intro: '',
  videoFile: null,
  coverFile: null,
  videoType: ''
});

const goBack = () => router.push('/index');

const toggleTheme = () => {
  isDark.value = !isDark.value;
  document.documentElement.classList.toggle('dark', isDark.value);
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light');
};

const handleVideoFile = (e) => {
  uploadForm.value.videoFile = e.target.files[0];
};

const handleCoverFile = (e) => {
  uploadForm.value.coverFile = e.target.files[0];
};

const loadCategories = async () => {
  try {
    const res = await request.get('/category/list');
    if (res.code === 200) {
      categories.value = res.data || [];
    }
  } catch (e) {
    console.error('加载分类失败', e);
  }
};

const doUpload = async () => {
  if (!uploadForm.value.title.trim()) {
    ElMessage.warning('请输入视频标题');
    return;
  }
  if (!uploadForm.value.videoFile) {
    ElMessage.warning('请选择视频文件');
    return;
  }
  if (!uploadForm.value.videoType) {
    ElMessage.warning('请选择视频分类');
    return;
  }

  try {
    loading.value = true;
    isUploading.value = true;
    uploadProgress.value = 0;
    
    const formData = new FormData();
    formData.append('title', uploadForm.value.title);
    formData.append('intro', uploadForm.value.intro || '');
    formData.append('video', uploadForm.value.videoFile);
    if (uploadForm.value.coverFile) {
      formData.append('cover', uploadForm.value.coverFile);
    }
    formData.append('videoType', uploadForm.value.videoType);
    
    const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}');
    formData.append('userId', userInfo.id);
    formData.append('username', userInfo.username);
    
    const result = await new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open('POST', '/api/video/upload');
      xhr.withCredentials = true;
      xhr.setRequestHeader('Accept', 'application/json');
      
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
          const percent = Math.round((e.loaded / e.total) * 100);
          uploadProgress.value = percent;
        }
      };
      
      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve(JSON.parse(xhr.responseText));
        } else {
          reject(new Error('上传失败'));
        }
      };
      
      xhr.onerror = () => reject(new Error('上传失败'));
      xhr.send(formData);
    });
    
    if (result.code === 200) {
      const videoId = result.data?.videoId;
      if (videoId) {
        uploadProgress.value = 100;
        ElMessage.success('文件上传完成，正在转码处理...');
        router.push(`/transcode/${videoId}`);
      } else {
        ElMessage.success('视频上传成功！');
        router.push('/my-videos');
      }
    } else {
      ElMessage.error(result.msg || '上传失败');
    }
  } catch (e) {
    ElMessage.error('上传失败：' + e.message);
  } finally {
    loading.value = false;
    isUploading.value = false;
    uploadProgress.value = 0;
  }
};

onMounted(() => {
  const savedTheme = localStorage.getItem('theme');
  if (savedTheme === 'dark') {
    isDark.value = true;
    document.documentElement.classList.add('dark');
  }
  loadCategories();
});
</script>

<style scoped>
.upload-page {
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
  max-width: 600px;
  margin: 0 auto;
  padding: 40px 16px;
}

.upload-card {
  background: var(--bg-secondary);
  padding: 32px;
  border-radius: 12px;
}

h2 {
  margin: 0 0 24px;
  font-size: 24px;
  text-align: center;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

.input, .textarea, .select {
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
  resize: vertical;
}

.input:focus, .textarea:focus, .select:focus {
  outline: none;
  border-color: var(--primary-500);
}

.file-input {
  width: 100%;
  padding: 10px;
  font-size: 14px;
  border: 1px dashed var(--border-color);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-primary);
  box-sizing: border-box;
  cursor: pointer;
}

.hint {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.file-preview {
  margin-top: 8px;
  padding: 8px 12px;
  font-size: 13px;
  background: var(--bg-tertiary);
  border-radius: 6px;
  color: var(--primary-500);
}

.btn-primary {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  font-weight: 500;
  color: #fff;
  background: var(--primary-500);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
  margin-top: 12px;
}

.btn-primary:hover:not(:disabled) {
  background: var(--primary-600);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.progress-container {
  margin-top: 20px;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: 8px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

.progress-bar {
  height: 8px;
  background: var(--bg-primary);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #a3a3a3, #737373);
  border-radius: 4px;
  transition: width 0.3s ease;
}
</style>
