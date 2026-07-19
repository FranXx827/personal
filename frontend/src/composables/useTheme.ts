import { computed } from 'vue'
import { useThemeStore, type ThemeMode } from '@/stores/theme'

export function useTheme() {
  const store = useThemeStore()
  return {
    mode: computed(() => store.mode),
    resolved: computed(() => store.resolved),
    isDark: computed(() => store.resolved === 'dark'),
    setMode: (m: ThemeMode) => store.setMode(m),
    toggle: () => store.toggle(),
  }
}
