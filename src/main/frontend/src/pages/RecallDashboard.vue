<template>
  <div class="app-shell">
    <!-- 顶栏 -->
    <header class="topbar">
      <div class="topbar-left">
        <div class="logo-dot"></div>
        <h1 class="app-title">司机智能召回</h1>
        <span class="badge">运营后台</span>
      </div>
      <div class="topbar-right">
        <el-date-picker v-model="store.currentDate" type="date"
          format="YYYY-MM-DD" value-format="YYYY-MM-DD" size="default"
          @change="refreshAll" style="width:168px" />
        <el-button type="primary" size="default" @click="triggerPipeline" :loading="pipelineLoading" round>
          <el-icon><VideoPlay /></el-icon> 运行管道
        </el-button>
        <el-button size="default" @click="refreshAll" :loading="refreshing" round>
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </header>

    <div class="content">
      <!-- KPI 卡片 -->
      <KpiCardRow :kpi="store.kpiData" />

      <!-- 表格 + 地图 横排 -->
      <div class="table-map-row">
        <div class="table-map-main">
          <div class="section-card">
            <div class="section-head">
              <el-icon :size="18"><List /></el-icon> 召回司机名单
              <span class="section-badge">{{ store.driverListPage.total }} 条记录</span>
            </div>
            <div class="section-body">
              <TableFilterBar :filters="store.filters" @change="onFilterChange" />
            </div>
            <DriverListTable
              :list="store.driverList"
              :loading="store.driverListLoading"
              :page="store.driverListPage"
              @viewDetail="openDetail"
              @outreach="openOutreach"
              @batchOutreach="openBatchOutreach"
              @pageChange="onPageChange"
              @sortChange="onSortChange" />
          </div>
        </div>
        <div class="table-map-side">
          <div class="mini-map-head"><el-icon><Location /></el-icon> 城市分布</div>
          <CityHeatMap :data="store.cityHeatData" />
        </div>
      </div>
      <!-- 图表 -->
      <div class="chart-row">
        <div class="chart-card">
          <div class="chart-head"><el-icon><TrendCharts /></el-icon> 近7天趋势</div>
          <TrendChart :data="store.trendData" />
        </div>
        <div class="chart-card">
          <div class="chart-head"><el-icon><Histogram /></el-icon> 触达状态分布</div>
          <StatusDistribution :data="store.statusDistribution" />
        </div>
      </div>
    </div>

    <OutreachDialog v-model="outreachVisible" :driver="selectedDriver"
      :driverIds="batchIds" :isBatch="isBatchOutreach" @confirm="doOutreach" />
    <DriverDetailDrawer v-model="detailVisible" :driver="selectedDriver" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRecallStore } from '@/stores/recall'
import KpiCardRow from '@/components/KpiCardRow.vue'
import DriverListTable from '@/components/DriverListTable.vue'
import TableFilterBar from '@/components/TableFilterBar.vue'
import TrendChart from '@/components/TrendChart.vue'
import StatusDistribution from '@/components/StatusDistribution.vue'
import CityHeatMap from '@/components/CityHeatMap.vue'
import OutreachDialog from '@/components/OutreachDialog.vue'
import DriverDetailDrawer from '@/components/DriverDetailDrawer.vue'
import { VideoPlay, Refresh, Location, List, TrendCharts, Histogram } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const store = useRecallStore()
const outreachVisible = ref(false); const detailVisible = ref(false)
const selectedDriver = ref(null); const isBatchOutreach = ref(false); const batchIds = ref([])
const pipelineLoading = ref(false); const refreshing = ref(false)

onMounted(() => refreshAll())

async function refreshAll() {
  refreshing.value = true
  try {
    const d = store.currentDate
    await Promise.all([store.fetchKpi(d), store.fetchDriverList(d),
      store.fetchTrend(7, d), store.fetchDistribution(d, 'status'), store.fetchCityHeatmap(d)])
  } finally { refreshing.value = false }
}

async function triggerPipeline() {
  pipelineLoading.value = true
  try {
    await axios.post('/api/v1/pipeline/trigger', { dataDate: store.currentDate })
    ElMessage.success('管道已启动，稍后自动刷新')
    setTimeout(() => refreshAll(), 3000)
  } catch { ElMessage.error('触发失败') }
  finally { pipelineLoading.value = false }
}

function onFilterChange(f) { store.updateFilters(f); store.fetchDriverList() }
function onPageChange() { store.fetchDriverList() }
function onSortChange({ prop, order }) {
  store.sortField = prop || 'recallScore'; store.sortOrder = order === 'ascending' ? 'asc' : 'desc'
  store.fetchDriverList()
}
function openDetail(row) { selectedDriver.value = row; detailVisible.value = true }
function openOutreach(row) { selectedDriver.value = row; isBatchOutreach.value = false; outreachVisible.value = true }
function openBatchOutreach(ids) { batchIds.value = ids; isBatchOutreach.value = true; outreachVisible.value = true }
async function doOutreach({ channel }) {
  try {
    if (isBatchOutreach.value) {
      const res = await store.batchOutreach(batchIds.value, channel)
      ElMessage.success(`触达完成，成功 ${res.successCount} 条`)
    } else {
      await store.singleOutreach(selectedDriver.value.id, channel)
      ElMessage.success('触达成功')
    }
    refreshAll()
  } catch { ElMessage.error('触达失败') }
}
</script>

<style>
/* 全局重置 */
body { margin: 0; background: #f0f2f5; }
</style>

<style scoped>
.app-shell { min-height: 100vh; background: linear-gradient(180deg, #f0f2f5 0%, #e8ecf1 100%); }

/* 顶栏 */
.topbar { height: 60px; background: #fff; border-bottom: 1px solid #e8ecf1;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 28px; position: sticky; top: 0; z-index: 100;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.topbar-left { display: flex; align-items: center; gap: 12px; }
.logo-dot { width: 10px; height: 10px; border-radius: 50%; background: #5470c6;
  box-shadow: 0 0 8px rgba(84,112,198,0.4); }
.app-title { font-size: 18px; font-weight: 700; color: #1a1a2e; margin: 0; }
.badge { font-size: 11px; color: #909399; background: #f0f2f5; padding: 2px 8px; border-radius: 4px; }
.topbar-right { display: flex; gap: 10px; align-items: center; }

/* 内容区 */
.content { max-width: 1400px; margin: 0 auto; padding: 20px 28px 40px; }

/* 卡片 */
.section-card { background: #fff; border-radius: 16px; box-shadow: 0 2px 16px rgba(0,0,0,0.05);
  margin-bottom: 16px; overflow: hidden; }
.section-head { font-size: 15px; font-weight: 600; color: #1a1a2e;
  padding: 16px 24px 0; display: flex; align-items: center; gap: 8px; }
.section-badge { font-size: 12px; color: #909399; font-weight: 400; margin-left: auto; }
.section-body { padding: 12px 24px 0; }
/* 表格 + 地图横排 */
.table-map-row { display: flex; gap: 16px; margin-bottom: 16px; align-items: stretch; }
.table-map-main { flex: 1; min-width: 0; }
.table-map-side { width: 260px; flex-shrink: 0; background: #fff; border-radius: 16px;
  box-shadow: 0 2px 16px rgba(0,0,0,0.05); overflow: hidden; }
.mini-map-head { font-size: 14px; font-weight: 600; color: #1a1a2e;
  padding: 14px 18px 0; display: flex; align-items: center; gap: 6px; }
@media (max-width: 1100px) { .table-map-row { flex-direction: column; } .table-map-side { width: 100%; } }

.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-card { background: #fff; border-radius: 16px; box-shadow: 0 2px 16px rgba(0,0,0,0.05);
  padding: 0 8px 8px; overflow: hidden; }
.chart-head { font-size: 14px; font-weight: 600; color: #1a1a2e;
  padding: 16px 16px 0; display: flex; align-items: center; gap: 6px; }
@media (max-width: 900px) { .chart-row { grid-template-columns: 1fr; } }
</style>
