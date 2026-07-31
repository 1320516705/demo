<template>
  <div class="kpi-row">
    <div class="kpi-card primary">
      <div class="kpi-icon"><el-icon :size="28"><UserFilled /></el-icon></div>
      <div class="kpi-body">
        <div class="kpi-label">可召回司机</div>
        <div class="kpi-value">{{ fmt(kpi?.recallableCount) }}<span class="unit">人</span></div>
        <div class="kpi-delta positive" v-if="kpi?.improvementPct">
          <el-icon><Top /></el-icon> 较规则提升 {{ pct(kpi?.improvementPct) }}
        </div>
      </div>
    </div>

    <div class="kpi-card success">
      <div class="kpi-icon"><el-icon :size="28"><TrendCharts /></el-icon></div>
      <div class="kpi-body">
        <div class="kpi-label">预期成功率</div>
        <div class="kpi-value">{{ pct(kpi?.expectedSuccessRate) }}<span class="unit">%</span></div>
        <div class="kpi-delta" v-if="kpi?.expectedSuccessRate > 0.3">基于历史转化模型预估</div>
      </div>
    </div>

    <div class="kpi-card warning">
      <div class="kpi-icon"><el-icon :size="28"><Coin /></el-icon></div>
      <div class="kpi-body">
        <div class="kpi-label">今日预算</div>
        <div class="kpi-value"><span class="prefix">¥</span>{{ fmt(kpi?.todayBudget) }}</div>
        <div class="kpi-delta">高潜司机 × 人均触达成本</div>
      </div>
    </div>

    <div class="kpi-card info">
      <div class="kpi-icon"><el-icon :size="28"><DataAnalysis /></el-icon></div>
      <div class="kpi-body">
        <div class="kpi-label">规则引擎基准</div>
        <div class="kpi-value">{{ fmt(kpi?.ruleBasedCount) }}<span class="unit">人</span></div>
        <div class="kpi-delta">ML 模型筛选更精准</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { UserFilled, TrendCharts, Coin, DataAnalysis, Top } from '@element-plus/icons-vue'
defineProps({ kpi: Object })
const fmt = v => v != null ? Number(v).toLocaleString() : '0'
const pct = v => v != null ? (Number(v) * 100).toFixed(1) : '0.0'
</script>

<style scoped>
.kpi-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px; }
@media (max-width: 1100px) { .kpi-row { grid-template-columns: repeat(2, 1fr); } }

.kpi-card { background: #fff; border-radius: 12px; padding: 20px 24px;
  display: flex; align-items: center; gap: 18px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06); transition: transform .15s, box-shadow .15s; }
.kpi-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.10); }

.kpi-icon { width: 56px; height: 56px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.primary .kpi-icon { background: #eef2ff; color: #5470c6; }
.success .kpi-icon { background: #edf7f0; color: #67c23a; }
.warning .kpi-icon { background: #fef6e8; color: #e6a23c; }
.info   .kpi-icon { background: #eaf4fe; color: #409EFF; }

.kpi-body { min-width: 0; }
.kpi-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.kpi-value { font-size: 28px; font-weight: 700; color: #303133; letter-spacing: -0.5px; }
.kpi-value .unit { font-size: 14px; color: #909399; font-weight: 400; margin-left: 3px; }
.kpi-value .prefix { font-size: 16px; font-weight: 500; margin-right: 2px; }
.kpi-delta { font-size: 12px; color: #909399; margin-top: 4px; display: flex; align-items: center; gap: 2px; }
.kpi-delta.positive { color: #67c23a; }
</style>
