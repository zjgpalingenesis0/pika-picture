<template>
  <div id="homePage">
    <!-- 搜索框 -->
    <div class="search-bar">
      <a-input-search
        v-model:value="searchParams.searchText"
        placeholder="从海量图片中搜索"
        enter-button="搜索"
        size="large"
        @search="doSearch"
      />
    </div>
    <!-- 分类和标签筛选 -->
    <a-tabs v-model:active-key="selectedCategory" @change="doSearch">
      <a-tab-pane key="all" tab="全部" />
      <a-tab-pane v-for="category in categoryList" :tab="category" :key="category" />
    </a-tabs>
    <div class="tag-bar">
      <span style="margin-right: 8px">标签：</span>
      <a-space :size="[0, 8]" wrap>
        <a-checkable-tag
          v-for="(tag, index) in tagList"
          :key="tag"
          v-model:checked="selectedTagList[index]"
          @change="doSearch"
        >
          {{ tag }}
        </a-checkable-tag>
      </a-space>
    </div>
    <!-- 图片列表 -->
    <PictureList :dataList="dataList" :loading="loading" />
    <!-- 分页 -->
    <a-pagination
      style="text-align: right"
      :current="searchParams.current"
      :pageSize="searchParams.pageSize"
      :total="total"
      @change="onPageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  listPictureTagCategoryUsingGet,
  listPictureVoByPageUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import PictureList from '@/components/PictureList.vue' // 定义数据

// 定义数据
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 10, // 🔧 修改为每页10张图片
  sortField: 'create_time', // 使用数据库字段名
  sortOrder: 'desc', // 使用数据库排序值
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    // 🔧 修复：避免覆盖已有参数，只在需要时初始化 tags
    if (!searchParams.tags) {
      searchParams.tags = [] as string[]
    } else {
      // 清空tags数组，但保持引用不变
      searchParams.tags.splice(0, searchParams.tags.length)
    }

    if (selectedCategory.value !== 'all') {
      searchParams.category = selectedCategory.value
    } else {
      searchParams.category = undefined
    }

    // [true, false, false] => ['java']
    selectedTagList.value.forEach((useTag, index) => {
      if (useTag) {
        searchParams.tags?.push(tagList.value[index])
      }
    })

    // 🔧 深拷贝一份参数，用于实际发送
    // 🔧 修复：直接构造简单的请求对象，避免复杂响应式对象的问题
    const requestData = {
      current: Number(searchParams.current) || 1,
      pageSize: Number(searchParams.pageSize) || 8,
      sortField: 'create_time', // 使用数据库字段名而不是Java字段名
      sortOrder: 'desc', // 使用数据库排序值而不是前端排序值
    }

    // 🔧 修复：只有 tags 有值时才添加
    if (searchParams.tags && searchParams.tags.length > 0) {
      (requestData as any).tags = [...searchParams.tags]
    }
    if (searchParams.category) {
      (requestData as any).category = searchParams.category
    }
    if (searchParams.searchText) {
      (requestData as any).searchText = searchParams.searchText
    }

    // 🔍 调试：打印实际发送的参数
    console.log('🚀 发送请求前的最终参数:', JSON.stringify(requestData, null, 2))
    console.log('📊 参数类型检查:')
    console.log('  - current:', requestData.current, '类型:', typeof requestData.current)
    console.log('  - pageSize:', requestData.pageSize, '类型:', typeof requestData.pageSize)
    console.log('  - sortField:', requestData.sortField, '类型:', typeof requestData.sortField)
    console.log('  - sortOrder:', requestData.sortOrder, '类型:', typeof requestData.sortOrder)

    const res = await listPictureVoByPageUsingPost(requestData as API.PictureQueryRequest)

    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records ?? []
      // 🔧 修复：确保 total 是数字类型
      total.value = Number(res.data.data.total ?? 0)
      // 🔍 调试：打印返回的数据
      console.log('===== 接口返回数据 =====')
      console.log('records 数量:', res.data.data.records?.length)
      console.log('records 前三张图片 ID:', res.data.data.records?.slice(0, 3).map(p => p.id))
      console.log('total:', res.data.data.total)
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取数据异常:', error)
    message.error('获取数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 页面加载时获取数据，请求一次
onMounted(() => {
  fetchData()
})

// 搜索
const doSearch = () => {
  // 重置搜索条件
  searchParams.current = 1
  fetchData()
}

// 分页变化处理
const onPageChange = (page: number, pageSize: number) => {
  console.log('分页变化:', { page, pageSize })

  // 强制刷新数据
  searchParams.current = page
  searchParams.pageSize = pageSize

  // 清空当前数据，避免显示旧数据
  dataList.value = []

  // 添加时间戳确保每次请求都是唯一的
  console.log('强制刷新分页数据')

  fetchData()
}

// 标签和分类列表
const categoryList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const tagList = ref<string[]>([])
const selectedTagList = ref<boolean[]>([])

/**
 * 获取标签和分类选项
 * @param values
 */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagList.value = res.data.data.tagList ?? []
    categoryList.value = res.data.data.categoryList ?? []
  } else {
    message.error('获取标签分类列表失败，' + res.data.message)
  }
}

onMounted(() => {
  getTagCategoryOptions()
})
</script>

<style scoped>
#homePage {
  margin-bottom: 16px;
}

#homePage .search-bar {
  max-width: 480px;
  margin: 0 auto 16px;
}

#homePage .tag-bar {
  margin-bottom: 16px;
}
</style>
