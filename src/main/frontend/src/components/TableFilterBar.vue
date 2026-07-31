<template>
  <div class="filter-bar">
    <el-select v-model="f.personaTag" placeholder="人设标签" clearable size="default" style="width:140px" @change="doEmit">
      <el-option v-for="t in personaOptions" :key="t.value" :label="t.label" :value="t.value" />
    </el-select>
    <el-select v-model="f.outreachStatus" placeholder="触达状态" clearable size="default" style="width:130px" @change="doEmit">
      <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
    </el-select>
    <el-input v-model="f.keyword" placeholder="搜索姓名/手机号" clearable size="default" style="width:190px"
      @clear="doEmit" @keyup.enter="doEmit">
      <template #prefix><el-icon><Search /></el-icon></template>
    </el-input>
    <el-button type="primary" size="default" @click="doEmit" :icon="Search">查询</el-button>
    <el-button size="default" @click="reset">重置</el-button>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps({ filters: Object })
const emit = defineEmits(['change'])

const f = reactive({
  personaTag: props.filters?.personaTag || '',
  outreachStatus: props.filters?.outreachStatus || '',
  keyword: props.filters?.keyword || ''
})

watch(() => props.filters, v => { if (v) Object.assign(f, v) }, { deep: true, immediate: true })

const personaOptions = [
  { label:'价格敏感型', value:'PRICE_SENSITIVE' }, { label:'时间敏感型', value:'TIME_SENSITIVE' },
  { label:'顺路回家型', value:'WAY_HOME' }, { label:'周末兼职型', value:'WEEKEND_PART_TIME' },
  { label:'稳定全职型', value:'STABLE_FULL_TIME' }
]
const statusOptions = [
  { label:'待触达', value:'PENDING' }, { label:'已触达', value:'CONTACTED' },
  { label:'已同意', value:'AGREED' }, { label:'已拒绝', value:'DECLINED' }, { label:'无响应', value:'NO_RESPONSE' }
]

function doEmit() { emit('change', { ...f }) }
function reset() { f.personaTag = ''; f.outreachStatus = ''; f.keyword = ''; doEmit() }
</script>

<style scoped>
.filter-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
</style>
