<template>
  <div class="watch-history-page">
    <NavBar />
    
    <div class="history-header">
      <h2>观看历史</h2>
      <div class="header-actions">
        <button class="btn-text" @click="goHome">返回首页</button>
        <button v-if="!isSelectionMode && list.length > 0" class="btn-text" @click="isSelectionMode = true">选择</button>
        <template v-if="isSelectionMode">
          <button class="btn-text" @click="selectAll">全选</button>
          <button class="btn-text" @click="cancelSelection">取消</button>
          <button class="btn-text" :disabled="selectedIds.length === 0" @click="deleteSelected">删除选中 ({{ selectedIds.length }})</button>
        </template>
        <button v-if="!isSelectionMode && list.length > 0" class="btn-text" @click="clearAll">清空记录</button>
      </div>
    </div>

    <div class="video-grid" v-if="list.length > 0">
      <div 
        v-for="item in list" 
        :key="item.id" 
        class="video-item"
        :class="{ selected: selectedIds.includes(item.id) }"
        @click="handleItemClick(item)"
      >
        <div class="checkbox" v-if="isSelectionMode" @click.stop="toggleSelect(item.id)">
          <span v-if="selectedIds.includes(item.id)">✓</span>
        </div>
        <div class="delete-btn" @click.stop="deleteOne(item.id)">×</div>
        <VideoCard
          :video="item"
          :disable-preview="true"
          @click="playVideo(item.id)"
        >
          <template #info>
            <span>{{ formatTime(item.watchedAt) }}</span>
          </template>
        </VideoCard>
      </div>
    </div>
    
    <div class="empty" v-else>
      <p>暂无观看记录</p>
      <button class="btn-primary-sm" @click="goHome">去首页看看</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import VideoCard from '../../components/VideoCard.vue'
import NavBar from '../../components/NavBar.vue'

const router = useRouter()
const list = ref([])
const isSelectionMode = ref(false)
const selectedIds = ref([])

const getUserInfo = () => {
  const info = sessionStorage.getItem('userInfo')
  return info ? JSON.parse(info) : null
}

const user = ref(getUserInfo() || {})

const getList = async () => {
  if (!user.value.id) return
  try {
    const res = await request.get('/video/history/list', {
      params: { userId: user.value.id }
    })
    if (res.code === 200 && res.data) {
    list.value = res.data
    console.log('观看历史数据:', res.data)
  }
  } catch (e) {
    console.log('获取观看历史失败', e)
  }
}

const playVideo = (videoId) => {
  router.push(`/video-play/${videoId}`)
}

const handleItemClick = (item) => {
  if (isSelectionMode.value) {
    toggleSelect(item.id)
  } else {
    playVideo(item.id)
  }
}

const toggleSelect = (id) => {
  const index = selectedIds.value.indexOf(id)
  if (index === -1) {
    selectedIds.value.push(id)
  } else {
    selectedIds.value.splice(index, 1)
  }
}

const cancelSelection = () => {
  isSelectionMode.value = false
  selectedIds.value = []
}

const selectAll = () => {
  selectedIds.value = list.value.map(item => item.id)
}

const deleteSelected = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条记录吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    console.log('删除选中的videoIds:', selectedIds.value)
    const res = await fetch('/api/video/history/deleteBatch', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: user.value.id,
        videoIds: selectedIds.value
      }),
      credentials: 'include'
    })
    const result = await res.json()
    console.log('删除结果:', result)
    if (result.code === 200) {
      ElMessage.success('删除成功')
      cancelSelection()
      getList()
    } else {
      ElMessage.error(result.msg || '删除失败')
    }
  } catch (e) {
    console.error('删除失败:', e)
    ElMessage.error('删除失败')
  }
}

const deleteOne = async (videoId) => {
  try {
    await ElMessageBox.confirm('确定删除这条观看记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.post('/video/history/delete', {
      userId: user.value.id,
      videoId: videoId
    })
    ElMessage.success('删除成功')
    getList()
  } catch (e) {}
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return date.toLocaleDateString()
}

const clearAll = async () => {
  try {
    await ElMessageBox.confirm('确定清空所有观看记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.post('/video/history/clear', null, {
      params: { userId: user.value.id }
    })
    list.value = []
    ElMessage.success('已清空')
  } catch (e) {}
}

const goHome = () => {
  router.push('/index')
}

onMounted(() => getList())
</script>

<style scoped>
.watch-history-page {
  min-height: 100vh;
  background: var(--bg-primary);
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: var(--bg-secondary);
  border-radius: 12px;
  margin: 0 24px 24px 24px;
  max-width: 1552px;
}

.history-header h2 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
}

.header-actions {
  display: flex;
  gap: 12px;
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

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
  padding: 0 24px;
  max-width: 1600px;
  margin: 0 auto;
}

.video-item {
  position: relative;
}

.video-item .checkbox {
  position: absolute;
  top: 8px;
  left: 8px;
  width: 24px;
  height: 24px;
  background: rgba(0, 0, 0, 0.5);
  border: 2px solid #fff;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  z-index: 10;
}

.video-item.selected .checkbox {
  background: #409eff;
  border-color: #409eff;
}

.video-item .delete-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  line-height: 0;
  cursor: pointer;
  z-index: 10;
  opacity: 0;
  transition: opacity 0.2s;
}

.video-item:hover .delete-btn {
  opacity: 1;
}

.empty {
  text-align: center;
  padding: 80px 0;
}

.empty p {
  color: var(--text-tertiary);
  margin-bottom: 20px;
}

.btn-primary-sm {
  padding: 10px 24px;
  background: var(--primary-500);
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
</style>
