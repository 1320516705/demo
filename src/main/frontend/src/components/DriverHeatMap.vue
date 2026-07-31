<template>
  <div class="heatmap-container">
    <div v-if="loading" class="loading-overlay">
      <el-icon class="is-loading"><Loading /></el-icon>
    </div>
    <div v-if="isEmergency" class="draw-toolbar">
      <el-button type="primary" size="small" @click="startDraw" :disabled="drawing">
        <el-icon><EditPen /></el-icon>圈选区域
      </el-button>
      <el-button size="small" @click="clearDraw" v-if="hasPolygon">清除</el-button>
    </div>
    <div id="heatmap-map" style="width:100%;height:100%;min-height:420px"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { Loading, EditPen } from '@element-plus/icons-vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const props = defineProps({
  points: { type: Array, default: () => [] },
  loading: Boolean,
  isEmergency: Boolean
})
const emit = defineEmits(['regionSelected'])

const drawing = ref(false)
const hasPolygon = ref(false)
let map = null
let heatLayer = null
let drawnItems = null

onMounted(async () => {
  await nextTick()
  initMap()
})

function initMap() {
  map = L.map('heatmap-map', {
    center: [31.2304, 121.4737],  // 上海市中心
    zoom: 11,
    zoomControl: true
  })

  // 免费 OpenStreetMap 瓦片，无需 API Key
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
    maxZoom: 18
  }).addTo(map)

  if (props.isEmergency) {
    drawnItems = new L.FeatureGroup()
    map.addLayer(drawnItems)
  }

  renderHeatmap()
}

function renderHeatmap() {
  if (!map || !props.points.length) return

  // 按 count 值分级：越大颜色越暖
  const maxCount = Math.max(...props.points.map(p => p.count || 1), 1)

  const heatPoints = props.points.map(p => {
    const intensity = (p.count || 1) / maxCount
    // 颜色从绿(冷)到红(热)
    const hue = 120 - intensity * 120  // 120°=绿, 0°=红
    const color = `hsl(${hue}, 80%, ${40 + intensity * 20}%)`
    return {
      lat: p.lat,
      lng: p.lng,
      count: p.count,
      avgScore: p.avgScore,
      radius: 12 + intensity * 25,
      color
    }
  })

  // 清除旧图层
  if (heatLayer) map.removeLayer(heatLayer)
  heatLayer = L.layerGroup()

  heatPoints.forEach(p => {
    const circle = L.circle([p.lat, p.lng], {
      radius: p.radius * 20,  // 米
      color: p.color,
      fillColor: p.color,
      fillOpacity: 0.45,
      weight: 1.5
    })
    circle.bindTooltip(
      `司机数: ${p.count}<br>均召回分: ${p.avgScore ? Number(p.avgScore).toFixed(1) : '-'}`,
      { direction: 'top', offset: [0, -p.radius * 0.8] }
    )
    circle.addTo(heatLayer)
  })

  heatLayer.addTo(map)
}

function startDraw() {
  if (!drawnItems) return
  drawing.value = true
  // Leaflet 不支持直接多边形绘制，使用点击模式
  const polygonPoints = []
  const tempMarkers = []

  map.on('click', function onClick(e) {
    polygonPoints.push([e.latlng.lat, e.latlng.lng])
    const marker = L.circleMarker(e.latlng, { radius: 4, color: '#409EFF', fillOpacity: 0.8 })
    marker.addTo(drawnItems)
    tempMarkers.push(marker)

    if (polygonPoints.length >= 3) {
      // 绘制预览多边形
      L.polygon(polygonPoints, { color: '#409EFF', fillOpacity: 0.1, dashArray: '5,5' }).addTo(drawnItems)
    }
  })

  // 双击结束绘制
  map.on('dblclick', function onDblClick() {
    map.off('click')
    map.off('dblclick')
    drawing.value = false
    hasPolygon.value = true
    if (polygonPoints.length >= 3) {
      polygonPoints.push(polygonPoints[0])  // 闭合
      emit('regionSelected', polygonPoints.map(p => [p[1], p[0]]))  // [lng, lat] 格式
    }
  })
}

function clearDraw() {
  if (drawnItems) drawnItems.clearLayers()
  hasPolygon.value = false
}

watch(() => props.points, () => nextTick(renderHeatmap), { deep: true })
</script>

<style scoped>
.heatmap-container { position: relative; width: 100%; height: 100%; border-radius: 0 0 12px 12px; overflow: hidden; }
.loading-overlay { position: absolute; z-index: 1000; inset: 0; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.7); font-size: 32px; }
.draw-toolbar { position: absolute; z-index: 1000; top: 8px; left: 50px; display: flex; gap: 8px; }
</style>
