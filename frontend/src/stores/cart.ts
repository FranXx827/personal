/**
 * 购物车状态 (持久化到 localStorage)
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CartItem } from '@/api/modules/order'

export const useCartStore = defineStore(
  'cart',
  () => {
    const items = ref<CartItem[]>([])

    const totalQuantity = computed(() =>
      items.value.reduce((sum, i) => sum + i.quantity, 0),
    )

    const selectedItems = computed(() => items.value.filter((i) => i.selected))

    const totalAmount = computed(() =>
      selectedItems.value.reduce((sum, i) => sum + i.price * i.quantity, 0),
    )

    function add(item: CartItem) {
      const exist = items.value.find((i) => i.skuId === item.skuId)
      if (exist) exist.quantity += item.quantity
      else items.value.push(item)
    }

    function remove(skuId: number) {
      items.value = items.value.filter((i) => i.skuId !== skuId)
    }

    function updateQuantity(skuId: number, quantity: number) {
      const item = items.value.find((i) => i.skuId === skuId)
      if (item) item.quantity = Math.max(1, quantity)
    }

    function toggleSelect(skuId: number) {
      const item = items.value.find((i) => i.skuId === skuId)
      if (item) item.selected = !item.selected
    }

    function selectAll(selected: boolean) {
      items.value.forEach((i) => (i.selected = selected))
    }

    function clear() {
      items.value = []
    }

    return {
      items,
      totalQuantity,
      selectedItems,
      totalAmount,
      add,
      remove,
      updateQuantity,
      toggleSelect,
      selectAll,
      clear,
    }
  },
  { persist: true },
)
