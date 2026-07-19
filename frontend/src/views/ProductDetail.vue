<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi, type Product } from '@/api/modules/product'
import { cartApi, seckillApi } from '@/api/modules/order'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import JIcon from '@/components/JIcon.vue'

const route = useRoute()
const router = useRouter()
const product = ref<Product | null>(null)
const quantity = ref(1)
const loading = ref(false)
const seckillLoading = ref(false)

async function load() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    product.value = await productApi.detail(id)
  } finally {
    loading.value = false
  }
}

async function addToCart() {
  if (!product.value) return
  try {
    await cartApi.add(product.value.id, quantity.value)
    ElMessage.success('已加入购物车')
  } catch {
    /* intercepted */
  }
}

async function buyNow() {
  if (!product.value) return
  try {
    await cartApi.add(product.value.id, quantity.value)
    router.push('/cart')
  } catch {
    /* intercepted */
  }
}

async function seckill() {
  if (!product.value) return
  seckillLoading.value = true
  try {
    const { orderNo } = await seckillApi.seckill(product.value.id)
    ElMessage.success(`秒杀成功，订单号 ${orderNo}`)
    router.push('/orders')
  } catch {
    /* intercepted */
  } finally {
    seckillLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page detail-page">
    <AppHeader />

    <div class="page-container">
      <div v-if="loading" class="skeleton-layout">
        <div class="skeleton-cover" />
        <div class="skeleton-info">
          <div class="skeleton-line w-80 h-7" />
          <div class="skeleton-line w-40 h-5" />
          <div class="skeleton-line w-60 h-16" />
          <div class="skeleton-line w-50 h-4" />
          <div class="skeleton-line w-100 h-12" />
        </div>
      </div>

      <template v-else-if="product">
        <div class="detail-layout">
          <figure class="cover-section">
            <img :src="product.cover" :alt="product.title" class="cover" />
          </figure>

          <div class="info-section">
            <h1 class="title">{{ product.title }}</h1>
            <div class="merchant-row">
              <JIcon name="shop" :size="14" />
              <span>{{ product.merchantName }}</span>
            </div>

            <div class="price-box">
              <span class="price-symbol">¥</span>
              <span class="price-value">{{ product.price.toFixed(2) }}</span>
            </div>

            <p class="description">{{ product.description }}</p>

            <div class="meta-row">
              <span>
                <JIcon name="star" :size="14" />
                评分 {{ product.rating.toFixed(1) }}
              </span>
              <span class="meta-dot" />
              <span>已售 {{ product.sales }}</span>
              <span class="meta-dot" />
              <span>库存 {{ product.stock }}</span>
            </div>

            <div class="actions">
              <div class="qty-wrapper">
                <span class="qty-label">数量</span>
                <div class="qty-control">
                  <button class="qty-btn" :disabled="quantity <= 1" @click="quantity--">
                    <JIcon name="minus" :size="14" />
                  </button>
                  <span class="qty-value">{{ quantity }}</span>
                  <button class="qty-btn" :disabled="quantity >= 99" @click="quantity++">
                    <JIcon name="plus" :size="14" />
                  </button>
                </div>
              </div>

              <div class="btn-group">
                <button class="btn btn-primary" @click="addToCart">
                  <JIcon name="cart" :size="15" />
                  加入购物车
                </button>
                <button class="btn btn-solid" @click="buyNow">立即购买</button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
/* 骨架 */
.skeleton-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-8);
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: var(--space-6);

  @media (max-width: 767px) {
    grid-template-columns: 1fr;
  }
}

.skeleton-cover {
  aspect-ratio: 1;
  background: var(--bg-skeleton);
  border-radius: var(--radius-md);
  animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.skeleton-line {
  height: 16px;
  border-radius: 4px;
  background: var(--bg-skeleton);
  animation: pulse 1.5s ease-in-out infinite;

  &.w-80 { width: 80%; }
  &.w-40 { width: 40%; }
  &.w-60 { width: 60%; }
  &.w-50 { width: 50%; }
  &.w-100 { width: 100%; }
  &.h-7 { height: 28px; }
  &.h-5 { height: 20px; }
  &.h-16 { height: 64px; }
  &.h-4 { height: 16px; }
  &.h-12 { height: 48px; }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 详情布局 */
.detail-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-8);
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: var(--space-6);

  @media (max-width: 767px) {
    grid-template-columns: 1fr;
    gap: var(--space-5);
    padding: var(--space-4);
  }
}

.cover-section {
  margin: 0;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--bg-skeleton);
}

.cover {
  width: 100%;
  display: block;
}

/* 信息区 */
.info-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.title {
  font-family: var(--font-display);
  font-size: clamp(1.25rem, 2.5vw, 1.625rem);
  font-weight: 650;
  color: var(--text-primary);
  line-height: var(--leading-tight);
  letter-spacing: -0.02em;
}

.merchant-row {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

.price-box {
  display: flex;
  align-items: baseline;
  gap: 2px;
  background: var(--color-primary-subtle);
  padding: var(--space-4) var(--space-5);
  border-radius: var(--radius-md);
  color: var(--color-primary);

  .price-symbol {
    font-size: var(--text-lg);
    font-weight: 600;
  }

  .price-value {
    font-family: var(--font-display);
    font-size: clamp(1.5rem, 3vw, 2rem);
    font-weight: 700;
    letter-spacing: -0.02em;
  }
}

.description {
  color: var(--text-secondary);
  line-height: var(--leading-relaxed);
  margin: 0;
  font-size: var(--text-sm);
}

.meta-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--text-tertiary);
  font-size: var(--text-sm);

  .j-icon {
    margin-right: 2px;
    vertical-align: middle;
  }
}

.meta-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--text-tertiary);
  opacity: 0.4;
}

/* 操作区 */
.actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border-subtle);
}

.qty-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.qty-label {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-secondary);
}

.qty-control {
  display: flex;
  align-items: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.qty-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: var(--bg-message);
  color: var(--text-secondary);
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-out-quart), color var(--duration-fast) var(--ease-out-quart);

  &:hover:not(:disabled) {
    background: var(--color-primary-ghost);
    color: var(--color-primary);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.qty-value {
  width: 48px;
  text-align: center;
  font-family: var(--font-display);
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.btn-group {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 44px;
  padding: 0 24px;
  font-family: var(--font-body);
  font-size: var(--text-sm);
  font-weight: 600;
  border: none;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out-quart);

  &:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }
}

.btn-primary {
  background: var(--color-primary);
  color: #fff;

  &:hover {
    background: var(--color-primary-hover);
    transform: translateY(-1px);
    box-shadow: var(--shadow-sm);
  }
}

.btn-solid {
  background: var(--text-primary);
  color: var(--text-inverse);

  &:hover {
    opacity: 0.85;
    transform: translateY(-1px);
  }
}
</style>
