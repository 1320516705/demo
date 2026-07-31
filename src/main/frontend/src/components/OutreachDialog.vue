<template>
  <el-dialog v-model="vis" title="确认触达" width="480px" @close="$emit('close')" destroy-on-close>
    <el-form label-width="80px">
      <el-form-item label="触达对象">
        <span v-if="isBatch" class="target-text">
          <el-tag size="small" type="primary" effect="plain">{{ driverIds?.length || 0 }}</el-tag> 位司机
        </span>
        <span v-else class="target-text">
          <strong>{{ driver?.driverName }}</strong> &nbsp; {{ driver?.phone }}
        </span>
      </el-form-item>
      <el-form-item label="触达渠道">
        <el-radio-group v-model="channel">
          <el-radio value="SMS" size="large">短信</el-radio>
          <el-radio value="PHONE" size="large">外呼</el-radio>
          <el-radio value="APP_PUSH" size="large">App推送</el-radio>
          <el-radio value="WECHAT" size="large">微信</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="话术预览" v-if="!isBatch && driver?.strategyScript">
        <el-input :model-value="driver.strategyScript" type="textarea" :rows="3" readonly />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="remark" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="vis = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">
        <el-icon><Promotion /></el-icon> 确认触达
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Promotion } from '@element-plus/icons-vue'

const props = defineProps({ modelValue: Boolean, driver: Object, driverIds: Array, isBatch: Boolean })
const emit = defineEmits(['update:modelValue', 'confirm', 'close'])

const vis = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })
const channel = ref('SMS')
const remark = ref('')
const submitting = ref(false)

async function submit() {
  submitting.value = true
  try { await emit('confirm', { channel: channel.value, remark: remark.value }) }
  finally { submitting.value = false; vis.value = false }
}
</script>

<style scoped>
.target-text { font-size: 14px; color: #303133; }
</style>
