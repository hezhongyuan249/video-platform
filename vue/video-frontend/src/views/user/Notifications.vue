<template>
  <div class="notifications-page">
    <div class="header-bar">
      <button class="back-btn" @click="router.back()">← 返回</button>
      <h2>消息通知</h2>
      <div class="header-actions">
        <button v-if="isEditMode" class="btn-cancel" @click="cancelEdit">取消</button>
        <button v-if="selectedIds.length > 0" class="btn-delete" @click="deleteSelected">删除选中 ({{ selectedIds.length }})</button>
        <button v-if="!isEditMode && list.length > 0" class="btn-text" @click="enterEditMode">删除</button>
        <button v-if="!isEditMode && list.length > 0" class="btn-text" @click="markAllRead">全部已读</button>
      </div>
    </div>
    
    <div class="tabs">
      <button :class="{ active: filterType === '' }" @click="filterType = ''; getList()">全部</button>
      <button :class="{ active: filterType === 'like' }" @click="filterType = 'like'; getList()">点赞</button>
      <button :class="{ active: filterType === 'favorite' }" @click="filterType = 'favorite'; getList()">收藏</button>
      <button :class="{ active: filterType === 'follow' }" @click="filterType = 'follow'; getList()">关注</button>
      <button :class="{ active: filterType === 'comment' }" @click="filterType = 'comment'; getList()">评论</button>
      <button :class="{ active: filterType === 'video' }" @click="filterType = 'video'; getList()">审核</button>
      <button class="btn-select-all" v-if="isEditMode && list.length > 0" @click="toggleSelectAll">
        {{ isAllSelected ? '取消全选' : '全选' }}
      </button>
    </div>
    
    <div class="notification-list">
      <div 
        v-for="item in list" 
        :key="item.id" 
        class="notification-item"
        :class="{ unread: item.isRead === 0 }"
      >
        <div class="checkbox-wrapper" v-if="isEditMode">
          <input 
            type="checkbox" 
            :value="item.id" 
            v-model="selectedIds"
            @click.stop
          />
        </div>
        <div class="notification-main" @click="handleClick(item)">
          <div class="notification-icon" :class="item.type">
            <svg v-if="item.type === 'like'" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
            </svg>
            <svg v-else-if="item.type === 'favorite'" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
            </svg>
            <svg v-else-if="item.type === 'follow'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="8.5" cy="7" r="4"/>
              <line x1="20" y1="8" x2="20" y2="14"/>
              <line x1="23" y1="11" x2="17" y2="11"/>
            </svg>
            <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
          </div>
          <div class="notification-content">
            <div class="notification-text">{{ item.content }}</div>
            <div class="notification-source" v-if="item.relatedVideoCover || item.relatedVideoTitle">
              <img v-if="item.relatedVideoCover" :src="item.relatedVideoCover" class="source-cover" />
              <div class="source-info">
                <div v-if="item.relatedVideoTitle" class="source-title">{{ item.relatedVideoTitle }}</div>
                <div v-if="item.relatedCommentContent" class="source-comment">"{{ item.relatedCommentContent }}"</div>
              </div>
            </div>
            <div class="notification-time">{{ formatTime(item.createTime) }}</div>
          </div>
        </div>
        <div class="unread-dot" v-if="item.isRead === 0"></div>
      </div>
      
      <div class="empty" v-if="list.length === 0">
        <p>暂无消息</p>
      </div>
    </div>
    
    <div class="pagination" v-if="total > pageSize">
      <button class="btn-page" :disabled="pageNum === 1" @click="pageNum--; getList()">上一页</button>
      <span class="page-num">{{ pageNum }}</span>
      <button class="btn-page" :disabled="list.length < pageSize" @click="pageNum++; getList()">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

const router = useRouter();
const list = ref([]);
const pageNum = ref(1);
const pageSize = ref(20);
const total = ref(0);
const filterType = ref('');
const selectedIds = ref([]);
const isEditMode = ref(false);
const isAllSelected = computed(() => list.value.length > 0 && selectedIds.value.length === list.value.length);

const enterEditMode = () => {
  isEditMode.value = true;
  selectedIds.value = [];
};

const cancelEdit = () => {
  isEditMode.value = false;
  selectedIds.value = [];
};

const getUserInfo = () => {
  const info = sessionStorage.getItem('userInfo');
  return info ? JSON.parse(info) : null;
};

const user = ref(getUserInfo() || {});

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedIds.value = [];
  } else {
    selectedIds.value = list.value.map(item => item.id);
  }
};

const getList = async () => {
  if (!user.value.id) return;
  try {
    const res = await fetch(`/api/notification/list?userId=${user.value.id}&pageNum=${pageNum.value}&pageSize=${pageSize.value}`, {
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      let data = result.data?.records || result.data || [];
      if (filterType.value) {
        if (filterType.value === 'video') {
          data = data.filter(item => item.type === 'video_upload' || item.type === 'video_review');
        } else if (filterType.value === 'comment') {
          data = data.filter(item => item.type === 'comment' || item.type === 'comment_reply');
        } else {
          data = data.filter(item => item.type === filterType.value);
        }
      }
      list.value = data;
      total.value = result.data?.total || data.length;
    }
  } catch (e) {
    ElMessage.error('获取消息列表失败');
  }
};

const handleClick = async (item) => {
  if (item.isRead === 0) {
    try {
      await fetch(`/api/notification/markReadOne?id=${item.id}`, { method: 'POST', credentials: 'include' });
      item.isRead = 1;
    } catch (e) {}
  }
  
  if (item.type === 'like' || item.type === 'favorite' || item.type === 'video_upload' || item.type === 'video_review') {
    router.push(`/video-play/${item.relatedId}`);
  } else if (item.type === 'follow') {
    router.push(`/user/${item.relatedId}`);
  }
};

const markAllRead = async () => {
  try {
    await fetch(`/api/notification/markRead?userId=${user.value.id}`, { method: 'POST', credentials: 'include' });
    list.value.forEach(item => item.isRead = 1);
    ElMessage.success('已全部标记为已读');
  } catch (e) {
    ElMessage.error('操作失败');
  }
};

const deleteSelected = async () => {
  if (selectedIds.value.length === 0) return;
  try {
    const res = await fetch('/api/notification/deleteBatch', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(selectedIds.value),
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      ElMessage.success('删除成功');
      selectedIds.value = [];
      isEditMode.value = false;
      getList();
    } else {
      ElMessage.error(result.message || '删除失败');
    }
  } catch (e) {
    ElMessage.error('删除失败');
  }
};

const formatTime = (timeStr) => {
  if (!timeStr) return '';
  const date = new Date(timeStr);
  const now = new Date();
  const diff = now - date;
  
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前';
  return date.toLocaleDateString();
};

onMounted(() => getList());
</script>

<style scoped>
.notifications-page {
  min-height: 100vh;
  background: var(--bg-primary);
  max-width: 800px;
  margin: 0 auto;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-bar h2 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
}

.back-btn {
  background: none;
  border: none;
  color: var(--text-primary);
  cursor: pointer;
  font-size: 14px;
}

.btn-text {
  background: none;
  border: none;
  color: var(--primary-500);
  cursor: pointer;
  font-size: 14px;
  padding: 8px 12px;
}

.btn-delete {
  background: #1a1a1a;
  border: none;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 500;
}

.btn-delete:hover {
  background: #333;
}

.btn-cancel {
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-primary);
  cursor: pointer;
  font-size: 14px;
  padding: 8px 16px;
  border-radius: 6px;
}

.btn-cancel:hover {
  background: var(--bg-hover);
}

.tabs {
  display: flex;
  gap: 8px;
  padding: 12px 24px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border);
}

.tabs button {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-secondary);
}

.tabs button.active {
  background: var(--primary-500);
  border-color: var(--primary-500);
  color: #fff;
}

.btn-select-all {
  margin-left: auto;
  background: #1a1a1a !important;
  border: 1px solid #1a1a1a !important;
  color: #fff !important;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 500;
}

.btn-select-all:hover {
  background: #333 !important;
  border-color: #333 !important;
}

.notification-list {
  padding: 0 24px;
}

.notification-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  transition: background 0.2s;
}

.checkbox-wrapper {
  flex-shrink: 0;
}

.checkbox-wrapper input {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: var(--primary-500);
}

.notification-main {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.notification-item:hover {
  background: var(--bg-hover);
}

.notification-item.unread {
  background: rgba(59, 130, 246, 0.05);
}

.notification-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notification-icon.like { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
.notification-icon.favorite { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
.notification-icon.follow { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.notification-icon.video { background: rgba(16, 185, 129, 0.1); color: #10b981; }

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-text {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.5;
}

.notification-time {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 4px;
}

.notification-source {
  display: flex;
  gap: 10px;
  margin-top: 8px;
  padding: 8px;
  background: var(--bg-secondary);
  border-radius: 6px;
}

.source-cover {
  width: 60px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.source-info {
  flex: 1;
  min-width: 0;
}

.source-title {
  font-size: 13px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-comment {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-500);
  flex-shrink: 0;
}

.empty {
  text-align: center;
  padding: 60px 0;
  color: var(--text-tertiary);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding: 20px;
}

.btn-page {
  padding: 8px 16px;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 4px;
  cursor: pointer;
  color: var(--text-primary);
}

.btn-page:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-num {
  font-size: 14px;
  color: var(--text-secondary);
}
</style>
