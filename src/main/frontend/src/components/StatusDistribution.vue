<template>
  <div ref="c" style="height:250px"></div>
</template>
<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
const props = defineProps({ data: Object })
const c = ref(null); let chart = null
const COLORS = ['#fac858','#5470c6','#67c23a','#f56c6c','#c0c4cc']
function render() {
  if (!c.value) return; if (!chart) chart = echarts.init(c.value)
  const items = props.data?.items || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left:12, right:20, top:10, bottom:24 },
    xAxis: { type:'category', data:items.map(i=>i.label), axisLabel:{fontSize:11} },
    yAxis: { type:'value', splitLine:{lineStyle:{color:'#f2f3f5'}} },
    series: [{ type:'bar', data:items.map((i,idx)=>({value:i.value,itemStyle:{color:i.color||COLORS[idx%5],borderRadius:[4,4,0,0]}})),
      barWidth:'50%', barGap:'30%' }]
  })
}
watch(() => props.data, () => nextTick(render), { deep: true })
onMounted(() => nextTick(render))
</script>
