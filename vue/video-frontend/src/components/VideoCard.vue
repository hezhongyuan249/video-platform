<template>
  <div
    class="video-card"
    @click="$emit('click', video.id)"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <div class="video-cover">
      <img
        v-show="!isPlaying"
        :src="video.coverUrl"
        :alt="video.title"
        class="cover-image"
      />
      <video
        ref="videoRef"
        class="video-player"
        :src="video.videoUrl"
        muted
        loop
        playsinline
        preload="metadata"
        @loadedmetadata="onLoadedMetadata"
        @timeupdate="onTimeUpdate"
        @ended="onEnded"
      ></video>

      <div class="video-controls" :class="{ visible: isPlaying }">
        <button
          class="control-btn play-btn"
          @click.stop="togglePlay"
        >
          <svg v-if="isPaused" viewBox="0 0 24 24" fill="currentColor">
            <path d="M8 5v14l11-7z"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="currentColor">
            <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>
          </svg>
        </button>

        <div class="progress-wrapper" @click.stop="seekTo">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            <div
              class="progress-handle"
              :style="{ left: progressPercent + '%' }"
              @mousedown.stop="startDrag"
            ></div>
          </div>
        </div>

        <button
          class="control-btn mute-btn"
          @click.stop="toggleMute"
        >
          <svg v-if="isMuted" viewBox="0 0 24 24" fill="currentColor">
            <path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="currentColor">
            <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/>
          </svg>
        </button>
      </div>

      <span v-if="!isPlaying" class="duration">{{ formatDuration(video.duration) }}</span>
      
      <div v-if="video.progress && video.progress > 0 && video.duration && video.duration > 0" class="history-progress">
        <div class="history-progress-bar" :style="{ width: ((video.progress / video.duration) * 100) + '%' }"></div>
        <span class="history-progress-text">{{ formatDuration(video.progress) }} / {{ formatDuration(video.duration) }}</span>
      </div>
    </div>

    <div class="video-meta">
      <h4 class="video-title">{{ video.title }}</h4>
      <div class="video-info">
        <slot name="info">
          <span>{{ video.username }}</span>
          <span>{{ formatViews(video.views || 0) }}播放</span>
          <span class="video-stats">
            <span class="stat-item">
              <svg viewBox="0 0 24 24" fill="currentColor"><path d="M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14.17 1 7.59 7.59C7.22 7.95 7 8.45 7 9v10c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/></svg>
              {{ formatCount(video.likes || 0) }}
            </span>
            <span class="stat-item">
              <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
              {{ formatCount(video.favorites || 0) }}
            </span>
          </span>
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { formatDuration, formatViews, formatCount } from '../utils/format'

const props = defineProps({
  video: {
    type: Object,
    required: true
  },
  disablePreview: {
    type: Boolean,
    default: false
  }
})

defineEmits(['click'])

const videoRef = ref(null)
const isPlaying = ref(false)
const isPaused = ref(false)
const isMuted = ref(true)
const currentTime = ref(0)
const duration = ref(0)
const isDragging = ref(false)

const progressPercent = computed(() => {
  if (duration.value === 0) return 0
  return (currentTime.value / duration.value) * 100
})

const STORAGE_KEY_PREFIX = 'video_progress_'

const getStorageKey = () => {
  return STORAGE_KEY_PREFIX + props.video.id
}

const formatTime = (seconds) => {
  if (!seconds || isNaN(seconds)) return '00:00'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

const saveProgressToBackend = async () => {
  if (!props.video.id || currentTime.value === 0) return
  localStorage.setItem(getStorageKey(), currentTime.value.toString())
}

const loadProgressFromBackend = async () => {
  return parseFloat(localStorage.getItem(getStorageKey()) || '0')
}

const handleMouseEnter = async () => {
  if (props.disablePreview) return
  
  if (videoRef.value) {
    isPlaying.value = true
    isPaused.value = false

    const savedProgress = await loadProgressFromBackend()
    if (savedProgress > 0 && savedProgress < duration.value - 1) {
      videoRef.value.currentTime = savedProgress
      currentTime.value = savedProgress
    }

    videoRef.value.play().catch(() => {
      isPlaying.value = false
    })
  }
}

const handleMouseLeave = () => {
  if (videoRef.value && !isDragging.value) {
    videoRef.value.pause()
    isPlaying.value = false
    isPaused.value = false
    saveProgressToBackend()
  }
}

const togglePlay = () => {
  if (!videoRef.value) return
  if (isPaused.value) {
    videoRef.value.play()
    isPaused.value = false
  } else {
    videoRef.value.pause()
    isPaused.value = true
  }
}

const toggleMute = () => {
  if (!videoRef.value) return
  isMuted.value = !isMuted.value
  videoRef.value.muted = isMuted.value
}

const seekTo = (event) => {
  if (!videoRef.value) return
  const rect = event.currentTarget.getBoundingClientRect()
  const percent = (event.clientX - rect.left) / rect.width
  const newTime = percent * duration.value
  videoRef.value.currentTime = newTime
  currentTime.value = newTime
}

const startDrag = (event) => {
  isDragging.value = true
  const onMouseMove = (e) => {
    const rect = event.target.closest('.progress-bar').getBoundingClientRect()
    let percent = (e.clientX - rect.left) / rect.width
    percent = Math.max(0, Math.min(1, percent))
    const newTime = percent * duration.value
    if (videoRef.value) {
      videoRef.value.currentTime = newTime
      currentTime.value = newTime
    }
  }
  const onMouseUp = () => {
    isDragging.value = false
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

const onLoadedMetadata = () => {
  duration.value = videoRef.value.duration
}

const onTimeUpdate = () => {
  if (!videoRef.value) return
  if (!isDragging.value) {
    currentTime.value = videoRef.value.currentTime
  }
}

const onEnded = () => {
  currentTime.value = 0
  localStorage.removeItem(getStorageKey())
  videoRef.value.play()
}
</script>

<style scoped>
.video-card {
  cursor: pointer;
  transition: transform 0.2s ease;
}

.video-card:hover {
  transform: translateY(-4px);
}

.video-cover {
  position: relative;
  aspect-ratio: 16/9;
  background: var(--bg-tertiary, #1a1a1a);
  border-radius: 12px;
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: opacity 0.3s ease;
}

.video-player {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.video-cover:hover .video-player {
  opacity: 1;
}

.video-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.85));
  opacity: 0;
  transition: opacity 0.2s ease;
}

.video-controls.visible {
  opacity: 1;
}

.control-btn {
  background: none;
  border: none;
  color: #fff;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.15s ease;
}

.control-btn:hover {
  transform: scale(1.15);
}

.control-btn svg {
  width: 20px;
  height: 20px;
}

.play-btn {
  width: 28px;
  height: 28px;
}

.mute-btn {
  width: 24px;
  height: 24px;
}

.progress-wrapper {
  flex: 1;
  padding: 8px 0;
  cursor: pointer;
}

.progress-bar {
  position: relative;
  height: 3px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
  overflow: visible;
}

.progress-fill {
  height: 100%;
  background: #ff4757;
  border-radius: 2px;
  transition: width 0.1s linear;
}

.progress-handle {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%) scale(0);
  width: 12px;
  height: 12px;
  background: #ff4757;
  border-radius: 50%;
  transition: transform 0.15s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.progress-wrapper:hover .progress-handle {
  transform: translate(-50%, -50%) scale(1);
}

.progress-wrapper:hover .progress-bar {
  height: 4px;
}

.duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
}

.history-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 24px;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px;
}

.history-progress-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 3px;
  background: #ff6b6b;
}

.history-progress-text {
  position: absolute;
  bottom: 4px;
  right: 8px;
  color: #fff;
  font-size: 11px;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.video-meta {
  padding: 12px 4px;
}

.video-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary, #fff);
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.video-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--text-secondary, #999);
  flex-wrap: wrap;
}

.video-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-item svg {
  width: 14px;
  height: 14px;
}
</style>
