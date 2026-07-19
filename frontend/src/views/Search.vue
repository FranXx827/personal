<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi, type Product, type ProductQuery } from '@/api/modules/product'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import JIcon from '@/components/JIcon.vue'

const route = useRoute()
const router = useRouter()

const products = ref<Product[]>([])
const total = ref(0)
const loading = ref(false)
const filterOpen = ref(false)

const query = reactive<ProductQuery>({
  keyword: '',
  page: 1,
  pageSize: 20,
  sort: 'sales_desc',
})

const filterForm = reactive({
  minPrice: undefined as number | undefined,
  maxPrice: undefined as number | undefined,
  sort: 'sales_desc' as ProductQuery['sort'],
})

async function load() {
  loading.value = true
  try {
    const res = await productApi.search({ ...query, ...filterForm })
    products.value = res.list
    total.value = res.total
  } catch {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

function goDetail(p: Product) {
  router.push(`/product/${p.id}`)
}

function onSortChange(v: ProductQuery['sort']) {
  filterForm.sort = v
  load()
}

onMounted(() => {
  const kw = route.query.keyword as string | undefined
  if (kw) query.keyword = kw
  load()
})

watch(
  () => route.query.keyword,
  (kw) => {
    if (typeof kw === 'string') {
      query.keyword = kw
      load()
    }
  },
)
</script>

<template>
  <div class="page search-page">
    <AppHeader />

    <div class="page-container">
      <div class="mobile-filter-bar">
        <span v-if="query.keyword" class="search-tag">
          <b>{{ query.keyword }}</b>
        </span>
        <span class="result-count">共 {{ total }} 件商品</span>
        <button class="filter-toggle" @click="filterOpen = !filterOpen">
          <JIcon name="filter" :size="14" />
          筛选
          <JIcon :name="filterOpen ? 'arrowUp' : 'arrowDown'" :size="12" />
        </button>
      </div>

      <div class="layout">
        <aside class="filter-sidebar" :class="{ open: filterOpen }">
          <div class="filter-card">
            <h3 class="filter-title">筛选</h3>
            <div class="filter-group">
              <label class="filter-label">价格区间</label>
              <div class="price-range">
                <input v-model.number="filterForm.minPrice" type="number" class="price-input" placeholder="最低" min="0" />
                <span class="range-sep">—</span>
                <input v-model.number="filterForm.maxPrice" type="number" class="price-input" placeholder="最高" min="0" />
              </div>
            </div>
            <div class="filter-group">
              <label class="filter-label">排序</label>
              <div class="sort-options">
                <button
                  v-for="opt in [
                    { value: 'sales_desc' as const, label: '销量' },
                    { value: 'price_asc' as const, label: '价格↑' },
                    { value: 'price_desc' as const, label: '价格↓' },
                    { value: 'newest' as const, label: '最新' },
                  ]"
                  :key="opt.value"
                  class="sort-btn"
                  :class="{ active: filterForm.sort === opt.value }"
                  @click="onSortChange(opt.value)"
                >
                  {{ opt.label }}
                </button>
              </div>
            </div>
            <button class="apply-btn" @click="load">
              <JIcon name="check" :size="14" />
              应用筛选
            </button>
          </div>
        </aside>

        <main class="results" role="main">
          <div class="results-header">
            <span v-if="query.keyword" class="result-keyword">
              “<b>{{ query.keyword }}</b>” 的搜索结果
            </span>
            <span class="result-count">共 {{ total }} 件商品</span>
          </div>

          <template v-if="loading">
            <div class="skeleton-grid">
              <div v-for="i in 6" :key="i" class="skeleton-card">
                <div class="skeleton-img" />
                <div class="skeleton-lines">
                  <div class="skeleton-line w-70" />
                  <div class="skeleton-line w-40" />
                  <div class="skeleton-line w-50" />
                </div>
              </div>
            </div>
          </template>

          <el-empty v-else-if="!products.length" description="未找到匹配商品" />

          <div v-else class="product-grid">
            <article
              v-for="p in products"
              :key="p.id"
              class="product-card"
              role="button"
              tabindex="0"
              :aria-label="`查看 ${p.title} 详情`"
              @click="goDetail(p)"
              @keydown.enter="goDetail(p)"
            >
              <figure class="cover-wrap">
                <img :src="p.cover" :alt="p.title" class="cover" loading="lazy" />
              </figure>
              <div class="card-body">
                <h3 class="prod-title">{{ p.title }}</h3>
                <div class="prod-price">
                  <span class="price-symbol">¥</span>
                  <span class="price-value">{{ p.price.toFixed(2) }}</span>
                </div>
                <div class="prod-meta">
                  <span>{{ p.merchantName }}</span>
                  <span>销量 {{ p.sales }}</span>
                </div>
              </div>
            </article>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
/* 移动端筛选条 */
.mobile-filter-bar {
  display: none;
  align-items: center;
  gap: var(--space-2);
  padding-bottom: var(--space-4);
  font-size: var(--text-sm);
  color: var(--text-secondary);

  .search-tag {
    color: var(--text-secondary);
    font-size: var(--text-sm);
  }

  .result-count {
    margin-right: auto;
    color: var(--text-tertiary);
    font-size: var(--text-sm);
  }

  @media (max-width: 767px) {
    display: flex;
  }
}

.filter-toggle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-family: var(--font-body);
  font-size: var(--text-xs);
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--radius-full);
  transition: all var(--duration-fast) var(--ease-out-quart);

  &:hover {
    border-color: var(--color-primary);
    color: var(--color-primary);
  }
}

/* 布局 */
.layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: var(--space-6);
  align-items: start;

  @media (max-width: 767px) {
    grid-template-columns: 1fr;
  }
}

/* 筛选侧栏 */
.filter-sidebar {
  position: sticky;
  top: 84px;

  @media (max-width: 767px) {
    position: static;
    display: none;

    &.open {
      display: block;
    }
  }
}

.filter-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}

.filter-title {
  font-family: var(--font-display);
  font-size: var(--text-base);
  font-weight: 600;
  margin: 0 0 var(--space-5);
  color: var(--text-primary);
}

.filter-group {
  margin-bottom: var(--space-4);
}

.filter-label {
  display: block;
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: var(--space-2);
  letter-spacing: 0.02em;
}

.price-range {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.price-input {
  width: 80px;
  height: 38px;
  padding: 0 var(--space-2);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xs);
  background: var(--bg-card);
  font-family: var(--font-body);
  font-size: var(--text-sm);
  color: var(--text-primary);
  text-align: center;
  outline: none;
  transition: border-color var(--duration-fast) var(--ease-out-quart);

  &::placeholder {
    color: var(--text-placeholder);
  }

  &:focus {
    border-color: var(--color-primary);
  }

  /* 移除默认 spin button */
  &::-webkit-inner-spin-button,
  &::-webkit-outer-spin-button {
    -webkit-appearance: none;
    margin: 0;
  }
  -moz-appearance: textfield;
}

.range-sep {
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

.sort-options {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.sort-btn {
  padding: 5px 12px;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-family: var(--font-body);
  font-size: var(--text-xs);
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--radius-xs);
  transition: all var(--duration-fast) var(--ease-out-quart);

  &:hover {
    border-color: var(--color-primary);
    color: var(--color-primary);
  }

  &.active {
    background: var(--color-primary);
    border-color: var(--color-primary);
    color: #fff;
  }
}

.apply-btn {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: var(--space-2);
  padding: 10px;
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-family: var(--font-body);
  font-size: var(--text-sm);
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: background var(--duration-fast) var(--ease-out-quart);

  &:hover {
    background: var(--color-primary-hover);
  }
}

/* 结果区域 */
.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--border-subtle);

  @media (max-width: 767px) {
    display: none;
  }
}

.result-keyword {
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

.result-count {
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

/* 商品网格 */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
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
    transform: translateY(-2px);
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
  padding: var(--space-3);
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

.prod-price {
  display: flex;
  align-items: baseline;
  gap: 1px;
  color: var(--color-primary);

  .price-symbol {
    font-size: var(--text-xs);
    font-weight: 600;
  }

  .price-value {
    font-family: var(--font-display);
    font-size: var(--text-base);
    font-weight: 700;
    letter-spacing: -0.01em;
  }
}

.prod-meta {
  display: flex;
  justify-content: space-between;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: var(--space-1);
}

/* 骨架屏 */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
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
  padding: var(--space-3);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.skeleton-line {
  height: 10px;
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
</style>
