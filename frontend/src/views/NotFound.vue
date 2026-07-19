<script setup lang="ts">
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'

const router = useRouter()
</script>

<template>
  <div class="page not-found">
    <AppHeader />
    <div class="content">
      <h1>404</h1>
      <p>你访问的页面不存在</p>
      <button class="btn btn-primary" @click="router.push('/')">
        返回首页
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.content {
  text-align: center;
  padding: var(--space-16) var(--space-5);
  color: var(--text-secondary);

  h1 {
    font-family: var(--font-display);
    font-size: clamp(4rem, 10vw, 6rem);
    font-weight: 700;
    margin: 0;
    color: var(--color-primary);
    line-height: 1;
    letter-spacing: -0.04em;
  }

  p {
    margin: var(--space-4) 0 var(--space-6);
    font-size: var(--text-base);
  }
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 42px;
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
</style>
