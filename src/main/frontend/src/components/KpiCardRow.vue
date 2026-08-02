<template>
  <div class="kpi-row">
    <div class="kpi-card primary">
      <div class="kpi-icon"><el-icon :size="26"><UserFilled /></el-icon></div>
      <div class="kpi-body">
        <div class="kpi-label">拉取司机总数</div>
        <div class="kpi-value">100<span class="unit">人</span></div>
        <div class="kpi-desc">T-1日离线司机快照数据</div>
      </div>
    </div>

    <div class="kpi-card warning">
      <div class="kpi-icon"><el-icon :size="26"><WarningFilled /></el-icon></div>
      <div class="kpi-body">
        <div class="kpi-label">过滤后候选司机</div>
        <div class="kpi-value">76<span class="unit">人</span></div>
        <div class="kpi-desc">满足在线+完单+供需比条件</div>
      </div>
    </div>

    <div class="kpi-card success">
      <div class="kpi-icon"><el-icon :size="26"><TrendCharts /></el-icon></div>
      <div class="kpi-body">
        <div class="kpi-label">预期召回成功率</div>
        <div class="kpi-value">{{ pct(kpi?.expectedSuccessRate) }}<span class="unit">%</span></div>
        <div class="kpi-desc">基于历史转化模型预估</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { UserFilled, WarningFilled, TrendCharts } from '@element-plus/icons-vue'
defineProps({ kpi: Object })
const fmt = v => v != null ? Number(v).toLocaleString() : '0'
const pct = v => v != null ? (Number(v) * 100).toFixed(1) : '0.0'
</script>

<style scoped>
.kpi-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 16px; }
@media (max-width: 800px) { .kpi-row { grid-template-columns: 1fr; } }
.kpi-card { background: #fff; border-radius: 12px; padding: 20px 24px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06); transition: transform .15s; }
.kpi-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.10); }
.kpi-icon { width: 52px; height: 52px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.primary .kpi-icon { background: #eef2ff; color: #5470c6; }
.warning .kpi-icon { background: #fef6e8; color: #e6a23c; }
.success .kpi-icon { background: #edf7f0; color: #67c23a; }
.kpi-label { font-size: 13px; color: #909399; margin-bottom: 2px; white-space: nowrap; }
.kpi-value { font-size: 28px; font-weight: 700; color: #303133; }
.kpi-value .unit { font-size: 14px; color: #909399; font-weight: 400; margin-left: 3px; }
.kpi-desc { font-size: 12px; color: #c0c4cc; margin-top: 3px; }
</style>
