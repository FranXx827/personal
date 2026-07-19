<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { orderApi, type Order } from '@/api/modules/order'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'

const orders = ref<Order[]>([])
const loading = ref(false)

const statusConfig: Record<string, { label: string; type: 'warning' | 'success' | 'info' | 'danger' }> = {
  PENDING_PAY: { label: '待支付', type: 'warning' },
  PAID: { label: '已支付', type: 'success' },
  SHIPPED: { label: '已发货', type: 'success' },
  COMPLETED: { label: '已完成', type: 'info' },
  CANCELED: { label: '已取消', type: 'info' },
  REFUNDING: { label: '退款中', type: 'danger' },
}

async function load() {
  loading.value = true
  try {
    const res = await orderApi.list({ page: 1, pageSize: 20 })
    orders.value = res.list
  } finally {
    loading.value = false
  }
}

async function cancel(o: Order) {
  try {
    await orderApi.cancel(o.orderNo, '用户主动取消')
    ElMessage.success('已取消')
    load()
  } catch {
    /* intercepted */
  }
}

async function pay(o: Order) {
  try {
    const { payUrl } = await orderApi.pay(o.orderNo)
    window.open(payUrl, '_blank')
  } catch {
    /* intercepted */
  }
}

onMounted(load)
</script>

<template>
  <div class="page orders-page">
    <AppHeader />
    <div class="page-container" style="max-width: 900px;">
      <h2 class="page-title">我的订单</h2>

      <template v-if="loading">
        <div class="sk-list">
          <div v-for="i in 3" :key="i" class="sk-order">
            <div class="sk-h" />
            <div class="sk-body">
              <div class="sk-line w-70" />
              <div class="sk-line w-50" />
            </div>
            <div class="sk-f" />
          </div>
        </div>
      </template>

      <el-empty v-else-if="!orders.length" description="暂无订单" />

      <div v-else class="order-list">
        <div v-for="o in orders" :key="o.orderNo" class="order-card">
          <div class="order-header">
            <span class="order-no">订单号：{{ o.orderNo }}</span>
            <el-tag :type="statusConfig[o.status]?.type || 'info'" size="small" effect="plain">
              {{ statusConfig[o.status]?.label || o.status }}
            </el-tag>
          </div>

          <div class="order-items">
            <div v-for="item in o.items" :key="item.skuId" class="order-item">
              <img :src="item.cover" :alt="item.title" class="item-cover" />
              <div class="item-info">
                <div class="item-title">{{ item.title }}</div>
                <div class="item-meta">¥{{ item.price.toFixed(2) }} × {{ item.quantity }}</div>
              </div>
            </div>
          </div>

          <div class="order-footer">
            <div class="order-total">
              合计：<b class="total-amount">¥{{ o.totalAmount.toFixed(2) }}</b>
            </div>
            <div class="order-actions">
              <button v-if="o.status === 'PENDING_PAY'" class="btn btn-sm btn-primary" @click="pay(o)">去支付</button>
              <button v-if="o.status === 'PENDING_PAY'" class="btn btn-sm btn-ghost" @click="cancel(o)">取消订单</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.order-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.order-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3) var(--space-5);
  background: var(--bg-message);
  border-bottom: 1px solid var(--border-subtle);
}

.order-no {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

.order-items {
  padding: var(--space-4) var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.order-item {
  display: flex;
  gap: var(--space-3);
  align-items: center;
}

.item-cover {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  background: var(--bg-skeleton);
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.item-meta {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: 2px;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3) var(--space-5);
  border-top: 1px solid var(--border-subtle);
}

.order-total {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.total-amount {
  font-family: var(--font-display);
  font-size: var(--text-base);
  font-weight: 700;
  color: var(--color-primary);
}

.order-actions {
  display: flex;
  gap: var(--space-2);
}

.btn {
  display: inline-flex;
  align-items: center;
  font-family: var(--font-body);
  border: none;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-quart);
  font-weight: 500;

  &:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }
}

.btn-sm {
  height: 32px;
  padding: 0 14px;
  font-size: var(--text-xs);
  border-radius: var(--radius-xs);
}

.btn-primary {
  background: var(--color-primary);
  color: #fff;

  &:hover {
    background: var(--color-primary-hover);
  }
}

.btn-ghost {
  background: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-secondary);

  &:hover {
    border-color: var(--text-tertiary);
    color: var(--text-primary);
  }
}

/* 骨架 */
.sk-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.sk-order {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.sk-h, .sk-f {
  height: 44px;
  background: var(--bg-skeleton);
  animation: pulse 1.5s ease-in-out infinite;
}

.sk-body {
  padding: var(--space-4) var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.sk-line {
  height: 14px;
  border-radius: 4px;
  background: var(--bg-skeleton);
  animation: pulse 1.5s ease-in-out infinite;
  &.w-70 { width: 70%; }
  &.w-50 { width: 50%; }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
