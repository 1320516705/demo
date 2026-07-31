<template>
  <div ref="c" style="height:280px"></div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({ data: Object })
const c = ref(null)
let chart = null

function render() {
  if (!c.value) return
  if (!chart) chart = echarts.init(c.value)
  const pts = props.data?.points || []
  chart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e4e7ed',
      textStyle: { color: '#303133', fontSize: 13 },
      axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(84,112,198,0.06)' } } },
    legend: { data: ['可召回','高潜力','已触达','已同意'], bottom: 0, textStyle: { fontSize: 11 } },
    grid: { left: 48, right: 16, top: 20, bottom: 36 },
    xAxis: { type: 'category', data: pts.map(p => p.date?.substring(5) || p.date), axisLine: { lineStyle: { color: '#dcdfe6' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f2f3f5' } } },
    series: [
      { name:'可召回', type:'line', data:pts.map(p=>p.recallable), smooth:true, symbol:'circle', symbolSize:4,
        lineStyle:{width:2.5,color:'#5470c6'}, itemStyle:{color:'#5470c6'} },
      { name:'高潜力', type:'line', data:pts.map(p=>p.highPotential), smooth:true, symbol:'circle', symbolSize:4,
        lineStyle:{width:2,color:'#91cc75'}, itemStyle:{color:'#91cc75'} },
      { name:'已触达', type:'line', data:pts.map(p=>p.contacted), smooth:true, symbol:'diamond', symbolSize:4,
        lineStyle:{width:2,color:'#fac858'}, itemStyle:{color:'#fac858'} },
      { name:'已同意', type:'line', data:pts.map(p=>p.agreed), smooth:true, symbol:'triangle', symbolSize:4,
        lineStyle:{width:2,color:'#ee6666'}, itemStyle:{color:'#ee6666'} },
    ]
  })
}
watch(() => props.data, () => nextTick(render), { deep: true })
onMounted(() => nextTick(render))
</script>
