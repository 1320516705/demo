<template>
  <div ref="c" style="height:250px"></div>
</template>
<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
const props = defineProps({ data: Object })
const c = ref(null); let chart = null
function render() {
  if (!c.value) return; if (!chart) chart = echarts.init(c.value)
  const items = (props.data?.items || []).filter(i => i.value > 0)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    series: [{ type:'pie', radius:['50%','78%'], center:['50%','52%'], avoidLabelOverlap:false,
      label:{ show:true, formatter:'{b}\n{d}%', fontSize:11 },
      data: items.map(i => ({ name:i.label, value:i.value, itemStyle:{ color:i.color || '#5470c6' } })) }]
  })
}
watch(() => props.data, () => nextTick(render), { deep: true })
onMounted(() => nextTick(render))
</script>
