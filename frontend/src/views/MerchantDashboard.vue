<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { merchantApi, type MerchantDashboard, type MerchantProduct } from '@/api/modules/merchant'
import { chatStream, type ChatMessage } from '@/api/modules/chat'
import AppHeader from '@/components/AppHeader.vue'
import JIcon from '@/components/JIcon.vue'

const router = useRouter()

const loading = ref(true)
const dash = ref<MerchantDashboard | null>(null)
const products = ref<MerchantProduct[]>([])
const showingAll = ref(false)
const productsLoading = ref(false)

async function loadAllProducts() {
  productsLoading.value = true
  try {
    const res = await merchantApi.products(1, 50)
    products.value = res.list
    showingAll.value = true
  } finally {
    productsLoading.value = false
  }
}

async function load() {
  loading.value = true
  try {
    dash.value = await merchantApi.dashboard()
    products.value = dash.value.products
  } catch {
    /* intercepted */
  } finally {
    loading.value = false
  }
}

const statusTag = (s: number) => (s === 1 ? 'success' : 'info')
const statusText = (s: number) => (s === 1 ? '上架中' : '已下架')

// ===== AI 客服 =====
interface Msg extends ChatMessage {
  id: string
  pending?: boolean
}
const messages = ref<Msg[]>([
  {
    id: 'sys',
    role: 'system',
    content: '你好，我是店铺助手。查商品、看订单、处理售后都可以问我。',
  },
])
const input = ref('')
const sending = ref(false)
const abortCtrl = ref<AbortController | null>(null)
const chatEl = ref<HTMLElement | null>(null)

async function scrollChat() {
  await nextTick()
  if (chatEl.value) chatEl.value.scrollTop = chatEl.value.scrollHeight
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  const aId = `a_${Date.now()}`
  messages.value.push({ id: `u_${Date.now()}`, role: 'user', content: text })
  messages.value.push({ id: aId, role: 'assistant', content: '', pending: true })
  input.value = ''
  await scrollChat()
  sending.value = true
  abortCtrl.value = new AbortController()
  try {
    await chatStream(
      {
        message: text,
        history: messages.value.slice(0, -2).map((m) => ({ role: m.role, content: m.content })),
      },
      {
        onSignal: () => abortCtrl.value!.signal,
        onToken: (c) => {
          const m = messages.value.find((x) => x.id === aId)
          if (m) m.content += c
        },
        onError: (e) => ElMessage.error(e.message),
        onDone: () => {
          const m = messages.value.find((x) => x.id === aId)
          if (m) m.pending = false
        },
      },
    )
  } finally {
    const m = messages.value.find((x) => x.id === aId)
    if (m) m.pending = false
    sending.value = false
    abortCtrl.value = null
    await scrollChat()
  }
}

function stop() {
  abortCtrl.value?.abort()
  sending.value = false
}

onMounted(load)
</script>

<template>
  <div class="page dash-page">
    <AppHeader />
    <div class="page-container" style="max-width: 1100px;">
      <header class="dash-head">
        <div>
          <h1>
            <JIcon name="dashboard" :size="22" />
            商家后台
          </h1>
          <p v-if="dash">欢迎回来，{{ dash.merchantName }}</p>
        </div>
        <button class="btn btn-ghost" @click="router.push('/chat')">
          <JIcon name="chat" :size="14" />
          购物助手
        </button>
      </header>

      <template v-if="loading">
        <div class="stats-skeleton">
          <div v-for="i in 3" :key="i" class="sk-stat" />
        </div>
        <div class="body-skeleton">
          <div class="sk-panel" />
          <div class="sk-panel" />
        </div>
      </template>

      <template v-else-if="dash">
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon stat-icon-products">
              <JIcon name="shop" :size="18" />
            </div>
            <div class="stat-label">上架商品</div>
            <div class="stat-value">{{ dash.productCount }}</div>
            <div class="stat-hint">件正在售卖</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon stat-icon-revenue">
              <JIcon name="star" :size="18" />
            </div>
            <div class="stat-label">营业额</div>
            <div class="stat-value">¥{{ dash.revenue.toFixed(2) }}</div>
            <div class="stat-hint">已支付 / 已发货 / 已完成</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon stat-icon-orders">
              <JIcon name="order" :size="18" />
            </div>
            <div class="stat-label">进行中订单</div>
            <div class="stat-value">{{ dash.ongoingOrders }}</div>
            <div class="stat-hint">待支付 / 已支付 / 已发货</div>
          </div>
        </div>

        <div class="body-grid">
          <div class="panel">
            <div class="panel-header">
              <span class="panel-title">店铺商品</span>
              <button
                v-if="!showingAll"
                class="panel-link"
                :disabled="productsLoading"
                @click="loadAllProducts"
              >
                <JIcon name="refresh" :size="12" />
                {{ productsLoading ? '加载中…' : '查看全部' }}
              </button>
              <button v-else class="panel-link" @click="products = (dash?.products ?? []); showingAll = false">
                收起
              </button>
            </div>
            <el-empty v-if="!products.length" description="暂无商品" />
            <el-table v-else :data="products" size="small">
              <el-table-column label="商品" min-width="200">
                <template #default="{ row }">
                  <div class="prod-cell">
                    <img :src="row.cover" :alt="row.title" class="pc-cover" />
                    <span class="pc-title">{{ row.title }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="价格" width="100" align="right">
                <template #default="{ row }">¥{{ row.price.toFixed(2) }}</template>
              </el-table-column>
              <el-table-column label="销量" width="70" align="right" prop="sales" />
              <el-table-column label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="statusTag(row.status)" size="small" effect="plain">{{ statusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="panel ai-panel">
            <div class="panel-header">
              <span class="panel-title">
                <JIcon name="chat" :size="14" />
                店铺助手
              </span>
              <span class="online-badge" />
            </div>
            <div ref="chatEl" class="ai-messages" role="log" aria-label="AI 客服对话">
              <div
                v-for="m in messages"
                :key="m.id"
                class="ai-msg"
                :class="['role-' + m.role, { pending: m.pending }]"
              >
                <div class="ai-bubble">{{ m.content }}<span v-if="m.pending" class="ai-cursor">▍</span></div>
              </div>
            </div>
            <div class="ai-composer">
              <div class="ai-input-wrap">
                <textarea
                  v-model="input"
                  class="ai-input"
                  :rows="2"
                  placeholder="问问 AI：今天有哪些待发货订单？"
                  :disabled="sending"
                  @keydown.enter.exact.prevent="send"
                />
              </div>
              <div class="ai-actions">
                <button v-if="sending" class="ai-stop" @click="stop">
                  <JIcon name="stop" :size="14" />
                  停止
                </button>
                <button v-else class="ai-send" :disabled="!input.trim()" @click="send">
                  <JIcon name="send" :size="14" />
                  发送
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-body);
  font-size: var(--text-sm);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-quart);

  &:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }
}

.btn-ghost {
  height: 36px;
  padding: 0 16px;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-secondary);
  border-radius: var(--radius-sm);

  &:hover {
    border-color: var(--text-tertiary);
    color: var(--text-primary);
  }
}

.dash-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: var(--space-6);

  h1 {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    font-family: var(--font-display);
    font-size: var(--text-2xl);
    font-weight: 650;
    color: var(--text-primary);
    letter-spacing: -0.02em;
  }

  p {
    margin: var(--space-1) 0 0;
    color: var(--text-secondary);
    font-size: var(--text-sm);
  }
}

/* 统计骨架 */
.stats-skeleton {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-5);
  margin-bottom: var(--space-6);

  @media (max-width: 767px) {
    grid-template-columns: 1fr;
  }
}

.sk-stat {
  height: 120px;
  background: var(--bg-skeleton);
  border-radius: var(--radius-lg);
  animation: pulse 1.5s ease-in-out infinite;
}

.body-skeleton {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: var(--space-5);

  @media (max-width: 1023px) {
    grid-template-columns: 1fr;
  }
}

.sk-panel {
  height: 400px;
  background: var(--bg-skeleton);
  border-radius: var(--radius-lg);
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-5);
  margin-bottom: var(--space-6);

  @media (max-width: 767px) {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: var(--space-5);

  .stat-icon {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--radius-sm);
    margin-bottom: var(--space-3);
  }

  .stat-icon-products {
    background: var(--color-primary-subtle);
    color: var(--color-primary);
  }

  .stat-icon-revenue {
    background: var(--color-success-subtle);
    color: var(--color-success);
  }

  .stat-icon-orders {
    background: var(--color-warning-subtle);
    color: var(--color-warning);
  }

  .stat-label {
    color: var(--text-tertiary);
    font-size: var(--text-sm);
    font-weight: 500;
  }

  .stat-value {
    font-family: var(--font-display);
    font-size: clamp(1.5rem, 3vw, 2rem);
    font-weight: 700;
    color: var(--text-primary);
    margin: var(--space-1) 0;
    letter-spacing: -0.02em;
  }

  .stat-hint {
    font-size: var(--text-xs);
    color: var(--text-tertiary);
  }
}

/* 主区网格 */
.body-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: var(--space-5);
  align-items: stretch;

  @media (max-width: 1023px) {
    grid-template-columns: 1fr;
  }
}

.panel {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;

  :deep(.el-table) {
    border-radius: 0 0 var(--radius-lg) var(--radius-lg);
  }

  :deep(.el-table__header-wrapper) {
    background: var(--bg-message);
  }

  :deep(.el-table th.el-table__cell) {
    background: var(--bg-message);
    color: var(--text-secondary);
    font-weight: 600;
  }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--border-subtle);
  color: var(--text-primary);
  font-size: var(--text-sm);
}

.panel-title {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-base);
  font-weight: 600;
  font-family: var(--font-display);
}

.panel-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: var(--color-primary);
  font-family: var(--font-body);
  font-size: var(--text-xs);
  font-weight: 500;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-xs);
  transition: background var(--duration-fast) var(--ease-out-quart);

  &:hover:not(:disabled) {
    background: var(--color-primary-ghost);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

/* 商品列表 */
.prod-cell {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.pc-cover {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-xs);
  object-fit: cover;
  background: var(--bg-skeleton);
}

.pc-title {
  font-size: var(--text-sm);
  color: var(--text-primary);
}

/* AI 客服 */
.ai-panel {
  height: 100%;
  min-height: 500px;

  @media (max-width: 1023px) {
    min-height: 400px;
  }
}

.online-badge {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-success);
  box-shadow: 0 0 0 3px var(--color-success-subtle);
}

.ai-messages {
  flex: 1;
  min-height: 280px;
  max-height: 400px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-3);
}

.ai-msg {
  display: flex;
  flex-direction: column;
  max-width: 90%;
}

.ai-bubble {
  padding: 9px 13px;
  border-radius: var(--radius-md);
  background: var(--bg-message);
  color: var(--text-primary);
  white-space: pre-wrap;
  line-height: var(--leading-relaxed);
  font-size: var(--text-sm);
}

.role-user {
  align-self: flex-end;

  .ai-bubble {
    background: var(--color-primary);
    color: #fff;
  }
}

.role-system {
  align-self: center;

  .ai-bubble {
    background: transparent;
    color: var(--text-tertiary);
    font-size: var(--text-xs);
  }
}

.ai-cursor {
  display: inline-block;
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

.ai-composer {
  border-top: 1px solid var(--border-subtle);
  padding: var(--space-3);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.ai-input-wrap {
  width: 100%;
}

.ai-input {
  width: 100%;
  border: none;
  background: transparent;
  font-family: var(--font-body);
  font-size: var(--text-sm);
  color: var(--text-primary);
  resize: none;
  outline: none;
  line-height: var(--leading-snug);

  &::placeholder {
    color: var(--text-placeholder);
  }
}

.ai-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}

.ai-send,
.ai-stop {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 14px;
  border: none;
  font-family: var(--font-body);
  font-size: var(--text-xs);
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--radius-xs);
  transition: all var(--duration-fast) var(--ease-out-quart);
}

.ai-send {
  background: var(--color-primary);
  color: #fff;

  &:hover:not(:disabled) {
    background: var(--color-primary-hover);
  }

  &:disabled {
    background: var(--bg-skeleton);
    color: var(--text-tertiary);
    cursor: not-allowed;
  }
}

.ai-stop {
  background: var(--color-danger-subtle);
  color: var(--color-danger);

  &:hover {
    background: var(--color-danger);
    color: #fff;
  }
}
</style>
