<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi, type UserType } from '@/api/modules/auth'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'login' | 'register'>('login')
const loginType = ref<UserType>('BUYER')
const registerType = ref<UserType>('BUYER')

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ merchantName: '', name: '', username: '', password: '', phone: '' })

const loading = ref(false)
const showPassword = ref(false)
const showRegPassword = ref(false)

function roleHome(role: UserType | ''): string {
  const redirect = router.currentRoute.value.query.redirect as string | undefined
  if (role === 'MERCHANT') return '/merchant/dashboard'
  return redirect && redirect.startsWith('/') ? redirect : '/'
}

async function doLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const role = await userStore.login({
      username: loginForm.username,
      password: loginForm.password,
      type: loginType.value,
    })
    ElMessage.success('登录成功')
    router.replace(roleHome(role))
  } catch {
    /* api error handled by interceptor */
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  const isMerchant = registerType.value === 'MERCHANT'
  const username = registerForm.username.trim()
  const password = registerForm.password
  if (!username || !password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  if (isMerchant && !registerForm.merchantName.trim()) {
    ElMessage.warning('请填写商户名称')
    return
  }
  if (!isMerchant && !registerForm.name.trim()) {
    ElMessage.warning('请填写用户名称')
    return
  }

  loading.value = true
  try {
    if (isMerchant) {
      await authApi.registerMerchant({
        merchantName: registerForm.merchantName.trim(),
        username,
        password,
        phone: registerForm.phone.trim(),
      })
    } else {
      await authApi.registerBuyer({
        name: registerForm.name.trim(),
        username,
        password,
        phone: registerForm.phone.trim(),
      })
    }
    ElMessage.success('注册成功，正在登录…')
    const role = await userStore.login({ username, password, type: registerType.value })
    router.replace(roleHome(role))
  } catch {
    /* api error handled by interceptor */
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-bg" aria-hidden="true">
      <div class="orb orb-1" />
      <div class="orb orb-2" />
      <div class="orb orb-3" />
      <div class="auth-vignette" />
    </div>

    <div class="auth-container">
      <div class="brand-section">
        <div class="brand-logo">智能电商</div>

        <h1 class="brand-title">买卖一体</h1>
        <p class="brand-sub">对话选品 · 轻松管店</p>

        <div class="brand-features">
          <span class="feature-item">商家经营：管商品、看营收</span>
          <span class="feature-item">买家购物：对话即搜索</span>
        </div>
      </div>

      <div class="auth-card">
        <div class="auth-tabs">
          <button
            class="auth-tab"
            :class="{ active: activeTab === 'login' }"
            @click="activeTab = 'login'"
          >登录</button>
          <button
            class="auth-tab"
            :class="{ active: activeTab === 'register' }"
            @click="activeTab = 'register'"
          >注册</button>
          <div class="auth-tab-indicator" :style="{ left: activeTab === 'login' ? '0%' : '50%' }" />
        </div>

        <div v-show="activeTab === 'login'" class="auth-form">
          <div class="type-switch">
            <button
              class="type-btn"
              :class="{ active: loginType === 'BUYER' }"
              @click="loginType = 'BUYER'"
            >买家</button>
            <button
              class="type-btn"
              :class="{ active: loginType === 'MERCHANT' }"
              @click="loginType = 'MERCHANT'"
            >商户</button>
          </div>

          <div class="field">
            <label class="field-label">用户名</label>
            <div class="field-input-wrap">
              <input v-model="loginForm.username" type="text" class="field-input" placeholder="输入用户名" @keyup.enter="doLogin" />
            </div>
          </div>

          <div class="field">
            <label class="field-label">密码</label>
            <div class="field-input-wrap">
              <input
                v-model="loginForm.password"
                :type="showPassword ? 'text' : 'password'"
                class="field-input"
                placeholder="输入密码"
                @keyup.enter="doLogin"
              />
              <button
                class="password-toggle"
                type="button"
                tabindex="-1"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                <svg v-if="showPassword" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                </svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                  <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </button>
            </div>
          </div>

          <button class="btn-submit" :disabled="loading" @click="doLogin">
            <span v-if="loading" class="spinner" />
            <span v-else>登录</span>
          </button>
        </div>

        <div v-show="activeTab === 'register'" class="auth-form">
          <div class="type-switch">
            <button
              class="type-btn"
              :class="{ active: registerType === 'BUYER' }"
              @click="registerType = 'BUYER'"
            >买家</button>
            <button
              class="type-btn"
              :class="{ active: registerType === 'MERCHANT' }"
              @click="registerType = 'MERCHANT'"
            >商户</button>
          </div>

          <div class="field" v-if="registerType === 'MERCHANT'">
            <label class="field-label">商户名称</label>
            <div class="field-input-wrap">
              <input v-model="registerForm.merchantName" type="text" class="field-input" placeholder="店铺名称（不可重复）" />
            </div>
          </div>
          <div class="field" v-else>
            <label class="field-label">用户名称</label>
            <div class="field-input-wrap">
              <input v-model="registerForm.name" type="text" class="field-input" placeholder="你的昵称（不可重复）" />
            </div>
          </div>

          <div class="field">
            <label class="field-label">用户名</label>
            <div class="field-input-wrap">
              <input v-model="registerForm.username" type="text" class="field-input" placeholder="登录用，不可重复" />
            </div>
          </div>

          <div class="field">
            <label class="field-label">
              密码
              <span class="optional">6–64 位</span>
            </label>
            <div class="field-input-wrap">
              <input
                v-model="registerForm.password"
                :type="showRegPassword ? 'text' : 'password'"
                class="field-input"
                placeholder="设置登录密码"
                @keyup.enter="doRegister"
              />
              <button
                class="password-toggle"
                type="button"
                tabindex="-1"
                :aria-label="showRegPassword ? '隐藏密码' : '显示密码'"
                @click="showRegPassword = !showRegPassword"
              >
                <svg v-if="showRegPassword" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                </svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                  <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </button>
            </div>
          </div>

          <div class="field">
            <label class="field-label">手机号 <span class="optional">选填</span></label>
            <div class="field-input-wrap">
              <input v-model="registerForm.phone" type="text" class="field-input" placeholder="用于接收订单通知" />
            </div>
          </div>

          <button class="btn-submit" :disabled="loading" @click="doRegister">
            <span v-if="loading" class="spinner" />
            <span v-else>注册</span>
          </button>
        </div>

        <div class="switch-tip">
          <template v-if="activeTab === 'login'">
            还没有账号？<button class="link-btn" @click="activeTab = 'register'">去注册</button>
          </template>
          <template v-else>
            已有账号？<button class="link-btn" @click="activeTab = 'login'">去登录</button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 多层叠加：径向光晕制造纵深，再叠一层柔和线性渐变打底 */
  background:
    radial-gradient(120% 100% at 18% 0%, rgba(94, 82, 214, 0.28) 0%, transparent 55%),
    radial-gradient(120% 120% at 100% 100%, rgba(54, 78, 130, 0.30) 0%, transparent 52%),
    radial-gradient(90% 90% at 50% 50%, rgba(40, 34, 64, 0.0) 0%, rgba(15, 13, 24, 0.55) 100%),
    linear-gradient(160deg, #16131f 0%, #1b1730 48%, #141120 100%);
  padding: var(--space-6);
  position: relative;
  overflow: hidden;
}

/* ---- 背景装饰 ---- */
.auth-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  /* screen 混合让色块像"发光"般融入暗色背景，而非贴上来的硬块 */
  mix-blend-mode: screen;
  opacity: 0.55;

  &.orb-1 {
    width: 520px;
    height: 520px;
    background: radial-gradient(circle at 30% 30%, #8b7ff5, #5e52d6 70%);
    top: -160px;
    right: -120px;
    animation: orb-drift-1 14s ease-in-out infinite;
  }

  &.orb-2 {
    width: 380px;
    height: 380px;
    background: radial-gradient(circle at 30% 30%, #5b8bd6, #3a5ea8 70%);
    bottom: -90px;
    left: -70px;
    animation: orb-drift-2 11s ease-in-out infinite reverse;
  }

  &.orb-3 {
    width: 220px;
    height: 220px;
    background: radial-gradient(circle at 30% 30%, #a99bff, #7367f0 70%);
    top: 46%;
    left: 32%;
    animation: orb-drift-3 9s ease-in-out infinite;
  }
}

/* 暗角：让四周自然压暗，卡片中心更聚焦、与背景过渡更顺 */
.auth-vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(120% 120% at 50% 45%, transparent 55%, rgba(10, 8, 18, 0.55) 100%);
}

@keyframes orb-drift-1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-40px, 30px) scale(1.06); }
  66% { transform: translate(20px, -20px) scale(0.96); }
}

@keyframes orb-drift-2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(30px, -40px) scale(1.08); }
}

@keyframes orb-drift-3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-30px, 20px) scale(1.12); }
}

/* ---- 主容器 ---- */
.auth-container {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 440px;
  padding-top: 5vh;
}

/* ---- 品牌广告区 ---- */
.brand-section {
  text-align: center;
  margin-bottom: var(--space-6);
  animation: section-fade-in 0.8s var(--ease-out-expo) both;
}

@keyframes section-fade-in {
  from { opacity: 0; transform: translateY(-12px); }
  to { opacity: 1; transform: translateY(0); }
}

.brand-logo {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-full);
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #fff;
  margin-bottom: var(--space-6);
}

.brand-title {
  font-family: var(--font-display);
  font-size: clamp(2rem, 5vw, 2.75rem);
  font-weight: 700;
  line-height: var(--leading-tight);
  margin: 0 0 var(--space-3);
  color: #fff;
  letter-spacing: -0.03em;
}

.brand-sub {
  font-size: var(--text-base);
  color: rgba(255, 255, 255, 0.55);
  margin: 0 0 var(--space-6);
  letter-spacing: 0.05em;
}

.brand-features {
  display: flex;
  justify-content: center;
  gap: var(--space-5);
  flex-wrap: wrap;
}

.feature-item {
  position: relative;
  padding: 0 var(--space-3);
  font-size: var(--text-xs);
  color: rgba(255, 255, 255, 0.45);
  line-height: var(--leading-snug);

  & + .feature-item::before {
    content: '';
    position: absolute;
    left: calc(var(--space-3) * -1);
    top: 50%;
    width: 1px;
    height: 12px;
    transform: translateY(-50%);
    background: rgba(255, 255, 255, 0.12);
  }
}

/* ---- 登录/注册卡片（与背景融合的玻璃面板）---- */
.auth-card {
  width: 100%;
  max-width: 420px;
  position: relative;
  overflow: hidden;
  /* 渐变玻璃质感，比纯色半透明更自然 */
  background: linear-gradient(180deg, rgba(46, 42, 66, 0.55) 0%, rgba(26, 24, 38, 0.45) 100%);
  backdrop-filter: blur(40px) saturate(180%);
  -webkit-backdrop-filter: blur(40px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.10);
  /* 更大的圆角，让背景板边角更圆滑 */
  border-radius: 30px;
  /* 去掉内边距，让登录/注册标签栏作为卡片头部与边框重合 */
  padding: 0;
  /* 柔和彩色投影 + 紫色融合光晕 + 内高光；box-shadow 严格贴合卡片圆角，不会出现第二层背景 */
  box-shadow:
    0 30px 80px -24px rgba(10, 8, 20, 0.7),
    0 0 80px -12px rgba(115, 103, 240, 0.45),
    0 0 0 1px rgba(255, 255, 255, 0.03),
    inset 0 1px 0 rgba(255, 255, 255, 0.07);
  animation: card-fade-in 0.6s var(--ease-out-expo) 0.3s both;
}

@keyframes card-fade-in {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.auth-tabs {
  display: flex;
  position: relative;
  /* 作为卡片头部，与卡片边框重合：去掉自身独立框，改为底部细分割线 */
  margin: 0;
  background: rgba(255, 255, 255, 0.04);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 0;
  padding: 4px;
}

.auth-tab {
  flex: 1;
  padding: 14px 0;
  text-align: center;
  background: transparent;
  border: none;
  font-family: var(--font-body);
  font-size: var(--text-sm);
  font-weight: 500;
  color: rgba(231, 230, 234, 0.6);
  cursor: pointer;
  border-radius: 6px;
  transition: all var(--duration-fast) var(--ease-out-quart);
  position: relative;
  z-index: 1;

  &.active {
    color: #fff;
    font-weight: 600;
    background: rgba(255, 255, 255, 0.14);
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
  }

  &:focus-visible {
    outline: 2px solid #8b7ff5;
    outline-offset: 2px;
  }
}

.type-switch {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-5);
  padding: 3px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: var(--radius-sm);

  .type-btn {
    flex: 1;
    padding: 8px 12px;
    border: none;
    background: transparent;
    color: rgba(231, 230, 234, 0.6);
    font-family: var(--font-body);
    font-size: var(--text-xs);
    font-weight: 500;
    cursor: pointer;
    border-radius: 5px;
    transition: all var(--duration-fast) var(--ease-out-quart);

    &.active {
      background: rgba(255, 255, 255, 0.14);
      color: #fff;
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
    }

    &:focus-visible {
      outline: 2px solid #8b7ff5;
      outline-offset: 2px;
    }
  }
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-6) var(--space-7) 0;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: var(--text-xs);
  font-weight: 600;
  color: rgba(231, 230, 234, 0.72);
}

.optional {
  font-weight: 400;
  color: rgba(231, 230, 234, 0.45);
  font-size: var(--text-caption);
}

.field-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.field-input {
  width: 100%;
  height: 46px;
  padding: 0 var(--space-3);
  font-family: var(--font-body);
  font-size: var(--text-sm);
  color: #e7e6ea;
  background: rgba(255, 255, 255, 0.06);
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-sm);
  outline: none;
  transition:
    border-color var(--duration-fast) var(--ease-out-quart),
    box-shadow var(--duration-fast) var(--ease-out-quart),
    background var(--duration-fast) var(--ease-out-quart);

  &::placeholder {
    color: rgba(231, 230, 234, 0.4);
  }

  &:focus {
    border-color: #8b7ff5;
    box-shadow: 0 0 0 3px rgba(139, 127, 245, 0.22);
    background: rgba(255, 255, 255, 0.09);
  }
}

.password-toggle {
  position: absolute;
  right: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: rgba(231, 230, 234, 0.55);
  cursor: pointer;
  border-radius: var(--radius-xs);
  transition: color var(--duration-fast) var(--ease-out-quart), background var(--duration-fast) var(--ease-out-quart);

  &:hover {
    color: #e7e6ea;
    background: rgba(139, 127, 245, 0.16);
  }

  &:focus-visible {
    outline: 2px solid #8b7ff5;
    outline-offset: 2px;
  }
}

.btn-submit {
  width: 100%;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: var(--space-3);
  border: none;
  background: linear-gradient(135deg, #7c70f5 0%, #5e52d6 100%);
  color: #fff;
  font-family: var(--font-body);
  font-size: var(--text-sm);
  font-weight: 600;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition:
    background var(--duration-fast) var(--ease-out-quart),
    transform var(--duration-fast) var(--ease-out-quart),
    box-shadow var(--duration-normal) var(--ease-out-quart);

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 8px 28px rgba(115, 103, 240, 0.45);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
    box-shadow: none;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.switch-tip {
  text-align: center;
  margin-top: var(--space-5);
  padding: 0 var(--space-7) var(--space-7);
  font-size: var(--text-sm);
  color: rgba(231, 230, 234, 0.45);
}

.link-btn {
  background: none;
  border: none;
  color: #a39af7;
  font-family: var(--font-body);
  font-size: var(--text-sm);
  font-weight: 600;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: var(--radius-xs);
  transition: color var(--duration-fast) var(--ease-out-quart), background var(--duration-fast) var(--ease-out-quart);

  &:hover {
    color: #c4bcfb;
    background: rgba(139, 127, 245, 0.16);
  }
}

@media (prefers-reduced-motion: reduce) {
  .brand-section,
  .auth-card {
    animation: none;
  }

  .orb {
    animation: none;
    opacity: 0.45;
  }
}
</style>
