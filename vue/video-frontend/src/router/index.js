import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/auth/Login.vue'
import Register from '../views/auth/Register.vue'
import FindPwd from '../views/auth/FindPwd.vue'
import Index from '../views/main/Index.vue'
import VideoPlay from '../views/main/VideoPlay.vue'
import WatchHistory from '../views/main/WatchHistory.vue'
import AdminIndex from '../views/admin/AdminIndex.vue'
import UserManage from '../views/admin/UserManage.vue'
import AdminVideo from '../views/admin/AdminVideo.vue'
import AdminVideoReview from '../views/admin/AdminVideoReview.vue'
import AdminVideoStat from '../views/admin/AdminVideoStat.vue'
import AdminCategory from '../views/admin/AdminCategory.vue'
import MyVideos from '../views/user/MyVideos.vue'
import MyFavorites from '../views/user/MyFavorites.vue'
import MyFollowing from '../views/user/MyFollowing.vue'
import MyProfile from '../views/user/MyProfile.vue'
import UploadVideo from '../views/upload/UploadVideo.vue'
import UserSettings from '../views/user/UserSettings.vue'
import Notifications from '../views/user/Notifications.vue'
import TranscodeProgressPage from '../views/main/TranscodeProgress.vue'
import { ElMessage } from 'element-plus'
import { getUserInfo, setUserInfo } from '../utils/userStorage'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: Login },
    { path: '/register', component: Register },
    { path: '/find-pwd', component: FindPwd },
    { path: '/index', component: Index, meta: { requiresAuth: true } },
    { path: '/my-videos', component: MyVideos, meta: { requiresAuth: true } },
    { path: '/my-favorites', component: MyFavorites, meta: { requiresAuth: true } },
    { path: '/watch-history', component: WatchHistory, meta: { requiresAuth: true } },
    { path: '/my-following', component: MyFollowing, meta: { requiresAuth: true } },
    { path: '/upload-video', component: UploadVideo, meta: { requiresAuth: true } },
    { path: '/my-profile', component: MyProfile, meta: { requiresAuth: true } },
    { path: '/user/:userId', component: MyProfile },
    { path: '/user-settings', component: UserSettings, meta: { requiresAuth: true } },
    { path: '/video-play/:id', component: VideoPlay },
    { path: '/transcode/:videoId', component: TranscodeProgressPage, meta: { requiresAuth: true } },
    { path: '/notifications', component: Notifications, meta: { requiresAuth: true } },
    { 
      path: '/admin', 
      component: AdminIndex, 
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: 'user', component: UserManage },
        { path: 'category', component: AdminCategory },
        { path: 'video/list', component: AdminVideo },
        { path: 'video/review', component: AdminVideoReview },
        { path: 'video/stat', component: AdminVideoStat }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/login' }
  ]
})

router.beforeEach(async (to, from, next) => {
  const userInfo2 = getUserInfo() || {}
  
  // 如果有 token，验证并刷新用户信息
  if (userInfo2.token) {
    try {
      const res = await fetch('/api/user/current', {
        headers: {
          'Authorization': 'Bearer ' + userInfo2.token
        }
      })
      const data = await res.json()
      if (data.code === 200 && data.data) {
        data.data.token = userInfo2.token
        setUserInfo(data.data)
      }
    } catch (e) {}
  }
  
  const userInfo = getUserInfo() || {}

  if (to.meta.requiresAuth && !userInfo.id) {
    ElMessage.warning('请先登录！')
    next('/login')
    return
  }

  if (to.meta.requiresAdmin && userInfo.role !== 'admin') {
    ElMessage.warning('无管理员权限，无法访问！')
    next('/index')
    return
  }

  if (userInfo.status === 1) {
    const allowPaths = ['/login', '/user-settings']
    if (!allowPaths.includes(to.path) && !to.path.startsWith('/admin')) {
      ElMessage.warning('账号已被封禁，仅可修改个人信息！')
      next('/index')
      return
    }
  }

  if (userInfo.delFlag === 1) {
    const allowPaths = ['/login', '/user-settings']
    if (!allowPaths.includes(to.path)) {
      ElMessage.warning('账号处于注销冷静期，仅可修改个人信息！')
      next('/index')
      return
    }
  }

  next()
})

export default router
