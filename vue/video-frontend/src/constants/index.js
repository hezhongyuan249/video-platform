// 全局常量抽离
export const REVIEW_STATUS = {
  PENDING: 0,
  PASS: 1,
  REJECT: 2,
  TEXT: ['待审核', '审核通过', '审核驳回'],
  TAG_TYPE: ['warning', 'success', 'danger']
}

export const VIDEO_STATUS = {
  NORMAL: 0,
  OFFLINE: 1,
  TEXT: ['正常', '下架'],
  TAG_TYPE: ['success', 'danger']
}

export const USER_STATUS = {
  NORMAL: 0,
  BANNED: 1,
  TEXT: ['正常', '封禁'],
  TAG_TYPE: ['success', 'danger']
}

export const USER_ROLE = {
  ADMIN: 'admin',
  USER: 'user',
  TEXT: {
    admin: '管理员',
    user: '普通用户'
  },
  TAG_TYPE: {
    admin: 'primary',
    user: 'success'
  }
}

// 接口基础URL（从环境变量读取，兜底本地）
export const BASE_URL = import.meta.env.VITE_BASE_URL || 'http://8.134.36.54:8088'

// 密码强度相关
export const PWD_STRENGTH = {
  WEAK: { text: '弱', class: 'weak' },
  MEDIUM: { text: '中', class: 'medium' },
  STRONG: { text: '强', class: 'strong' }
}