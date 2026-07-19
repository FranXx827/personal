import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type UserInfo, type UserType } from '@/api/modules/auth'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref<string>('')
  const profile = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!accessToken.value)
  const roles = computed(() => profile.value?.roles ?? [])
  const role = computed<UserType | ''>(() => (profile.value?.roles?.[0] as UserType) ?? '')

  function roleHome(): string {
    return role.value === 'MERCHANT' ? '/merchant/dashboard' : '/'
  }

  async function login(data: Parameters<typeof authApi.login>[0]): Promise<UserType | ''> {
    const token = await authApi.login(data)
    accessToken.value = token.accessToken
    if (token.refreshToken) {
      localStorage.setItem('app:refreshToken', token.refreshToken)
    }
    await fetchProfile()
    return role.value
  }

  async function fetchProfile() {
    profile.value = await authApi.getProfile()
  }

  async function refresh(): Promise<string | null> {
    try {
      const refreshToken = localStorage.getItem('app:refreshToken')
      if (!refreshToken) {
        return null
      }
      const token = await authApi.refresh(refreshToken)
      accessToken.value = token.accessToken
      if (token.refreshToken) {
        localStorage.setItem('app:refreshToken', token.refreshToken)
      }
      return token.accessToken
    } catch {
      localStorage.removeItem('app:refreshToken')
      return null
    }
  }

  function logout() {
    accessToken.value = ''
    profile.value = null
    localStorage.removeItem('app:refreshToken')
    authApi.logout().catch(() => { /* 静默 */ })
  }

  function hasRole(r: string) {
    return roles.value.includes(r)
  }

  return {
    accessToken,
    profile,
    isLoggedIn,
    roles,
    role,
    login,
    fetchProfile,
    refresh,
    logout,
    hasRole,
    roleHome,
  }
})
