import { ref, onMounted, onUnmounted } from 'vue'

export function useCategories() {
  // 分类数据
  const categories = ref([
    { id: 'all', name: '全部' }
  ])

  // 视频类型数据
  const videoTypes = ref([])

  // 从后端API加载分类数据
  const loadCategories = async () => {
    try {
      const res = await fetch('/api/category/list')
      const result = await res.json()
      if (result.code === 200 && result.data) {
        // 添加'全部'分类在第一位，使用id字段作为id
        categories.value = [
          { id: 'all', name: '全部' },
          ...result.data.map(cat => ({
            id: String(cat.id),  // 转换为字符串，因为 videoType 是字符串
            name: cat.name
          }))
        ]
        // 同时保存到localStorage作为缓存
        localStorage.setItem('videoCategories', JSON.stringify(result.data))
      }
    } catch (e) {
      // 如果API失败，尝试从localStorage加载
      const savedCategories = localStorage.getItem('videoCategories')
      if (savedCategories) {
        const parsedCategories = JSON.parse(savedCategories)
        categories.value = [
          { id: 'all', name: '全部' },
          ...parsedCategories.map(cat => ({
            id: String(cat.id),  // 转换为字符串，因为 videoType 是字符串
            name: cat.name
          }))
        ]
      }
    }
  }

  // 保存分类数据到localStorage（仅作为缓存）
  const saveCategories = (categoryList) => {
    localStorage.setItem('videoCategories', JSON.stringify(categoryList))
  }

  // 同步视频类型数据
  const syncVideoTypes = () => {
    videoTypes.value = categories.value
      .filter(cat => cat.id !== 'all')
      .map(cat => ({ value: cat.id, label: cat.name }))
  }

  // 监听分类数据变化
  const handleStorageChange = (event) => {
    if (event.key === 'videoCategories') {
      loadCategories()
      syncVideoTypes()
    }
  }

  // 初始化分类数据
  onMounted(() => {
    loadCategories()
    syncVideoTypes()
    // 添加storage事件监听器
    window.addEventListener('storage', handleStorageChange)
  })

  onUnmounted(() => {
    // 移除storage事件监听器
    window.removeEventListener('storage', handleStorageChange)
  })

  return {
    categories,
    videoTypes,
    loadCategories,
    saveCategories,
    syncVideoTypes
  }
}
