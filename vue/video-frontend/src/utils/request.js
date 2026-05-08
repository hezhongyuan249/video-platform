import axios from 'axios'
import { getUserInfo } from './userStorage'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use(
  config => {
    const user = getUserInfo()
    if (user && user.token) {
      config.headers['Authorization'] = 'Bearer ' + user.token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  res => res.data,
  error => Promise.reject(error)
)

export default request
