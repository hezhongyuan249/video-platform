<template>
  <div class="admin-video">
    <div class="action-bar">
      <h2>视频管理</h2>
      <div class="action-right">
        <input type="text" v-model="searchTitle" placeholder="搜索视频标题..." class="search-input" @keyup.enter="search" />
        <button class="btn-primary-sm" @click="search">搜索</button>
        <button class="btn-danger-sm" @click="clearAll">清空所有视频</button>
      </div>
    </div>
    
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>封面</th>
            <th>视频标题</th>
            <th>简介</th>
            <th>上传用户</th>
            <th>上传时间</th>
            <th>状态</th>
            <th>审核状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td><img :src="item.coverUrl" class="video-cover" @click="openPreview(item)" style="cursor:pointer" /></td>
            <td>{{ item.title }}</td>
            <td class="intro-cell">{{ item.intro || '暂无' }}</td>
            <td>{{ item.username }}</td>
            <td>{{ formatDate(item.createTime) }}</td>
            <td>
              <span class="status-tag" :class="item.status === 0 ? 'success' : 'warning'">
                {{ item.status === 0 ? '正常' : '已下线' }}
              </span>
            </td>
            <td>
              <span class="status-tag" :class="getReviewClass(item.reviewStatus)">
                {{ getReviewText(item.reviewStatus) }}
              </span>
            </td>
            <td>
              <button class="btn-text-sm" @click="openPreview(item)">查看</button>
              <button class="btn-text-sm" @click="changeStatus(item)">切换</button>
              <button class="btn-text-sm danger" @click="del(item.id)">删除</button>
            </td>
          </tr>
          <tr v-if="list.length === 0">
            <td colspan="8" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <div class="pagination">
      <span class="pagination-info">共 {{ total }} 条</span>
      <div class="pagination-buttons">
        <button class="btn-page" :disabled="pageNum === 1" @click="pageNum--; getList()">上一页</button>
        <span class="page-num">{{ pageNum }}</span>
        <button class="btn-page" :disabled="list.length < pageSize" @click="pageNum++; getList()">下一页</button>
      </div>
    </div>

    <!-- 视频预览弹窗 -->
    <div class="modal-overlay" v-if="previewVisible" @click="previewVisible = false">
      <div class="preview-modal" @click.stop>
        <div class="preview-header">
          <h3>视频预览</h3>
          <button class="close-btn" @click="previewVisible = false">×</button>
        </div>
        <div class="preview-content">
          <video :src="previewData.videoUrl" controls class="preview-video"></video>
          <div class="preview-info">
            <h4>{{ previewData.title }}</h4>
            <p><strong>作者：</strong>{{ previewData.username }}</p>
            <p><strong>简介：</strong>{{ previewData.intro || '暂无' }}</p>
            <p><strong>审核状态：</strong>
              <span :class="getReviewClass(previewData.reviewStatus)">{{ getReviewText(previewData.reviewStatus) }}</span>
            </p>
            <p v-if="previewData.reviewStatus === 2"><strong>拒绝原因：</strong>{{ previewData.rejectReason }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';

const list = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const searchTitle = ref('');

const previewVisible = ref(false);
const previewData = ref({});

const getList = async () => {
  try {
    let url = `/api/video/admin/list?pageNum=${pageNum.value}&pageSize=${pageSize.value}`;
    if (searchTitle.value) {
      url += `&title=${encodeURIComponent(searchTitle.value)}`;
    }
    const res = await fetch(url, {
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      list.value = result.data?.records || result.data || [];
      total.value = result.data?.total || result.data?.length || 0;
    }
  } catch (e) {
    ElMessage.error('获取视频列表失败');
  }
};

const openPreview = (item) => {
  previewData.value = item;
  previewVisible.value = true;
};

const search = () => {
  pageNum.value = 1;
  getList();
};

const getReviewClass = (status) => {
  if (status === 0) return 'warning';
  if (status === 1) return 'success';
  return 'danger';
};

const getReviewText = (status) => {
  if (status === 0) return '待审核';
  if (status === 1) return '已通过';
  return '已拒绝';
};

const formatDate = (dateStr) => {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

const changeStatus = async (row) => {
  const oldStatus = row.status;
  try {
    const res = await fetch('/api/video/admin/changeStatus', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id: row.id, status: oldStatus === 0 ? 1 : 0 }),
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      row.status = oldStatus === 0 ? 1 : 0;
      ElMessage.success('操作成功');
    } else {
      ElMessage.error(result.msg || '操作失败');
    }
  } catch (e) {
    ElMessage.error('操作失败');
    row.status = oldStatus;
  }
};

const del = async (id) => {
  if (!confirm('确定删除？删除后无法恢复！')) return;
  try {
    const res = await fetch('/api/video/admin/delete', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id }),
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      ElMessage.success('删除成功');
      getList();
    } else {
      ElMessage.error(result.msg || '删除失败');
    }
  } catch (e) {
    ElMessage.error('删除失败');
  }
};

const clearAll = async () => {
  if (!confirm('确定清空所有视频数据？此操作不可恢复！')) return;
  try {
    const res = await fetch('/api/video/admin/clearAll', {
      method: 'DELETE',
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      ElMessage.success('视频数据已清空');
      getList();
    } else {
      ElMessage.error(result.msg || '清空失败');
    }
  } catch (e) {
    ElMessage.error('清空失败');
  }
};

onMounted(() => getList());
</script>

<style scoped>
.admin-video { padding: 24px; background: var(--bg-primary); }
.action-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.action-bar h2 { font-size: 20px; font-weight: 600; color: var(--text-primary); margin: 0; }
.action-right { display: flex; gap: 8px; align-items: center; }

.search-input { padding: 8px 12px; border: 1px solid var(--border); border-radius: 4px; font-size: 14px; background: var(--bg-primary); color: var(--text-primary); }
.search-input:focus { outline: none; border-color: var(--primary-500); }

.btn-primary-sm { padding: 8px 16px; background: var(--primary-500); color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 14px; }
.btn-primary-sm:hover { background: var(--primary-600); }

.btn-danger-sm { padding: 8px 16px; background: var(--error-500); color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 14px; }
.btn-danger-sm:hover { background: var(--error-600); }

.table-container { overflow: visible; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 12px 16px; text-align: left; border-bottom: 1px solid var(--border); }
.data-table th { background: var(--bg-secondary); font-weight: 500; font-size: 14px; color: var(--text-secondary); }
.data-table td { font-size: 14px; color: var(--text-primary); }
.data-table tr:hover td { background: var(--bg-hover); }
.empty { text-align: center; color: var(--text-tertiary); }

.status-tag { padding: 4px 8px; border-radius: 4px; font-size: 12px; }
.status-tag.success { background: var(--success-500); color: #fff; }
.status-tag.warning { background: var(--warning-500); color: #fff; }
.status-tag.danger { background: var(--error-500); color: #fff; }

.video-cover { width: 60px; height: 34px; object-fit: cover; border-radius: 4px; cursor: pointer; }
.intro-cell { max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.preview-modal { background: var(--bg-primary); border-radius: 8px; width: 90%; max-width: 800px; max-height: 90vh; overflow: auto; }
.preview-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid var(--border); }
.preview-header h3 { margin: 0; font-size: 18px; color: var(--text-primary); }
.close-btn { background: none; border: none; font-size: 24px; cursor: pointer; color: var(--text-secondary); }
.preview-content { padding: 20px; }
.preview-video { width: 100%; max-height: 400px; background: #000; }
.preview-info { margin-top: 16px; }
.preview-info h4 { margin: 0 0 12px; color: var(--text-primary); }
.preview-info p { margin: 8px 0; color: var(--text-secondary); font-size: 14px; }

.btn-text-sm { padding: 4px 8px; background: transparent; border: 1px solid var(--border); border-radius: 4px; cursor: pointer; font-size: 13px; color: var(--text-primary); margin-right: 8px; }
.btn-text-sm:hover { background: var(--bg-hover); }
.btn-text-sm.danger { color: var(--error-500); border-color: var(--error-500); }
.btn-text-sm.danger:hover { background: rgba(239, 68, 68, 0.1); }

.pagination { display: flex; justify-content: space-between; align-items: center; margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--border); }
.pagination-info { font-size: 14px; color: var(--text-secondary); }
.pagination-buttons { display: flex; align-items: center; gap: 12px; }
.btn-page { padding: 6px 12px; background: var(--bg-primary); border: 1px solid var(--border); border-radius: 4px; cursor: pointer; font-size: 14px; }
.btn-page:hover:not(:disabled) { background: var(--bg-hover); }
.btn-page:disabled { opacity: 0.5; cursor: not-allowed; }
.page-num { font-size: 14px; color: var(--text-secondary); }

.dark .admin-video { background: var(--dark-bg-primary); }
.dark .action-bar h2 { color: var(--dark-text-primary); }
.dark .data-table th { background: var(--dark-bg-secondary); color: var(--dark-text-secondary); }
.dark .data-table td { color: var(--dark-text-primary); }
.dark .data-table tr:hover td { background: var(--dark-bg-hover); }
.dark .empty { color: var(--dark-text-tertiary); }
.dark .btn-text-sm { border-color: var(--dark-border); color: var(--dark-text-primary); }
.dark .btn-page { background: var(--dark-bg-primary); border-color: var(--dark-border); color: var(--dark-text-primary); }
.dark .pagination { border-color: var(--dark-border); }
.dark .pagination-info, .dark .page-num { color: var(--dark-text-secondary); }
</style>
