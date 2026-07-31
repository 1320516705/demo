<template>
  <el-drawer v-model="visible" title="司机召回详情" size="480px" @close="$emit('close')">
    <template v-if="driver">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="姓名">{{ driver.driverName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ driver.phone }}</el-descriptions-item>
        <el-descriptions-item label="召回潜力分">
          <el-tag :type="scoreTagType">{{ driver.recallScore }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="人设标签">{{ driver.personaTagLabel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="推荐渠道">{{ driver.recommendedChannelLabel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="触达状态">{{ driver.outreachStatusLabel || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="section-title">个性化召回话术</div>
      <el-alert :closable="false" type="success">
        {{ driver.strategyScript || '暂未生成召回话术' }}
      </el-alert>
      <div v-if="driver.llmResponseRaw" class="section-title">LLM 原始输出</div>
      <el-input v-if="driver.llmResponseRaw" :model-value="formatJson(driver.llmResponseRaw)" type="textarea"
        :rows="8" readonly class="raw-output" />
    </template>
  </el-drawer>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ modelValue: Boolean, driver: Object })
defineEmits(['update:modelValue', 'close'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => undefined
})

function scoreTagType() {
  const s = Number(props.driver?.recallScore)
  if (s >= 80) return 'success'
  if (s >= 60) return 'warning'
  return 'danger'
}

function formatJson(raw) {
  try { return JSON.stringify(JSON.parse(raw), null, 2) } catch { return raw }
}
</script>

<style scoped>
.section-title { font-weight: 600; margin: 16px 0 8px; font-size: 14px; }
.raw-output { margin-top: 8px; }
</style>
