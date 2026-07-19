<script setup lang="ts">
import { RouterView } from 'vue-router'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
themeStore.init()
</script>

<template>
  <el-config-provider>
    <RouterView v-slot="{ Component, route }">
      <transition name="fade-page" mode="out-in">
        <component :is="Component" :key="route.fullPath" />
      </transition>
    </RouterView>
  </el-config-provider>
</template>

<style lang="scss">
.fade-page-enter-active,
.fade-page-leave-active {
  transition: opacity 0.25s ease;
}
.fade-page-enter-from,
.fade-page-leave-to {
  opacity: 0;
}
</style>
