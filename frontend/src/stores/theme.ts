/**
 * 主题切换 (light / dark / system)
 * - 持久化到 localStorage
 * - 监听 system 偏好变化
 */
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'app:theme'

export const useThemeStore = defineStore(
  'theme',
  () => {
    const mode = ref<ThemeMode>((localStorage.getItem(STORAGE_KEY) as ThemeMode) || 'system')
    const resolved = ref<'light' | 'dark'>('light')

    function getSystemTheme(): 'light' | 'dark' {
      return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
    }

    function apply() {
      const actual = mode.value === 'system' ? getSystemTheme() : mode.value
      resolved.value = actual
      document.documentElement.setAttribute('data-theme', actual)
      document.documentElement.classList.toggle('dark', actual === 'dark')
    }

    function init() {
      apply()
      const mql = window.matchMedia('(prefers-color-scheme: dark)')
      mql.addEventListener('change', () => {
        if (mode.value === 'system') apply()
      })
    }

    function setMode(m: ThemeMode) {
      mode.value = m
    }

    function toggle() {
      const next = resolved.value === 'dark' ? 'light' : 'dark'
      setMode(next)
    }

    watch(mode, () => {
      apply()
    })

    return { mode, resolved, init, setMode, toggle }
  },
  {
    persist: {
      key: STORAGE_KEY,
      pick: ['mode'],
    },
  },
)
