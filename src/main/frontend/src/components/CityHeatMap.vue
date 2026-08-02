<template>
  <div class="city-list">
    <div v-for="(item, i) in sorted" :key="item.name" class="city-row">
      <span class="city-rank" :style="{ color: rankColor(i) }">{{ i + 1 }}</span>
      <span class="city-name">{{ item.name }}</span>
      <span class="city-count">前100</span>
      <div class="city-bar-bg">
        <div class="city-bar-fill" :style="{ width: (item.value / maxVal * 100) + '%', background: barColor(i) }"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ data: Array })

const sorted = computed(() =>
  [...(props.data || [])].sort((a, b) => (b.value || 0) - (a.value || 0))
)
const maxVal = computed(() => Math.max(...(props.data || []).map(d => d.value || 0), 1))

function rankColor(i) { const c = ['#f56c6c','#e6a23c','#fac858','#909399']; return c[i] || '#c0c4cc' }
function barColor(i) { const c = ['#f56c6c','#e6a23c','#fac858','#67c23a','#409EFF','#5470c6','#73c0de','#a0a0a0']; return c[i % c.length] }
</script>

<style scoped>
.city-list { padding: 8px 16px 12px; }
.city-row { display: flex; align-items: center; gap: 8px; padding: 6px 0; }
.city-rank { font-size: 14px; font-weight: 700; width: 20px; text-align: center; }
.city-name { font-size: 13px; font-weight: 500; color: #303133; width: 44px; white-space: nowrap; }
.city-count { font-size: 12px; color: #909399; width: 42px; text-align: right; }
.city-bar-bg { flex: 1; height: 8px; background: #f0f2f5; border-radius: 4px; overflow: hidden; }
.city-bar-fill { height: 100%; border-radius: 4px; transition: width .4s ease; min-width: 4px; }
</style>
