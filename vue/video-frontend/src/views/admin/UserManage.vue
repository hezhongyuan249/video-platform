<template>
  <div class="user-manage">
    <div class="action-bar">
      <h2>用户管理</h2>
      <div class="search-box">
        <input v-model="searchUsername" placeholder="搜索用户名..." class="search-input" @keyup.enter="getUserList" />
        <button class="btn-primary-sm" @click="getUserList">搜索</button>
        <button class="btn-secondary-sm" @click="resetSearch">重置</button>
      </div>
    </div>
    
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>身份</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in userList" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.username }}</td>
            <td>
              <span class="role-tag" :class="item.role === 'admin' ? 'admin' : 'user'">
                {{ item.role === 'admin' ? '管理员' : '普通用户' }}
              </span>
            </td>
            <td>
              <span class="status-tag" :class="item.status === 0 ? 'success' : 'danger'">
                {{ item.status === 0 ? '正常' : '已封禁' }}
              </span>
            </td>
            <td>
              <button class="btn-text-sm" :disabled="item.role === 'admin' || item.id === currentAdminId" @click="toggleStatus(item)">
                {{ item.status === 0 ? '封禁' : '解封' }}
              </button>
              <button class="btn-text-sm danger" :disabled="item.role === 'admin' || item.id === currentAdminId" @click="deleteUser(item.id)">删除</button>
            </td>
          </tr>
          <tr v-if="userList.length === 0">
            <td colspan="5" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <div class="pagination">
      <span class="pagination-info">共 {{ total }} 条</span>
      <div class="pagination-buttons">
        <button class="btn-page" :disabled="pageNum === 1" @click="pageNum--; getUserList()">上一页</button>
        <span class="page-num">{{ pageNum }}</span>
        <button class="btn-page" :disabled="userList.length < pageSize" @click="pageNum++; getUserList()">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { USER_STATUS, USER_ROLE } from '../../constants';

const userList = ref([]);
const currentAdminId = ref(null);
const searchUsername = ref('');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const getCurrentLoginUser = function() {
  const userInfo = sessionStorage.getItem('userInfo');
  if (userInfo) currentAdminId.value = JSON.parse(userInfo).id;
};

const getUserList = async function() {
  try {
    const res = await fetch('/api/user/admin/list?username=' + (searchUsername.value || ''), {
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      userList.value = result.data?.records || result.data || [];
      total.value = result.data?.total || result.data?.length || 0;
    }
  } catch (e) {
    ElMessage.error('获取用户列表失败');
  }
};

const resetSearch = function() {
  searchUsername.value = '';
  pageNum.value = 1;
  getUserList();
};

const toggleStatus = async function(row) {
  if (row.role === 'admin') { ElMessage.warning('禁止操作管理员账号！'); return; }
  if (row.id === currentAdminId.value) { ElMessage.warning('禁止操作自身！'); return; }
  const msg = row.status === 0 ? '确认封禁该用户？' : '确认解封该用户？';
  if (!confirm(msg)) return;
  try {
    const res = await fetch('/api/user/admin/changeStatus', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id: row.id, status: row.status === 0 ? 1 : 0 }),
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      ElMessage.success('操作成功！');
      getUserList();
    } else {
      ElMessage.error(result.msg || '操作失败');
    }
  } catch (e) {
    ElMessage.error('操作失败');
  }
};

const deleteUser = async function(id) {
  const user = userList.value.find(item => item.id === id);
  if (user.role === 'admin') { ElMessage.warning('禁止删除管理员账号！'); return; }
  if (id === currentAdminId.value) { ElMessage.warning('禁止删除自身！'); return; }
  if (!confirm('确认永久删除该用户？删除后无法恢复！')) return;
  try {
    const res = await fetch('/api/user/admin/delete/' + id, {
      credentials: 'include'
    });
    const result = await res.json();
    if (result.code === 200) {
      ElMessage.success('删除成功！');
      getUserList();
    } else {
      ElMessage.error(result.msg || '删除失败');
    }
  } catch (e) {
    ElMessage.error('删除失败');
  }
};

onMounted(function() {
  getCurrentLoginUser();
  getUserList();
});
</script>

<style scoped>
.user-manage { padding: 24px; background: var(--bg-primary); }
.action-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; flex-wrap: wrap; gap: 16px; }
.action-bar h2 { font-size: 20px; font-weight: 600; color: var(--text-primary); margin: 0; }
.search-box { display: flex; gap: 10px; align-items: center; }
.search-input { padding: 8px 12px; border: 1px solid var(--border); border-radius: 4px; font-size: 14px; width: 200px; }

.btn-primary-sm { padding: 8px 16px; background: var(--primary-500); color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; }
.btn-primary-sm:hover { opacity: 0.9; }

.btn-secondary-sm { padding: 8px 16px; background: transparent; color: var(--text-primary); border: 1px solid var(--border); border-radius: 4px; cursor: pointer; font-size: 14px; }
.btn-secondary-sm:hover { background: var(--bg-hover); }

.table-container { overflow: visible; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 12px 16px; text-align: left; border-bottom: 1px solid var(--border); }
.data-table th { background: var(--bg-secondary); font-weight: 500; font-size: 14px; color: var(--text-secondary); }
.data-table td { font-size: 14px; color: var(--text-primary); }
.data-table tr:hover td { background: var(--bg-hover); }
.empty { text-align: center; color: var(--text-tertiary); }

.role-tag, .status-tag { padding: 4px 8px; border-radius: 4px; font-size: 12px; }
.role-tag.admin { background: var(--primary-500); color: #fff; }
.role-tag.user { background: var(--bg-tertiary); color: var(--text-secondary); }
.status-tag.success { background: var(--success-500); color: #fff; }
.status-tag.danger { background: var(--error-500); color: #fff; }

.btn-text-sm { padding: 4px 8px; background: transparent; border: 1px solid var(--border); border-radius: 4px; cursor: pointer; font-size: 13px; color: var(--text-primary); margin-right: 8px; }
.btn-text-sm:hover:not(:disabled) { background: var(--bg-hover); }
.btn-text-sm:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-text-sm.danger { color: var(--error-500); border-color: var(--error-500); }
.btn-text-sm.danger:hover:not(:disabled) { background: rgba(239, 68, 68, 0.1); }

.pagination { display: flex; justify-content: space-between; align-items: center; margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--border); }
.pagination-info { font-size: 14px; color: var(--text-secondary); }
.pagination-buttons { display: flex; align-items: center; gap: 12px; }
.btn-page { padding: 6px 12px; background: var(--bg-primary); border: 1px solid var(--border); border-radius: 4px; cursor: pointer; font-size: 14px; }
.btn-page:hover:not(:disabled) { background: var(--bg-hover); }
.btn-page:disabled { opacity: 0.5; cursor: not-allowed; }
.page-num { font-size: 14px; color: var(--text-secondary); }

.dark .user-manage { background: var(--dark-bg-primary); }
.dark .action-bar h2 { color: var(--dark-text-primary); }
.dark .search-input { background: var(--dark-bg-primary); border-color: var(--dark-border); color: var(--dark-text-primary); }
.dark .data-table th { background: var(--dark-bg-secondary); color: var(--dark-text-secondary); }
.dark .data-table td { color: var(--dark-text-primary); }
.dark .data-table tr:hover td { background: var(--dark-bg-hover); }
.dark .empty { color: var(--dark-text-tertiary); }
.dark .btn-text-sm { border-color: var(--dark-border); color: var(--dark-text-primary); }
.dark .btn-page { background: var(--dark-bg-primary); border-color: var(--dark-border); color: var(--dark-text-primary); }
.dark .pagination { border-color: var(--dark-border); }
.dark .pagination-info, .dark .page-num { color: var(--dark-text-secondary); }
</style>
