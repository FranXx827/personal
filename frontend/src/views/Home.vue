<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { productApi, type Product } from '@/api/modules/product'
import AppHeader from '@/components/AppHeader.vue'
import JIcon from '@/components/JIcon.vue'

const router = useRouter()
const hotProducts = ref<Product[]>([])
const loading = ref(false)

const categories = [
  { icon: 'devices', name: '数码电器' },
  { icon: 'backpack', name: '服饰鞋包' },
  { icon: 'home', name: '家居生活' },
  { icon: 'sparkles', name: '美妆个护' },
  { icon: 'food', name: '食品生鲜' },
  { icon: 'book', name: '图书文娱' },
  { icon: 'running', name: '运动户外' },
  { icon: 'toy', name: '母婴玩具' },
]

onMounted(async () => {
  loading.value = true
  try {
    hotProducts.value = await productApi.hot(8)
  } finally {
    loading.value = false
  }
})

const catIcons: Record<string, string> = {
  devices: 'M4 6a2 2 0 012-2h12a2 2 0 012 2v7a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 15v3M10 15v3M7 18h10',
  backpack: 'M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z',
  home: 'M2.25 12l8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25',
  sparkles: 'M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456zM16.894 20.567L16.5 21.75l-.394-1.183a2.25 2.25 0 00-1.423-1.423L13.5 18.75l1.183-.394a2.25 2.25 0 001.423-1.423l.394-1.183.394 1.183a2.25 2.25 0 001.423 1.423l1.183.394-1.183.394a2.25 2.25 0 00-1.423 1.423z',
  food: 'M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z',
  book: 'M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25',
  running: 'M15.362 5.214A8.252 8.252 0 0112 21 8.25 8.25 0 016.038 7.048 8.287 8.287 0 009 9.6a8.983 8.983 0 013.361-6.867 8.21 8.21 0 003 2.48z',
  toy: 'M9 9l10.5-3m0 6.553v3.75a2.25 2.25 0 01-1.632 2.163l-1.32.377a1.803 1.803 0 11-.99-3.467l2.31-.66a2.25 2.25 0 001.632-2.163zm0 0V2.25L9 5.25v10.303m0 0v3.75a2.25 2.25 0 01-1.632 2.163l-1.32.377a1.803 1.803 0 01-.99-3.467l2.31-.66A2.25 2.25 0 009 15.553z',
}
</script>

<template>
  <div class="page home">
    <AppHeader />

    <section class="hero">
      <div class="hero-inner">
        <span class="hero-badge">
          <JIcon name="sparkles" :size="14" />
          智能选品
        </span>
        <h1 class="hero-title">挑好物<br />问就行</h1>
        <p class="hero-subtitle">一句话描述你想要的，剩下的事交给它</p>
        <div class="hero-actions">
          <button class="btn btn-primary" @click="router.push('/chat')">
            <JIcon name="chat" :size="16" />
            开始对话
          </button>
          <button class="btn btn-ghost" @click="router.push('/search')">
            去逛逛
            <JIcon name="arrowRight" :size="14" />
          </button>
        </div>
      </div>
    </section>

    <!-- 分类导航 — 无 emoji，使用 SVG 线形图标 -->
    <section class="section">
      <div class="section-inner">
        <div class="cat-grid">
          <div
            v-for="c in categories"
            :key="c.name"
            class="cat-item"
            role="button"
            tabindex="0"
            :aria-label="`搜索${c.name}`"
            @click="router.push({ name: 'Search', query: { keyword: c.name } })"
            @keydown.enter="router.push({ name: 'Search', query: { keyword: c.name } })"
          >
            <div class="cat-icon-wrap">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path :d="catIcons[c.icon]" />
              </svg>
            </div>
            <span class="cat-name">{{ c.name }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="section">
      <div class="section-inner">
        <div class="section-head">
          <h2 class="section-title">热门推荐</h2>
          <button class="section-link" @click="router.push('/search')">
            查看全部
            <JIcon name="arrowRight" :size="12" />
          </button>
        </div>

        <template v-if="loading">
          <div class="skeleton-grid">
            <div v-for="i in 4" :key="i" class="skeleton-card">
              <div class="skeleton-img" />
              <div class="skeleton-lines">
                <div class="skeleton-line w-70" />
                <div class="skeleton-line w-40" />
                <div class="skeleton-line w-50" />
              </div>
            </div>
          </div>
        </template>

        <el-empty v-else-if="!hotProducts.length" description="暂无推荐商品，去搜索看看吧">
          <el-button type="primary" @click="router.push('/search')">去搜索</el-button>
        </el-empty>

        <div v-else class="product-grid">
          <article
            v-for="p in hotProducts"
            :key="p.id"
            class="product-card"
            role="button"
            tabindex="0"
            :aria-label="`查看 ${p.title} 详情`"
            @click="router.push(`/product/${p.id}`)"
            @keydown.enter="router.push(`/product/${p.id}`)"
          >
            <figure class="cover-wrap">
              <img :src="p.cover" :alt="p.title" class="cover" loading="lazy" />
            </figure>
            <div class="card-body">
              <h3 class="prod-title" :title="p.title">{{ p.title }}</h3>
              <div class="prod-meta">
                <span class="merchant">{{ p.merchantName }}</span>
                <span>已售 {{ p.sales }}</span>
              </div>
              <div class="prod-price">
                <span class="price-symbol">¥</span>
                <span class="price-value">{{ p.price.toFixed(2) }}</span>
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>

    <footer class="home-footer">
      <span class="footer-mark" />
      <span>智能电商 · 懂你所想</span>
    </footer>
  </div>
</template>

<style scoped lang="scss">
/* ============================ */
/* Hero                          */
/* ============================ */
.hero {
  padding: var(--space-16) var(--space-5) var(--space-12);
  text-align: center;

  @media (min-width: 768px) {
    padding: var(--space-20) var(--space-6) var(--space-16);
  }
}

.hero-inner {
  max-width: 600px;
  margin: 0 auto;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: var(--text-xs);
  font-weight: 600;
  padding: 5px 14px;
  border-radius: var(--radius-full);
  background: var(--color-primary-subtle);
  color: var(--color-primary);
  margin-bottom: var(--space-5);
}

.hero-title {
  font-family: var(--font-display);
  font-size: clamp(2rem, 5vw, 3rem);
  font-weight: 700;
  margin: 0 0 var(--space-4);
  color: var(--text-primary);
  line-height: var(--leading-tight);
  letter-spacing: -0.03em;
}

.hero-subtitle {
  font-size: var(--text-base);
  color: var(--text-secondary);
  margin: 0 0 var(--space-6);
  line-height: var(--leading-relaxed);
}

.hero-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: center;
  flex-wrap: wrap;
}

/* 自定义按钮 */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 10px 24px;
  font-family: var(--font-body);
  font-size: var(--text-sm);
  font-weight: 600;
  border: none;
  cursor: pointer;
  border-radius: var(--radius-full);
  transition: all var(--duration-fast) var(--ease-out-quart);
  user-select: none;

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
    box-shadow: var(--shadow-md);
  }

  &:active {
    transform: translateY(0);
  }
}

.btn-ghost {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);

  &:hover {
    background: var(--bg-message);
    color: var(--text-primary);
    border-color: var(--text-tertiary);
  }
}

/* ============================ */
/* 通用区块                       */
/* ============================ */
.section {
  padding: 0 var(--space-5);

  @media (min-width: 768px) {
    padding: 0 var(--space-6);
  }
}

.section-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding-bottom: var(--space-10);

  @media (min-width: 768px) {
    padding-bottom: var(--space-12);
  }
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-5);
}

.section-title {
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: 650;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.section-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: var(--font-body);
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-tertiary);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: color var(--duration-fast) var(--ease-out-quart), background var(--duration-fast) var(--ease-out-quart);

  &:hover {
    color: var(--color-primary);
    background: var(--color-primary-ghost);
  }
}

/* ============================ */
/* 分类网格 — 无 emoji 图标          */
/* ============================ */
.cat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-3);

  @media (min-width: 480px) {
    grid-template-columns: repeat(4, 1fr);
    gap: var(--space-4);
  }

  @media (min-width: 768px) {
    grid-template-columns: repeat(8, 1fr);
    gap: var(--space-4);
  }
}

.cat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-2);
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-quart);

  &:hover {
    border-color: var(--color-primary-subtle);
    background: var(--color-primary-subtle);
    transform: translateY(-2px);
    box-shadow: var(--shadow-sm);
  }

  &:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }

  @media (max-width: 480px) {
    padding: var(--space-3) var(--space-1);
  }
}

.cat-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: var(--bg-message);
  color: var(--text-secondary);
  transition: all var(--duration-fast) var(--ease-out-quart);

  .cat-item:hover & {
    background: var(--color-primary-subtle);
    color: var(--color-primary);
  }

  @media (max-width: 480px) {
    width: 38px;
    height: 38px;
  }
}

.cat-name {
  font-size: var(--text-xs);
  font-weight: 500;
  color: var(--text-secondary);
  text-align: center;
}

/* ============================ */
/* 商品网格                       */
/* ============================ */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: var(--space-4);

  @media (min-width: 768px) {
    gap: var(--space-5);
  }
}

.product-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  cursor: pointer;
  overflow: hidden;
  transition: all var(--duration-normal) var(--ease-out-quart);

  &:hover {
    border-color: var(--border-color);
    box-shadow: var(--shadow-md);
    transform: translateY(-3px);
  }

  &:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }
}

.cover-wrap {
  margin: 0;
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  background: var(--bg-skeleton);
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--duration-slow) var(--ease-out-quart);

  .product-card:hover & {
    transform: scale(1.05);
  }
}

.card-body {
  padding: var(--space-4);

  @media (max-width: 600px) {
    padding: var(--space-3);
  }
}

.prod-title {
  font-family: var(--font-body);
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin: 0 0 var(--space-1);
}

.prod-meta {
  display: flex;
  justify-content: space-between;
  font-size: var(--text-xs);
  color: var(--text-tertiary);

  .merchant {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 60%;
  }
}

.prod-price {
  margin-top: var(--space-2);
  display: flex;
  align-items: baseline;
  gap: 1px;
  color: var(--color-primary);

  .price-symbol {
    font-size: var(--text-sm);
    font-weight: 600;
  }

  .price-value {
    font-family: var(--font-display);
    font-size: var(--text-lg);
    font-weight: 700;
    letter-spacing: -0.01em;
  }
}

/* ============================ */
/* 骨架屏                         */
/* ============================ */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: var(--space-4);
}

.skeleton-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.skeleton-img {
  width: 100%;
  aspect-ratio: 1;
  background: var(--bg-skeleton);
  animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-lines {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.skeleton-line {
  height: 12px;
  border-radius: 4px;
  background: var(--bg-skeleton);
  animation: pulse 1.5s ease-in-out infinite;

  &.w-70 { width: 70%; }
  &.w-40 { width: 40%; }
  &.w-50 { width: 50%; }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ============================ */
/* 页脚                          */
/* ============================ */
.home-footer {
  text-align: center;
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  padding: var(--space-12) var(--space-5) var(--space-16);

  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
}

.footer-mark {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--text-tertiary);
  opacity: 0.5;
}
</style>
