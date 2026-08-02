<template>
  <div class="table-section">
    <div class="table-header">
      <span class="table-title">召回司机名单</span>
      <el-button type="primary" size="small" :disabled="selectedRows.length === 0" @click="$emit('batchOutreach', selectedRows.map(r => r.id))">
        <el-icon><Promotion /></el-icon> 批量触达 ({{ selectedRows.length }})
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe
      style="width:100%" max-height="380"
      @sort-change="$emit('sortChange', $event)"
      @selection-change="onSelectionChange" ref="tableRef"
      :row-class-name="rowClass" highlight-current-row>

      <el-table-column type="selection" width="40" />

      <el-table-column prop="driverName" label="司机" width="80" sortable="custom">
        <template #default="{ row }">
          <span class="driver-name">{{ row.driverName }}</span>
        </template>
      </el-table-column>

      <el-table-column label="日完单" width="90" align="center" sortable="custom">
        <template #default="{ row }">
          <el-progress :percentage="orderLevel(row.dailyOrders)" :stroke-width="6" :show-text="false"
            :color="['#91cc75','#fac858','#f56c6c'][orderLevel(row.dailyOrders)>6?0:orderLevel(row.dailyOrders)>3?1:2]" />
          <span class="level-text">{{ orderLabel(row.dailyOrders) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="高峰占比" width="85" align="center">
        <template #default="{ row }">
          <span class="peak-ratio">{{ peakRatio(row) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="在线" width="90" align="center" sortable="custom">
        <template #default="{ row }">
          <el-progress :percentage="onlinePct(row.dailyOnlineHours)" :stroke-width="6" :show-text="false"
            :color="onlinePct(row.dailyOnlineHours)>70?'#67c23a':onlinePct(row.dailyOnlineHours)>40?'#e6a23c':'#f56c6c'" />
          <span class="level-text">{{ onlineLabel(row.dailyOnlineHours) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="收入等级" width="85" align="center">
        <template #default="{ row }">
          <el-tag :type="incomeType(row)" size="small" effect="plain">{{ incomeLabel(row) }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="recallScore" label="召回分" width="110" sortable="custom">
        <template #default="{ row }">
          <div class="score-cell">
            <el-progress :percentage="Number(row.recallScore)" :stroke-width="7"
              :color="scoreColor(row.recallScore)" :show-text="false" />
            <span class="score-num" :style="{ color: scoreColor(row.recallScore) }">{{ row.recallScore }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="strategyScript" label="召回话术" min-width="240" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.strategyScript" class="script-text">{{ row.strategyScript }}</span>
          <span v-else class="muted">待生成</span>
        </template>
      </el-table-column>

      <el-table-column prop="recommendedChannelLabel" label="渠道" width="85" align="center" />

      <el-table-column prop="outreachStatusLabel" label="状态" width="85" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.outreachStatus)" size="small" effect="dark" round>
            {{ row.outreachStatusLabel || row.outreachStatus }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="120" fixed="right" align="center">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="$emit('viewDetail', row)">详情</el-button>
          <el-button v-if="row.outreachStatus === 'PENDING'" size="small" text type="success"
            @click="$emit('outreach', row)">触达</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-footer">
      <div class="footer-info">共 {{ page.total }} 条</div>
      <el-pagination small background
        v-model:current-page="currentPage" v-model:page-size="currentSize"
        :total="page.total" :page-sizes="[20,50,100]"
        layout="sizes, prev, pager, next"
        @size-change="onPageChange" @current-change="onPageChange" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Promotion } from '@element-plus/icons-vue'

const props = defineProps({ list: Array, loading: Boolean, page: Object })
defineEmits(['batchOutreach', 'viewDetail', 'outreach', 'pageChange', 'sortChange'])

const tableRef = ref(null)
const selectedRows = ref([])
const currentPage = ref(props.page?.page || 1)
const currentSize = ref(props.page?.size || 20)

watch(() => props.page, v => {
  if (v) { currentPage.value = v.page; currentSize.value = v.size }
})

function onSelectionChange(rows) { selectedRows.value = rows }
function onPageChange() {
  // 切换页时清除勾选
  selectedRows.value = []
  tableRef.value?.clearSelection()
}

function scoreColor(v) {
  const s = Number(v); if (s >= 80) return '#67c23a'; if (s >= 60) return '#e6a23c'; return '#f56c6c'
}
function statusType(s) {
  const m = { PENDING:'warning', CONTACTED:'primary', AGREED:'success', DECLINED:'danger', NO_RESPONSE:'info' }
  return m[s] || 'info'
}
function rowClass({ row }) {
  if (row.outreachStatus === 'AGREED') return 'row-agreed'
  return ''
}

// 脱敏函数
function orderLevel(v) { if (!v||v<=2) return 2; if (v<=4) return 5; if (v<=6) return 8; return 10 }
function orderLabel(v) { if (!v||v<=2) return '少'; if (v<=4) return '中'; if (v<=6) return '多'; return '很多' }
function peakRatio(row) {
  const m = row.morningPeakOrders||0, e = row.eveningPeakOrders||0
  if (m+e===0) return '—'; if (m>e) return '早>晚'; if (e>m) return '晚>早'; return '均衡'
}
function onlinePct(v) { if (!v) return 2; return Math.min(10, Math.round(Number(v)*10)) }
function onlineLabel(v) { if (!v||v<4) return '低'; if (v<8) return '中'; return '高' }
function incomeType(row) {
  const v = Number(row.baseIncome||0); if (v>200) return 'success'; if (v>120) return ''; return 'info'
}
function incomeLabel(row) {
  const v = Number(row.baseIncome||0); if (v>200) return '高'; if (v>120) return '中'; return '低'
}
</script>

<style scoped>
.table-section { background: #fff; border-radius: 12px; padding: 16px 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.table-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.table-title { font-size: 15px; font-weight: 600; color: #303133; }
.driver-name { font-weight: 500; }
.score-cell { display: flex; align-items: center; gap: 6px; }
.score-cell .el-progress { flex: 1; }
.score-num { font-weight: 700; font-size: 14px; min-width: 34px; text-align: right; }
.script-text { font-size: 13px; color: #606266; line-height: 1.4; }
.muted { color: #c0c4cc; }
.peak-text { font-size: 13px; color: #606266; }
.income-text { font-size: 13px; font-weight: 500; }
.income-base { color: #303133; }
.income-bonus { color: #67c23a; font-size: 12px; margin-left: 2px; }
.table-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 14px; }
.footer-info { font-size: 13px; color: #909399; }
:deep(.row-agreed) { background: #f0f9eb !important; }
</style>
