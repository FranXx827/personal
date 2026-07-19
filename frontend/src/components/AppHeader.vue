<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { useTheme } from '@/composables/useTheme'
import JIcon from '@/components/JIcon.vue'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const { isDark, toggle } = useTheme()

const isMerchant = computed(() => userStore.role === 'MERCHANT')
const searchKeyword = ref('')
const mobileMenuOpen = ref(false)
const isMobile = ref(window.innerWidth < 768)

function onResize() {
  isMobile.value = window.innerWidth < 768
  if (!isMobile.value) mobileMenuOpen.value = false
}

window.addEventListener('resize', onResize)
onUnmounted(() => window.removeEventListener('resize', onResize))

function goSearch() {
  if (!searchKeyword.value.trim()) return
  router.push({ name: 'Search', query: { keyword: searchKeyword.value } })
  mobileMenuOpen.value = false
}

function navigate(path: string) {
  router.push(path)
  mobileMenuOpen.value = false
}

function logout() {
  userStore.logout()
  router.push({ name: 'Login' })
}
</script>

<template>
  <header class="app-header">
    <div class="header-inner">
      <div class="logo" @click="navigate('/')" role="button" tabindex="0" aria-label="返回首页" @keydown.enter="navigate('/')">
        <svg class="logo-mark" width="28" height="28" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect width="28" height="28" rx="7" fill="var(--color-primary)" />
          <path d="M8 14L12 18L20 10" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <span class="logo-text">智能电商</span>
      </div>

      <div class="search-wrapper">
        <JIcon name="search" :size="16" />
        <input
          v-model="searchKeyword"
          type="text"
          class="search-input"
          placeholder="搜索商品…"
          @keyup.enter="goSearch"
          aria-label="搜索商品"
        />
      </div>

      <nav class="nav" aria-label="主导航">
        <button class="nav-btn" @click="navigate('/')">
          <JIcon name="home" :size="16" />
          <span>首页</span>
        </button>

        <button class="nav-btn" @click="navigate('/chat')">
          <JIcon name="chat" :size="16" />
          <span>AI 助手</span>
        </button>

        <template v-if="isMerchant">
          <button class="nav-btn" @click="navigate('/merchant/dashboard')">
            <JIcon name="dashboard" :size="16" />
            <span>商家后台</span>
          </button>
        </template>
        <template v-else>
          <button class="nav-btn cart-btn" @click="navigate('/cart')">
            <JIcon name="cart" :size="16" />
            <span>购物车</span>
            <span v-if="cartStore.totalQuantity > 0" class="cart-badge">{{ cartStore.totalQuantity > 99 ? '99+' : cartStore.totalQuantity }}</span>
          </button>
          <button class="nav-btn" @click="navigate('/orders')">
            <JIcon name="order" :size="16" />
            <span>我的订单</span>
          </button>
        </template>

        <el-dropdown trigger="click">
          <button class="nav-btn user-btn" tabindex="0">
            <JIcon name="user" :size="16" />
            <span class="user-name">{{ userStore.profile?.nickname || '未登录' }}</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="!userStore.isLoggedIn" @click="navigate('/login')">
                登录
              </el-dropdown-item>
              <template v-else>
                <el-dropdown-item v-if="isMerchant" @click="navigate('/merchant/dashboard')">
                  商家后台
                </el-dropdown-item>
                <el-dropdown-item v-else @click="navigate('/orders')">我的订单</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </template>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <button class="nav-btn theme-btn" @click="toggle" :title="isDark ? '切换浅色模式' : '切换深色模式'" aria-label="切换主题">
          <JIcon :name="isDark ? 'sun' : 'moon'" :size="16" />
        </button>
      </nav>

      <button class="hamburger" @click="mobileMenuOpen = !mobileMenuOpen" :aria-label="mobileMenuOpen ? '关闭菜单' : '打开菜单'" :aria-expanded="mobileMenuOpen">
        <JIcon :name="mobileMenuOpen ? 'close' : 'menu'" :size="20" />
      </button>
    </div>

    <Transition name="slide-down">
      <div v-if="mobileMenuOpen" class="mobile-panel" role="navigation" aria-label="移动端导航">
        <div class="mobile-search">
          <JIcon name="search" :size="16" class="mobile-search-icon" />
          <input v-model="searchKeyword" type="text" placeholder="搜索商品…" @keyup.enter="goSearch" aria-label="搜索商品" />
        </div>

        <div class="mobile-links">
          <button class="mobile-link" @click="navigate('/')">
            <JIcon name="home" :size="18" />
            <span>首页</span>
          </button>
          <button class="mobile-link" @click="navigate('/chat')">
            <JIcon name="chat" :size="18" />
            <span>AI 助手</span>
          </button>
          <button v-if="isMerchant" class="mobile-link" @click="navigate('/merchant/dashboard')">
            <JIcon name="dashboard" :size="18" />
            <span>商家后台</span>
          </button>
          <template v-else>
            <button class="mobile-link" @click="navigate('/cart')">
              <JIcon name="cart" :size="18" />
              <span>购物车</span>
              <span v-if="cartStore.totalQuantity > 0" class="mobile-badge">{{ cartStore.totalQuantity }}</span>
            </button>
            <button class="mobile-link" @click="navigate('/orders')">
              <JIcon name="order" :size="18" />
              <span>我的订单</span>
            </button>
          </template>
          <button v-if="!userStore.isLoggedIn" class="mobile-link" @click="navigate('/login')">
            <JIcon name="user" :size="18" />
            <span>登录</span>
          </button>
          <button v-else class="mobile-link" @click="logout">
            <JIcon name="user" :size="18" />
            <span>退出登录</span>
          </button>
          <button class="mobile-link mobile-theme" @click="toggle">
            <JIcon :name="isDark ? 'sun' : 'moon'" :size="18" />
            <span>{{ isDark ? '浅色模式' : '深色模式' }}</span>
          </button>
        </div>
      </div>
    </Transition>
  </header>
</template>

<style scoped lang="scss">
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--header-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--header-border);
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  height: 60px;
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: 0 var(--space-5);

  @media (min-width: 768px) {
    padding: 0 var(--space-6);
  }
}

/* ----- Logo ----- */
.logo {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  flex-shrink: 0;
  user-select: none;

  &:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 4px;
    border-radius: var(--radius-xs);
  }
}

.logo-mark {
  flex-shrink: 0;
}

.logo-text {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

/* ----- 搜索框 ----- */
.search-wrapper {
  flex: 1;
  max-width: 360px;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 0 var(--space-3);
  height: 38px;
  background: var(--bg-message);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
  color: var(--text-tertiary);
  transition: border-color var(--duration-fast) var(--ease-out-quart), box-shadow var(--duration-fast) var(--ease-out-quart);

  &:focus-within {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px var(--color-primary-ghost);
  }

  @media (max-width: 767px) {
    display: none;
  }
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: var(--text-sm);
  font-family: var(--font-body);
  color: var(--text-primary);
  outline: none;

  &::placeholder {
    color: var(--text-placeholder);
  }
}

/* ----- 导航按钮 ----- */
.nav {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-left: auto;

  @media (max-width: 767px) {
    display: none;
  }
}

.nav-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-family: var(--font-body);
  font-size: var(--text-sm);
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: background var(--duration-fast) var(--ease-out-quart), color var(--duration-fast) var(--ease-out-quart);
  white-space: nowrap;

  &:hover {
    background: var(--color-primary-ghost);
    color: var(--color-primary);
  }

  &:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }
}

.cart-btn {
  position: relative;
}

.cart-badge {
  position: absolute;
  top: 2px;
  right: 4px;
  min-width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
  background: var(--color-danger);
  color: #fff;
  border-radius: var(--radius-full);
  pointer-events: none;
}

.user-btn {
  .user-name {
    max-width: 80px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.theme-btn {
  padding: 6px 8px;
}

/* ----- 汉堡菜单按钮 ----- */
.hamburger {
  display: none;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: none;
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  margin-left: auto;

  &:hover {
    background: var(--bg-message);
  }

  &:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }

  @media (max-width: 767px) {
    display: inline-flex;
  }
}

/* ----- 移动端面板 ----- */
.mobile-panel {
  border-top: 1px solid var(--border-subtle);
  background: var(--bg-card);
  padding: var(--space-4) var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  box-shadow: var(--shadow-lg);
}

.mobile-search {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 0 var(--space-3);
  height: 40px;
  background: var(--bg-message);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
  color: var(--text-tertiary);

  input {
    flex: 1;
    border: none;
    background: transparent;
    font-size: var(--text-sm);
    font-family: var(--font-body);
    color: var(--text-primary);
    outline: none;

    &::placeholder {
      color: var(--text-placeholder);
    }
  }
}

.mobile-links {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mobile-link {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  padding: 12px var(--space-3);
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-family: var(--font-body);
  font-size: var(--text-base);
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--radius-sm);
  text-align: left;
  transition: background var(--duration-fast) var(--ease-out-quart);

  &:hover {
    background: var(--bg-message);
  }

  &:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: -2px;
  }
}

.mobile-badge {
  margin-left: auto;
  min-width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  font-size: 11px;
  font-weight: 700;
  background: var(--color-danger);
  color: #fff;
  border-radius: var(--radius-full);
}

.mobile-theme {
  margin-top: var(--space-2);
  border-top: 1px solid var(--border-subtle);
  padding-top: var(--space-4);
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

/* 滑入动画 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: opacity var(--duration-normal) var(--ease-out-quart), transform var(--duration-normal) var(--ease-out-quart);
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
