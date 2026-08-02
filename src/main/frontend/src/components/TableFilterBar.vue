<template>
  <div class="filter-bar">
    <el-select v-model="f.outreachStatus" placeholder="触达状态" clearable size="default" style="width:130px" @change="doEmit">
      <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
    </el-select>
    <el-input v-model="f.keyword" placeholder="搜索姓名/手机号" clearable size="default" style="width:200px"
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

const f = reactive({ outreachStatus: props.filters?.outreachStatus || '', keyword: props.filters?.keyword || '' })
watch(() => props.filters, v => { if (v) Object.assign(f, v) }, { deep: true, immediate: true })

const statusOptions = [
  { label:'待触达', value:'PENDING' }, { label:'已触达', value:'CONTACTED' },
  { label:'已同意', value:'AGREED' }, { label:'已拒绝', value:'DECLINED' }, { label:'无响应', value:'NO_RESPONSE' }
]

function doEmit() { emit('change', { ...f }) }
function reset() { f.outreachStatus = ''; f.keyword = ''; doEmit() }
</script>

<style scoped>
.filter-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
</style>
