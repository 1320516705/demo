<template>
  <div class="dashboard">
    <!-- 顶部标题栏 -->
    <div class="top-bar">
      <div class="top-left">
        <h1 class="page-title">离线司机智能召回</h1>
        <el-tag size="small" effect="plain" type="success" v-if="store.mode === 'daily'">日常定时模式</el-tag>
        <el-tag size="small" effect="plain" type="warning" v-else>应急实时模式</el-tag>
      </div>
      <div class="top-right">
        <template v-if="store.mode === 'daily'">
          <el-date-picker v-model="store.currentDate" type="date" placeholder="选择日期"
            format="YYYY-MM-DD" value-format="YYYY-MM-DD" size="default"
            @change="refreshAll" style="width:170px" />
          <el-button type="primary" size="default" @click="triggerPipeline" :loading="pipelineLoading">
            <el-icon><VideoPlay /></el-icon> 运行管道
          </el-button>
          <el-button size="default" @click="refreshAll" :loading="refreshing">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </template>
        <template v-else>
          <el-button type="primary" size="default" @click="store.setMode('daily')">
            <el-icon><Switch /></el-icon> 切换日常模式
          </el-button>
        </template>
      </div>
    </div>

    <!-- KPI 卡片 -->
    <KpiCardRow v-if="store.mode === 'daily'" :kpi="store.kpiData" />

    <!-- 主区域: 地图 + 表格 -->
    <div class="main-row">
      <div class="panel map-wrapper">
        <div class="panel-title">
          <el-icon><Location /></el-icon> 司机分布热力图
          <span class="panel-badge" v-if="store.heatmapPoints.length">{{ store.heatmapPoints.length }} 个区域</span>
        </div>
        <DriverHeatMap
          :points="store.heatmapPoints"
          :loading="false"
          :isEmergency="store.mode === 'emergency'"
          @regionSelected="onRegionSelected" />
      </div>

      <div class="panel table-wrapper">
        <TableFilterBar :filters="store.filters" @change="onFilterChange" />
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

    <!-- 底部图表 -->
    <div class="chart-row" v-if="store.mode === 'daily'">
      <div class="panel">
        <div class="panel-title"><el-icon><TrendCharts /></el-icon> 近7天趋势</div>
        <TrendChart :data="store.trendData" />
      </div>
      <div class="panel">
        <div class="panel-title"><el-icon><PieChart /></el-icon> 人设分布</div>
        <PersonaDistribution :data="store.personaDistribution" />
      </div>
      <div class="panel">
        <div class="panel-title"><el-icon><Histogram /></el-icon> 触达状态</div>
        <StatusDistribution :data="store.statusDistribution" />
      </div>
    </div>

    <!-- 应急状态 -->
    <div v-if="store.mode === 'emergency' && store.emergencyStatus" class="emergency-bar">
      <el-alert :title="emergencyLabel[store.emergencyStatus]"
        :type="emergencyType[store.emergencyStatus]" :closable="false" show-icon center />
    </div>

    <!-- 触达弹窗 -->
    <OutreachDialog v-model="outreachVisible" :driver="selectedDriver"
      :driverIds="batchIds" :isBatch="isBatchOutreach" @confirm="doOutreach" />

    <!-- 详情抽屉 -->
    <DriverDetailDrawer v-model="detailVisible" :driver="selectedDriver" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRecallStore } from '@/stores/recall'
import KpiCardRow from '@/components/KpiCardRow.vue'
import DriverHeatMap from '@/components/DriverHeatMap.vue'
import DriverListTable from '@/components/DriverListTable.vue'
import TableFilterBar from '@/components/TableFilterBar.vue'
import TrendChart from '@/components/TrendChart.vue'
import PersonaDistribution from '@/components/PersonaDistribution.vue'
import StatusDistribution from '@/components/StatusDistribution.vue'
import OutreachDialog from '@/components/OutreachDialog.vue'
import DriverDetailDrawer from '@/components/DriverDetailDrawer.vue'
import { VideoPlay, Refresh, Switch, Location, TrendCharts, PieChart, Histogram } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const store = useRecallStore()
const outreachVisible = ref(false)
const detailVisible = ref(false)
const selectedDriver = ref(null)
const isBatchOutreach = ref(false)
const batchIds = ref([])
const pipelineLoading = ref(false)
const refreshing = ref(false)

const emergencyLabel = { QUERYING:'正在查询周边司机…', SCORING:'正在评估召回潜力…',
  STRATEGIZING:'正在生成召回策略…', COMPLETED:'应急召回完成！', FAILED:'应急召回失败' }
const emergencyType = { QUERYING:'info', SCORING:'info', STRATEGIZING:'info', COMPLETED:'success', FAILED:'error' }

onMounted(() => refreshAll())

async function refreshAll() {
  refreshing.value = true
  try {
    const d = store.currentDate
    await Promise.all([store.fetchKpi(d), store.fetchDriverList(d), store.fetchHeatmap(d),
      store.fetchTrend(7, d), store.fetchDistribution(d, 'persona'), store.fetchDistribution(d, 'status')])
  } finally { refreshing.value = false }
}

async function triggerPipeline() {
  pipelineLoading.value = true
  try {
    await axios.post('/api/v1/pipeline/trigger', { dataDate: store.currentDate })
    ElMessage.success('管道已启动，约1分钟后刷新查看结果')
    setTimeout(() => refreshAll(), 3000)
  } catch { ElMessage.error('触发失败') }
  finally { pipelineLoading.value = false }
}

function onFilterChange(f) { store.updateFilters(f); store.fetchDriverList() }
function onPageChange() { store.fetchDriverList() }
function onSortChange({ prop, order }) {
  store.sortField = prop || 'recallScore'
  store.sortOrder = order === 'ascending' ? 'asc' : 'desc'
  store.fetchDriverList()
}

function openDetail(row) { selectedDriver.value = row; detailVisible.value = true }
function openOutreach(row) { selectedDriver.value = row; isBatchOutreach.value = false; outreachVisible.value = true }
function openBatchOutreach(ids) { batchIds.value = ids; isBatchOutreach.value = true; outreachVisible.value = true }

async function doOutreach({ channel, remark }) {
  try {
    if (isBatchOutreach.value) {
      const res = await store.batchOutreach(batchIds.value, channel)
      ElMessage.success(`触达完成：成功 ${res.successCount} 条`)
    } else {
      await store.singleOutreach(selectedDriver.value.id, channel)
      ElMessage.success('触达成功')
    }
    refreshAll()
  } catch { ElMessage.error('触达失败') }
}

function onRegionSelected(polygon) { store.triggerEmergency(polygon, '圈选区域') }
</script>

<style scoped>
.dashboard { padding: 20px 24px; max-width: 1440px; margin: 0 auto; background: #f0f2f5; min-height: 100vh; }

/* 顶部 */
.top-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.top-left { display: flex; align-items: center; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0; letter-spacing: -0.5px; }
.top-right { display: flex; gap: 10px; align-items: center; }

/* 通用面板 */
.panel { background: #fff; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); overflow: hidden; }
.panel-title { font-size: 15px; font-weight: 600; color: #303133; padding: 14px 20px 0;
  display: flex; align-items: center; gap: 6px; }
.panel-badge { font-size: 12px; color: #909399; font-weight: 400; margin-left: auto; }

/* 主区域 */
.main-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; align-items: start; }
@media (max-width: 1100px) { .main-row { grid-template-columns: 1fr; } }
.map-wrapper { height: 520px; padding: 0; display: flex; flex-direction: column; }
.map-wrapper :deep(.heatmap-container) { flex: 1; }
.table-wrapper { height: 520px; display: flex; flex-direction: column; overflow: hidden; }
.table-wrapper > :first-child { flex-shrink: 0; padding: 14px 20px 0; }
.table-wrapper > :last-child { flex: 1; overflow: hidden; display: flex; flex-direction: column;
  box-shadow: none; border-radius: 0; padding: 0 20px 12px; }

/* 图表区 */
.chart-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.chart-row .panel { padding: 0 8px 8px; }
@media (max-width: 1100px) { .chart-row { grid-template-columns: 1fr; } }

.emergency-bar { margin-top: 16px; }
</style>
