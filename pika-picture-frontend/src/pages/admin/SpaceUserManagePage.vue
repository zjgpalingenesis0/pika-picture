<template>
  <div id="spaceManagePage">
    <a-flex justify="space-between">
      <h2>空间成员管理</h2>
      <a-space>
        <a-button type="primary" href="/add_space" target="_blank">+ 创建空间</a-button>
        <a-button type="primary" ghost href="/space_analyze?queryPublic=1" target="_blank"
          >分析公共图库
        </a-button>
        <a-button type="primary" ghost href="/space_analyze?queryAll=1" target="_blank"
          >分析全部空间
        </a-button>
      </a-space>
    </a-flex>
    <div style="margin-bottom: 16px" />
    <!-- 添加成员表单 -->
    <a-card title="添加成员" style="margin-bottom: 16px">
      <a-form layout="inline" :model="formData" @finish="handleSubmit">
        <a-form-item label="用户 id" name="userId">
          <a-input v-model:value="formData.userId" placeholder="请输入用户 id" allow-clear />
        </a-form-item>
        <a-form-item label="角色" name="spaceRole">
          <a-select
            v-model:value="formData.spaceRole"
            :options="SPACE_ROLE_OPTIONS"
            placeholder="请选择角色"
            style="width: 120px"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">添加用户</a-button>
        </a-form-item>
      </a-form>
    </a-card>
    <!-- 成员列表 -->
    <a-card title="成员列表">
      <!-- 搜索和筛选 -->
      <a-form layout="inline" style="margin-bottom: 16px">
        <a-form-item label="搜索">
          <a-input
            v-model:value="searchText"
            placeholder="搜索用户名或账号"
            allow-clear
            style="width: 200px"
            @change="onSearchChange"
          />
        </a-form-item>
        <a-form-item label="角色筛选">
          <a-select
            v-model:value="roleFilter"
            :options="[{ label: '全部', value: '' }, ...SPACE_ROLE_OPTIONS]"
            placeholder="全部角色"
            style="width: 120px"
            @change="onFilterChange"
          />
        </a-form-item>
      </a-form>
      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="filteredDataList"
        :row-selection="rowSelection"
        :pagination="pagination"
        :row-key="(record: API.SpaceUserVO) => record.id"
      >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userInfo'">
          <a-space>
            <a-avatar :src="record.user?.userAvatar" />
            {{ record.user?.userName }}
          </a-space>
        </template>
        <template v-if="column.dataIndex === 'spaceRole'">
          <a-tag :color="getRoleColor(record.spaceRole)">
            {{ SPACE_ROLE_MAP[record.spaceRole] || record.spaceRole }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'permissions'">
          <a-select
            v-model:value="record.spaceRole"
            :options="SPACE_ROLE_OPTIONS"
            size="small"
            @change="(value) => editSpaceRole(value, record)"
          >
            <template #suffixIcon>
              <EditOutlined />
            </template>
          </a-select>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space wrap>
            <a-button type="link" @click="doViewUser(record.user)">查看</a-button>
            <a-button type="link" danger @click="doDelete(record.id)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 批量操作栏 -->
    <div v-if="selectedRowKeys.length > 0" class="batch-actions">
      <a-space>
        <span>已选择 {{ selectedRowKeys.length }} 项</span>
        <a-button type="primary" size="small" @click="doBatchEditRole">批量修改角色</a-button>
        <a-button danger size="small" @click="doBatchDelete">批量删除</a-button>
      </a-space>
    </div>
    </a-card>
    <!-- 修改角色弹窗 -->
    <a-modal
      v-model:visible="batchEditVisible"
      title="批量修改角色"
      @ok="handleBatchEditRole"
    >
      <a-form layout="vertical">
        <a-form-item label="选择角色">
          <a-select
            v-model:value="batchRole"
            :options="SPACE_ROLE_OPTIONS"
            placeholder="请选择新角色"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { EditOutlined } from '@ant-design/icons-vue'
import { SPACE_ROLE_OPTIONS, SPACE_ROLE_MAP } from '../../constants/space.ts'
import {
  addSpaceUserUsingPost,
  deleteSpaceUserUsingPost,
  editSpaceUserUsingPost,
  listSpaceUserUsingPost,
} from '@/api/spaceUserController.ts'
import dayjs from 'dayjs'

interface Props {
  id: string
}

const props = defineProps<Props>()

const columns = [
  {
    title: '用户',
    dataIndex: 'userInfo',
  },
  {
    title: '角色',
    dataIndex: 'spaceRole',
  },
  {
    title: '权限列表',
    dataIndex: 'permissions',
    width: 200,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 定义数据
const dataList = ref<API.SpaceUserVO[]>([])

// 搜索文本
const searchText = ref<string>('')
// 角色筛选
const roleFilter = ref<string>('')

// 过滤后的数据列表
const filteredDataList = computed(() => {
  let filtered = dataList.value

  // 角色筛选
  if (roleFilter.value) {
    filtered = filtered.filter((item) => item.spaceRole === roleFilter.value)
  }

  // 文本搜索
  if (searchText.value) {
    const searchLower = searchText.value.toLowerCase()
    filtered = filtered.filter((item) => {
      const userName = item.user?.userName ?? ''
      const userAccount = item.user?.userAccount ?? ''
      return (
        userName.toLowerCase().includes(searchLower) ||
        userAccount.toLowerCase().includes(searchLower)
      )
    })
  }

  return filtered
})

// 行选择
const selectedRowKeys = ref<string[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: string[]) => {
    selectedRowKeys.value = keys
  },
}))

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

// 获取数据
const fetchData = async () => {
  const spaceId = props.id
  if (!spaceId) {
    return
  }
  const res = await listSpaceUserUsingPost({
    spaceId,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 页面加载时获取数据，请求一次
onMounted(() => {
  fetchData()
})

// 添加成员表单
const formData = reactive<API.SpaceUserAddRequest>({
  spaceRole: 'viewer', // 默认角色为浏览者
})

// 创建成员
const handleSubmit = async () => {
  const spaceId = props.id
  if (!spaceId) {
    return
  }
  if (!formData.userId) {
    message.warning('请输入用户 ID')
    return
  }
  const res = await addSpaceUserUsingPost({
    spaceId,
    ...formData,
  })
  if (res.data.code === 0) {
    message.success('添加成功')
    // 清空表单
    formData.userId = undefined
    // 刷新数据
    fetchData()
  } else {
    message.error('添加失败，' + res.data.message)
  }
}

// 编辑成员角色
const editSpaceRole = async (value, record) => {
  const res = await editSpaceUserUsingPost({
    id: record.id,
    spaceRole: value,
  })
  if (res.data.code === 0) {
    message.success('修改成功')
  } else {
    message.error('修改失败，' + res.data.message)
  }
}

// 删除数据
const doDelete = async (id: string) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该成员吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      const res = await deleteSpaceUserUsingPost({ id })
      if (res.data.code === 0) {
        message.success('删除成功')
        // 刷新数据
        fetchData()
      } else {
        message.error('删除失败')
      }
    },
  })
}

// 查看用户
const doViewUser = (user: API.UserVO) => {
  if (user?.id) {
    window.open(`/user/${user.id}`, '_blank')
  }
}

// 搜索变化
const onSearchChange = () => {
  // 触发计算属性重新计算
  pagination.current = 1
}

// 筛选变化
const onFilterChange = () => {
  pagination.current = 1
}

// 批量编辑角色弹窗
const batchEditVisible = ref(false)
const batchRole = ref<string>('')

// 打开批量编辑
const doBatchEditRole = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要修改的成员')
    return
  }
  batchEditVisible.value = true
}

// 执行批量编辑角色
const handleBatchEditRole = async () => {
  if (!batchRole.value) {
    message.warning('请选择角色')
    return
  }

  // 批量修改
  const promises = selectedRowKeys.value.map((id) =>
    editSpaceUserUsingPost({
      id: Number(id),
      spaceRole: batchRole.value,
    })
  )

  try {
    await Promise.all(promises)
    message.success(`成功修改 ${selectedRowKeys.value.length} 个成员的角色`)
    batchEditVisible.value = false
    selectedRowKeys.value = []
    fetchData()
  } catch (error) {
    message.error('批量修改失败')
  }
}

// 批量删除
const doBatchDelete = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要删除的成员')
    return
  }

  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个成员吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      const promises = selectedRowKeys.value.map((id) => deleteSpaceUserUsingPost({ id: Number(id) }))

      try {
        await Promise.all(promises)
        message.success(`成功删除 ${selectedRowKeys.value.length} 个成员`)
        selectedRowKeys.value = []
        fetchData()
      } catch (error) {
        message.error('批量删除失败')
      }
    },
  })
}

// 获取权限显示文本
const getPermissionText = (record: API.SpaceUserVO) => {
  const role = record.spaceRole
  if (!role) return '-'

  const roleConfig: any = SPACE_ROLE_OPTIONS.find((r) => r.value === role)
  if (roleConfig) {
    return SPACE_ROLE_MAP[role] || role
  }
  return role
}

// 获取角色标签颜色
const getRoleColor = (role: string) => {
  const colorMap: Record<string, string> = {
    admin: 'red',
    editor: 'blue',
    viewer: 'green',
  }
  return colorMap[role] || 'default'
}
</script>
