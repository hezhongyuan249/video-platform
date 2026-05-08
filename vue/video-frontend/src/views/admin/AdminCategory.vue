<template>
  <div class="admin-category">
    <div class="action-bar">
      <h2>分类管理</h2>
      <div class="action-buttons">
        <button class="btn-primary-sm" @click="openAddDialog" :disabled="isDragging">
          新增分类
        </button>
        <button class="btn-secondary-sm" @click="toggleDragMode" :disabled="categoryList.length < 2">
          {{ isDragging ? '取消拖拽' : '调整顺序' }}
        </button>
        <button v-if="isDragging" class="btn-success-sm" @click="saveDragOrder">
          完成
        </button>
      </div>
    </div>

    <div class="category-list">
      <div v-if="isDragging" class="drag-list">
        <div class="drag-container">
          <div 
            v-for="(item, index) in categoryList" 
            :key="item.id" 
            class="drag-item"
            :class="{ 'dragging': dragIndex === index }"
            :data-index="index"
            draggable="true"
            @dragstart="handleDragStart($event, index)"
            @dragover.prevent="handleDragOver($event, index)"
            @drop="handleDrop($event, index)"
            @dragend="handleDragEnd"
          >
            <div class="drag-handle">⋮⋮</div>
            <div class="drag-content">
              <div class="drag-item-name">{{ item.name }}</div>
              <div class="drag-item-value">{{ item.value }}</div>
            </div>
          </div>
        </div>
      </div>
      
      <table v-else class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>分类名称</th>
            <th>分类值</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in categoryList" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.name }}</td>
            <td>{{ item.value }}</td>
            <td>{{ formatDate(item.createTime) }}</td>
            <td>
              <button class="btn-text-sm" @click="openEditDialog(item)">编辑</button>
              <button class="btn-text-sm danger" @click="deleteCategory(item.id)">删除</button>
            </td>
          </tr>
          <tr v-if="categoryList.length === 0">
            <td colspan="5" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="addDialogVisible" class="dialog-overlay" @click.self="addDialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3>新增分类</h3>
          <button class="dialog-close" @click="addDialogVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>分类名称</label>
            <input v-model="addForm.name" class="form-input" placeholder="请输入分类名称" />
          </div>
          <div class="form-group">
            <label>分类值</label>
            <input v-model="addForm.value" class="form-input" placeholder="请输入分类值" />
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-secondary-sm" @click="addDialogVisible = false">取消</button>
          <button class="btn-primary-sm" @click="addCategory">确认添加</button>
        </div>
      </div>
    </div>

    <div v-if="editDialogVisible" class="dialog-overlay" @click.self="editDialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3>编辑分类</h3>
          <button class="dialog-close" @click="editDialogVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>分类名称</label>
            <input v-model="editForm.name" class="form-input" placeholder="请输入分类名称" />
          </div>
          <div class="form-group">
            <label>分类值</label>
            <input v-model="editForm.value" class="form-input" placeholder="请输入分类值" />
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-secondary-sm" @click="editDialogVisible = false">取消</button>
          <button class="btn-primary-sm" @click="updateCategory">确认修改</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const categoryList = ref([])
const isDragging = ref(false)
const dragIndex = ref(-1)
const dragOverIndex = ref(-1)

const handleDragStart = function(e, index) {
  dragIndex.value = index;
  e.dataTransfer.effectAllowed = 'move';
  e.dataTransfer.setData('text/plain', index);
}

const handleDragOver = function(e, index) {
  e.preventDefault();
  dragOverIndex.value = index;
}

const handleDrop = function(e, index) {
  e.preventDefault();
  const fromIndex = dragIndex.value;
  const toIndex = index;
  
  if (fromIndex !== toIndex && fromIndex >= 0 && toIndex >= 0) {
    const item = categoryList.value[fromIndex];
    categoryList.value.splice(fromIndex, 1);
    categoryList.value.splice(toIndex, 0, item);
  }
  
  dragIndex.value = -1;
  dragOverIndex.value = -1;
}

const handleDragEnd = function() {
  dragIndex.value = -1;
  dragOverIndex.value = -1;
}

const loadCategories = async function() {
  try {
    const res = await fetch('/api/category/list')
    const result = await res.json()
    if (result.code === 200) {
      categoryList.value = result.data || []
    }
  } catch (e) {
    console.error('获取分类失败', e)
  }
}

const toggleDragMode = function() {
  isDragging.value = !isDragging.value
}

const saveDragOrder = async function() {
  try {
    const res = await fetch('/api/category/reorder', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(categoryList.value),
      credentials: 'include'
    })
    const result = await res.json()
    if (result.code === 200) {
      ElMessage.success('分类顺序调整成功')
    } else {
      ElMessage.error(result.msg || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  }
  isDragging.value = false
}

const addDialogVisible = ref(false)
const addForm = ref({ name: '', value: '' })

const editDialogVisible = ref(false)
const editForm = ref({ id: '', name: '', value: '' })

const formatDate = function(date) {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleString('zh-CN')
}

const openAddDialog = function() {
  addForm.value = { name: '', value: '' }
  addDialogVisible.value = true
}

const openEditDialog = function(category) {
  editForm.value = { ...category }
  editDialogVisible.value = true
}

const addCategory = async function() {
  if (!addForm.value.name || !addForm.value.value) {
    ElMessage.warning('请填写完整的分类信息')
    return
  }

  try {
    const res = await fetch('/api/category/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: addForm.value.name,
        value: addForm.value.value
      }),
      credentials: 'include'
    })
    const result = await res.json()
    if (result.code === 200) {
      ElMessage.success('分类添加成功')
      loadCategories()
      addDialogVisible.value = false
    } else {
      ElMessage.error(result.msg || '添加失败')
    }
  } catch (e) {
    ElMessage.error('添加失败')
  }
}

const updateCategory = async function() {
  if (!editForm.value.name || !editForm.value.value) {
    ElMessage.warning('请填写完整的分类信息')
    return
  }

  try {
    const res = await fetch('/api/category/update', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: editForm.value.id,
        name: editForm.value.name,
        value: editForm.value.value
      }),
      credentials: 'include'
    })
    const result = await res.json()
    if (result.code === 200) {
      ElMessage.success('分类更新成功')
      loadCategories()
      editDialogVisible.value = false
    } else {
      ElMessage.error(result.msg || '更新失败')
    }
  } catch (e) {
    ElMessage.error('更新失败')
  }
}

const deleteCategory = async function(id) {
  if (!confirm('确定要删除这个分类吗？')) return
  try {
    const res = await fetch('/api/category/delete/' + id, {
      credentials: 'include'
    })
    const result = await res.json()
    if (result.code === 200) {
      ElMessage.success('分类删除成功')
      loadCategories()
    } else {
      ElMessage.error(result.msg || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(function() {
  loadCategories()
})
</script>

<style scoped>
.admin-category {
  padding: 24px;
  background: var(--bg-primary);
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.action-bar h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.btn-primary-sm {
  padding: 8px 16px;
  background: var(--primary-500);
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary-sm:hover {
  background: var(--primary-700);
}

.btn-primary-sm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary-sm {
  padding: 8px 16px;
  background: transparent;
  color: var(--text-primary);
  border: 1px solid var(--border);
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-secondary-sm:hover {
  background: var(--bg-hover);
}

.btn-secondary-sm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-success-sm {
  padding: 8px 16px;
  background: var(--success-500);
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-success-sm:hover {
  background: var(--success-600);
}

.category-list {
  min-height: 400px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid var(--border);
}

.data-table th {
  background: var(--bg-secondary);
  font-weight: 500;
  font-size: 14px;
  color: var(--text-secondary);
}

.data-table td {
  font-size: 14px;
  color: var(--text-primary);
}

.data-table tr:hover td {
  background: var(--bg-hover);
}

.empty {
  text-align: center;
  color: var(--text-tertiary);
}

.btn-text-sm {
  padding: 4px 8px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-primary);
  margin-right: 8px;
}

.btn-text-sm:hover {
  background: var(--bg-hover);
}

.btn-text-sm.danger {
  color: var(--error-500);
  border-color: var(--error-500);
}

.btn-text-sm.danger:hover {
  background: rgba(239, 68, 68, 0.1);
}

.drag-list {
  margin: 20px 0;
}

.drag-container {
  border: 1px solid var(--border);
  border-radius: 4px;
  padding: 10px;
  background: var(--bg-secondary);
}

.drag-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 8px;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 4px;
  cursor: move;
  transition: all 0.2s ease;
}

.drag-item:hover {
  border-color: var(--primary-500);
}

.drag-item:last-child {
  margin-bottom: 0;
}

.drag-handle {
  margin-right: 16px;
  color: var(--text-tertiary);
  cursor: move;
  font-size: 18px;
}

.drag-content {
  flex: 1;
}

.drag-item-name {
  font-weight: 500;
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.drag-item-value {
  font-size: 12px;
  color: var(--text-tertiary);
}

.dialog-overlay {
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

.dialog {
  background: var(--bg-primary);
  border-radius: 8px;
  width: 400px;
  max-width: 90%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
}

.dialog-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.dialog-close {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--text-tertiary);
  cursor: pointer;
  line-height: 1;
}

.dialog-close:hover {
  color: var(--text-primary);
}

.dialog-body {
  padding: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid var(--border);
}

.form-group {
  margin-bottom: 16px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  font-size: 14px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background: var(--bg-primary);
  color: var(--text-primary);
}

.form-input:focus {
  outline: none;
  border-color: var(--primary-900);
}

.form-input::placeholder {
  color: var(--text-tertiary);
}

.drag-item {
  user-select: none;
  cursor: move;
}

.drag-item.dragging {
  opacity: 0.5;
  background: var(--primary-100);
}
</style>
