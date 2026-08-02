import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as recallApi from '@/api/recall'
import * as kpiApi from '@/api/kpi'
import * as chartApi from '@/api/chart'
import * as emergencyApi from '@/api/emergency'
import axios from 'axios'

export const useRecallStore = defineStore('recall', () => {
  // --- State ---
  // 默认日期取 mock 数据日期 2026-07-29，后续可切换
  const currentDate = ref('2026-07-29')
  const mode = ref('daily') // 'daily' | 'emergency'

  // KPI
  const kpiData = ref(null)
  const kpiLoading = ref(false)

  // Driver list
  const driverList = ref([])
  const driverListPage = ref({ page: 1, size: 20, total: 0 })
  const driverListLoading = ref(false)
  const filters = ref({ outreachStatus: '', scoreRange: [0, 100], keyword: '' })
  const sortField = ref('recallScore')
  const sortOrder = ref('desc')

  // Charts
  const cityHeatData = ref([])
  const trendData = ref(null)
  const statusDistribution = ref(null)

  // Emergency
  const emergencySessionId = ref(null)
  const emergencyStatus = ref(null)
  const emergencyResults = ref([])
  let emergencyPollTimer = null

  // --- Actions ---
  async function fetchKpi(date) {
    kpiLoading.value = true
    try {
      const res = await kpiApi.getKpiSummary(date || currentDate.value)
      kpiData.value = res.data.data
    } finally {
      kpiLoading.value = false
    }
  }

  async function fetchDriverList(date, pag, sort) {
    driverListLoading.value = true
    try {
      const page = pag?.page || driverListPage.value.page
      const size = pag?.size || driverListPage.value.size
      const sortF = sort?.sortField || sortField.value
      const sortO = sort?.sortOrder || sortOrder.value

      const params = {
        dataDate: date || currentDate.value,
        page, size,
        sort: sortF,
        order: sortO
      }
      if (filters.value.outreachStatus) params.outreachStatus = filters.value.outreachStatus
      if (filters.value.keyword) params.keyword = filters.value.keyword
      if (filters.value.scoreRange[0] > 0) params.scoreMin = filters.value.scoreRange[0]
      if (filters.value.scoreRange[1] < 100) params.scoreMax = filters.value.scoreRange[1]

      const res = await recallApi.getRecallList(params)
      const data = res.data.data
      driverList.value = data.records || []
      driverListPage.value = { page: data.page, size: data.size, total: data.total }
    } finally {
      driverListLoading.value = false
    }
  }

  async function fetchCityHeatmap(date) {
    try {
      const res = await axios.get('/api/v1/chart/city-heatmap', { params: { dataDate: date || currentDate.value } })
      cityHeatData.value = res.data.data || []
    } catch (e) { /* ignore */ }
  }

  async function fetchTrend(days, endDate) {
    try {
      const res = await chartApi.getTrendChart(days || 7, endDate || currentDate.value)
      trendData.value = res.data.data
    } catch (e) { /* ignore */ }
  }

  async function fetchDistribution(date, type) {
    try {
      const res = await chartApi.getDistribution(date || currentDate.value, type)
      const data = res.data.data
      if (type === 'status') statusDistribution.value = data
    } catch (e) { /* ignore */ }
  }

  async function singleOutreach(id, channel) {
    await recallApi.outreachDriver(id, { channel })
    await fetchDriverList()
  }

  async function batchOutreach(ids, channel) {
    const res = await recallApi.batchOutreach({ ids, channel })
    await fetchDriverList()
    return res.data.data
  }

  async function triggerEmergency(polygon, regionDesc) {
    const res = await emergencyApi.triggerEmergencyRecall({ polygon, regionDesc })
    emergencySessionId.value = res.data.data.sessionId
    emergencyStatus.value = res.data.data.status
    emergencyResults.value = []
    startEmergencyPoll()
  }

  function startEmergencyPoll() {
    stopEmergencyPoll()
    emergencyPollTimer = setInterval(async () => {
      if (!emergencySessionId.value) return
      try {
        const res = await emergencyApi.getEmergencyStatus(emergencySessionId.value)
        emergencyStatus.value = res.data.data.status
        if (res.data.data.status === 'COMPLETED' || res.data.data.status === 'FAILED') {
          stopEmergencyPoll()
          if (res.data.data.status === 'COMPLETED') {
            const resultRes = await emergencyApi.getEmergencyResult(emergencySessionId.value)
            emergencyResults.value = JSON.parse(resultRes.data.data.resultData || '[]')
          }
        }
      } catch (e) { stopEmergencyPoll() }
    }, 3000)
  }

  function stopEmergencyPoll() {
    if (emergencyPollTimer) { clearInterval(emergencyPollTimer); emergencyPollTimer = null }
  }

  function updateFilters(newFilters) { Object.assign(filters.value, newFilters) }
  function resetFilters() { filters.value = { outreachStatus: '', scoreRange: [0, 100], keyword: '' } }
  function setDate(date) { currentDate.value = date }
  function setMode(m) { mode.value = m }

  return {
    currentDate, mode, kpiData, kpiLoading,
    driverList, driverListPage, driverListLoading, filters, sortField, sortOrder,
    cityHeatData, trendData, statusDistribution,
    emergencySessionId, emergencyStatus, emergencyResults,
    fetchKpi, fetchDriverList, fetchCityHeatmap, fetchTrend, fetchDistribution,
    singleOutreach, batchOutreach,
    triggerEmergency, startEmergencyPoll, stopEmergencyPoll,
    updateFilters, resetFilters, setDate, setMode
  }
})
