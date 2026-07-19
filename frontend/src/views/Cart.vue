<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { orderApi } from '@/api/modules/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import JIcon from '@/components/JIcon.vue'

const router = useRouter()
const cart = useCartStore()

const totalText = computed(() => cart.totalAmount.toFixed(2))
const itemCount = computed(() => cart.selectedItems.length)

async function checkout() {
  if (!cart.selectedItems.length) {
    ElMessage.warning('请选择商品')
    return
  }
  try {
    await ElMessageBox.confirm(`确认下单，合计 ¥${totalText.value}？`, '下单确认')
    const { orderNo } = await orderApi.create({
      items: cart.selectedItems.map((i) => ({ skuId: i.skuId, quantity: i.quantity })),
      addressId: 1,
    })
    cart.clear()
    ElMessage.success('下单成功')
    router.push(`/orders`)
  } catch {
    /* cancelled or intercepted */
  }
}
</script>

<template>
  <div class="page cart-page">
    <AppHeader />
    <div class="page-container" style="max-width: 900px;">
      <h2 class="page-title">购物车</h2>

      <el-empty v-if="!cart.items.length" description="购物车是空的">
        <el-button type="primary" @click="router.push('/')">去逛逛</el-button>
      </el-empty>

      <template v-else>
        <div class="cart-table-wrap">
          <el-table :data="cart.items" class="cart-table" @selection-change="cart.selectAll">
            <el-table-column type="selection" width="50" />
            <el-table-column label="商品" min-width="280">
              <template #default="{ row }">
                <div class="product-cell">
                  <img :src="row.cover" :alt="row.title" class="cart-cover" />
                  <div>
                    <div class="cart-item-title">{{ row.title }}</div>
                    <div class="cart-item-specs">{{ row.specs }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="110" align="right">
              <template #default="{ row }">
                <span class="cell-price">¥{{ row.price.toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="150" align="center">
              <template #default="{ row }">
                <div class="qty-control">
                  <button class="qty-btn" :disabled="row.quantity <= 1" @click="row.quantity--; cart.updateQuantity(row.skuId, row.quantity)">
                    <JIcon name="minus" :size="12" />
                  </button>
                  <span class="qty-value">{{ row.quantity }}</span>
                  <button class="qty-btn" :disabled="row.quantity >= 99" @click="row.quantity++; cart.updateQuantity(row.skuId, row.quantity)">
                    <JIcon name="plus" :size="12" />
                  </button>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="小计" width="110" align="right">
              <template #default="{ row }">
                <span class="cell-subtotal">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="" width="70" align="center">
              <template #default="{ row }">
                <button class="del-btn" @click="cart.remove(row.skuId)" title="删除">
                  <JIcon name="trash" :size="14" />
                </button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="cart-mobile">
          <div v-for="item in cart.items" :key="item.skuId" class="cart-mobile-item">
            <el-checkbox :model-value="item.selected" @change="cart.toggleSelect(item.skuId)" />
            <img :src="item.cover" :alt="item.title" class="m-cover" />
            <div class="m-info">
              <div class="m-title">{{ item.title }}</div>
              <div class="m-specs">{{ item.specs }}</div>
              <div class="m-bottom">
                <span class="m-price">¥{{ item.price.toFixed(2) }}</span>
                <div class="qty-control sm">
                  <button class="qty-btn" :disabled="item.quantity <= 1" @click="item.quantity--; cart.updateQuantity(item.skuId, item.quantity)">
                    <JIcon name="minus" :size="10" />
                  </button>
                  <span class="qty-value">{{ item.quantity }}</span>
                  <button class="qty-btn" :disabled="item.quantity >= 99" @click="item.quantity++; cart.updateQuantity(item.skuId, item.quantity)">
                    <JIcon name="plus" :size="10" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="checkout-bar">
          <div class="checkout-info">
            <span>已选 <b>{{ itemCount }}</b> 件</span>
            <span class="checkout-divider" />
            <span>合计：<b class="checkout-total">¥{{ totalText }}</b></span>
          </div>
          <button class="checkout-btn" @click="checkout">去结算</button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
/* 桌面端表格 */
.cart-table-wrap {
  @media (max-width: 767px) {
    display: none;
  }
}

.cart-table {
  border-radius: var(--radius-md);
  overflow: hidden;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.cart-cover {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  background: var(--bg-skeleton);
  flex-shrink: 0;
}

.cart-item-title {
  color: var(--text-primary);
  font-size: var(--text-sm);
  font-weight: 600;
}

.cart-item-specs {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: 2px;
}

.cell-price {
  font-family: var(--font-display);
  font-weight: 600;
  color: var(--text-secondary);
}

.cell-subtotal {
  font-family: var(--font-display);
  font-weight: 700;
  color: var(--color-primary);
}

.del-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  border-radius: var(--radius-xs);
  transition: all var(--duration-fast) var(--ease-out-quart);

  &:hover {
    background: var(--color-danger-subtle);
    color: var(--color-danger);
  }
}

/* 数量控件 */
.qty-control {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xs);
  overflow: hidden;

  &.sm {
    .qty-btn { width: 28px; height: 28px; }
    .qty-value { width: 36px; font-size: var(--text-xs); }
  }
}

.qty-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
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
  width: 40px;
  text-align: center;
  font-family: var(--font-display);
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
}

/* 移动端列表 */
.cart-mobile {
  display: none;
  flex-direction: column;
  gap: var(--space-3);

  @media (max-width: 767px) {
    display: flex;
  }
}

.cart-mobile-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: var(--space-3);

  .m-cover {
    width: 68px;
    height: 68px;
    border-radius: var(--radius-sm);
    object-fit: cover;
    flex-shrink: 0;
  }
}

.m-info {
  flex: 1;
  min-width: 0;
}

.m-title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.m-specs {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: 2px;
}

.m-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-2);
}

.m-price {
  font-family: var(--font-display);
  font-size: var(--text-base);
  font-weight: 700;
  color: var(--color-primary);
}

/* 底部结算栏 */
.checkout-bar {
  margin-top: var(--space-5);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-5);
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  position: sticky;
  bottom: var(--space-4);
  box-shadow: var(--shadow-md);
}

.checkout-info {
  font-size: var(--text-sm);
  color: var(--text-secondary);

  b { font-weight: 700; }
}

.checkout-divider {
  display: inline-block;
  width: 1px;
  height: 14px;
  background: var(--border-color);
  margin: 0 var(--space-3);
  vertical-align: middle;
}

.checkout-total {
  font-family: var(--font-display);
  font-size: var(--text-lg);
  color: var(--color-primary);
}

.checkout-btn {
  height: 42px;
  padding: 0 28px;
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-family: var(--font-body);
  font-size: var(--text-sm);
  font-weight: 600;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: background var(--duration-fast) var(--ease-out-quart), transform var(--duration-fast) var(--ease-out-quart);

  &:hover {
    background: var(--color-primary-hover);
    transform: translateY(-1px);
  }
}
</style>
