<template>
  <div class="space-user-analyze">
    <!-- 加载状态 -->
    <div v-if="loading" style="text-align: center; padding: 40px;">
      <a-spin size="large" />
      <div style="margin-top: 16px;">正在加载数据...</div>
    </div>

    <!-- 数据展示 -->
    <a-flex v-else gap="middle">
      <a-card title="存储空间" style="width: 50%">
        <div style="height: 320px; text-align: center">
          <h3>
            {{ formatSize(data.usedSize) }} /
            {{ data.maxSize ? formatSize(data.maxSize) : '无限制' }}
          </h3>
          <a-progress type="dashboard" :percent="data.sizeUsageRatio ?? 0" />
        </div>
      </a-card>
      <a-card title="图片数量" style="width: 50%">
        <div style="height: 320px; text-align: center">
          <h3>
            {{ data.usedCount }} / {{ data.maxCount ?? '无限制' }}
          </h3>
          <a-progress type="dashboard" :percent="data.countUsageRatio ?? 0" />
        </div>
      </a-card>
    </a-flex>
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { getSpaceUsageAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'
import { formatSize } from '@/utils'

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: string | undefined
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

// 图表数据
const data = ref<API.SpaceUsageAnalyzeResponse>({})
// 加载状态
const loading = ref(true)

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    // 构建请求参数，只有明确为 true 时才传递
    const params: any = {}
    if (props.queryAll === true) {
      params.queryAll = true
    }
    if (props.queryPublic === true) {
      params.queryPublic = true
    }
    if (props.spaceId !== undefined) {
      params.spaceId = props.spaceId
    }
    console.log('SpaceUsageAnalyze 发送请求参数:', params)
    const res = await getSpaceUsageAnalyzeUsingPost(params)
    console.log('SpaceUsageAnalyze 响应:', res.data)
    if (res.data.code === 0) {
      data.value = res.data.data || {}
    } else {
      const errorMsg = `获取数据失败，${res.data.message}`
      console.error('SpaceUsageAnalyze: ' + errorMsg)
      message.error(errorMsg)
    }
  } catch (error) {
    console.error('SpaceUsageAnalyze: 请求数据异常', error)
    message.error('请求数据异常，请检查网络连接或联系管理员')
  } finally {
    loading.value = false
  }
}

/**
 * 监听变量，参数改变时触发数据的重新加载
 */
watchEffect(() => {
  fetchData()
})
</script>

<style scoped></style>
