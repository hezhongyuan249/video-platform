<template>
  <div class="admin-video-stat">
    <div class="stat-header">
      <h2>视频数据统计</h2>
    </div>

    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon total">📹</div>
        <div class="stat-content">
          <p class="stat-label">总视频数</p>
          <h3 class="stat-value">{{ statData.totalCount }}</h3>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon today">📤</div>
        <div class="stat-content">
          <p class="stat-label">今日上传</p>
          <h3 class="stat-value">{{ statData.todayCount }}</h3>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon normal">✓</div>
        <div class="stat-content">
          <p class="stat-label">正常视频</p>
          <h3 class="stat-value">{{ statData.normalCount }}</h3>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon offline">✕</div>
        <div class="stat-content">
          <p class="stat-label">下架视频</p>
          <h3 class="stat-value">{{ statData.offlineCount }}</h3>
        </div>
      </div>
    </div>

    <div class="user-rank-card">
      <h3>用户上传排行</h3>
      <table class="data-table">
        <thead>
          <tr>
            <th>排名</th>
            <th>用户名</th>
            <th>上传数量</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in userRankList" :key="item.rank">
            <td>
              <span class="rank-tag" :class="getRankClass(item.rank)">{{ item.rank }}</span>
            </td>
            <td>{{ item.username }}</td>
            <td>{{ item.videoCount }}</td>
          </tr>
          <tr v-if="userRankList.length === 0">
            <td colspan="3" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const statData = ref({
  totalCount: 0,
  todayCount: 0,
  normalCount: 0,
  offlineCount: 0
})
const userRankList = ref([])

const getRankClass = function(rank) {
  if (rank === 1) return 'gold'
  if (rank === 2) return 'silver'
  if (rank === 3) return 'bronze'
  return ''
}

const getStatData = async function() {
  try {
    const res = await fetch('/api/video/admin/stat', {
      credentials: 'include'
    })
    const result = await res.json()
    if (result.code === 200) {
      statData.value = result.data.stat || { totalCount: 0, todayCount: 0, normalCount: 0, offlineCount: 0 }
      userRankList.value = (result.data.userRank || []).map(function(item, index) {
        return {
          rank: index + 1,
          username: item.username,
          videoCount: item.videoCount
        }
      })
    }
  } catch (e) {
    ElMessage.error('获取统计数据失败')
    console.error('getStatData error:', e)
  }
}

onMounted(function() {
  getStatData()
})
</script>

<style scoped>
.admin-video-stat {
  padding: 24px;
  background: var(--bg-primary);
}

.stat-header {
  margin-bottom: 24px;
}

.stat-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

@media (max-width: 1024px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .stat-cards {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 8px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 16px;
  border-radius: 8px;
}

.stat-icon.total {
  background: rgba(23, 23, 23, 0.1);
}

.stat-icon.today {
  background: rgba(34, 197, 94, 0.1);
}

.stat-icon.normal {
  background: rgba(34, 197, 94, 0.1);
}

.stat-icon.offline {
  background: rgba(239, 68, 68, 0.1);
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 4px 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.user-rank-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 20px;
}

.user-rank-card h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px 0;
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
  font-weight: 500;
  font-size: 14px;
  color: var(--text-secondary);
}

.data-table td {
  font-size: 14px;
  color: var(--text-primary);
}

.data-table tr:last-child td {
  border-bottom: none;
}

.data-table tr:hover td {
  background: var(--bg-hover);
}

.empty {
  text-align: center;
  color: var(--text-tertiary);
}

.rank-tag {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
}

.rank-tag.gold {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: #fff;
}

.rank-tag.silver {
  background: linear-gradient(135deg, #e5e7eb, #9ca3af);
  color: #fff;
}

.rank-tag.bronze {
  background: linear-gradient(135deg, #d97706, #b45309);
  color: #fff;
}
</style>
