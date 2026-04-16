<template>
  <div id="pictureDetailPage">
    <!-- 实时编辑状态提示 -->
    <a-alert
      v-if="editingUser"
      :message="`${editingUser.userName} 正在编辑此图片`"
      type="info"
      show-icon
      closable
      style="margin-bottom: 16px"
    >
      <template #description>
        <a-space>
          <a-avatar :size="24" :src="editingUser.userAvatar" />
          <span>{{ editingUser.userName }}</span>
          <span>正在编辑此图片</span>
        </a-space>
      </template>
    </a-alert>
    <!-- 协作在线用户列表 -->
    <div v-if="onlineUsers.length > 0" class="online-users">
      <a-avatar-group>
        <a-tooltip v-for="user in onlineUsers" :key="user.id" :title="user.userName">
          <a-avatar :size="32" :src="user.userAvatar" />
        </a-tooltip>
      </a-avatar-group>
      <span class="online-count">{{ onlineUsers.length }} 人在线</span>
    </div>
    <a-row :gutter="[16, 16]">
      <!-- 图片预览 -->
      <a-col :sm="24" :md="16" :xl="18">
        <a-card title="图片预览">
          <a-image :src="picture.url" style="max-height: 600px; object-fit: contain" />
        </a-card>
      </a-col>
      <!-- 图片信息区域 -->
      <a-col :sm="24" :md="8" :xl="6">
        <a-card title="图片信息">
          <a-descriptions :column="1">
            <a-descriptions-item label="作者">
              <a-space>
                <a-avatar :size="24" :src="picture.user?.userAvatar" />
                <div>{{ picture.user?.userName }}</div>
              </a-space>
            </a-descriptions-item>
            <a-descriptions-item label="名称">
              {{ picture.name ?? '未命名' }}
            </a-descriptions-item>
            <a-descriptions-item label="简介">
              {{ picture.introduction ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="分类">
              {{ picture.category ?? '默认' }}
            </a-descriptions-item>
            <a-descriptions-item label="标签">
              <a-tag v-for="tag in picture.tags" :key="tag">
                {{ tag }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="格式">
              {{ picture.picFormat ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="宽度">
              {{ picture.picWidth ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="高度">
              {{ picture.picHeight ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="宽高比">
              {{ picture.picScale ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="大小">
              {{ formatSize(picture.picSize) }}
            </a-descriptions-item>
            <a-descriptions-item label="主色调">
              <a-space>
                {{ picture.picColor ?? '-' }}
                <div
                  v-if="picture.picColor"
                  :style="{
                    width: '16px',
                    height: '16px',
                    backgroundColor: toHexColor(picture.picColor),
                  }"
                />
              </a-space>
            </a-descriptions-item>
          </a-descriptions>
          <!-- 图片操作 -->
          <a-space wrap>
            <a-button type="primary" @click="doDownload">
              免费下载
              <template #icon>
                <DownloadOutlined />
              </template>
            </a-button>
            <a-button :icon="h(ShareAltOutlined)" type="primary" ghost @click="doShare">
              分享
            </a-button>
            <a-button v-if="canEdit" :icon="h(EditOutlined)" type="default" @click="doEdit">
              编辑
            </a-button>
            <a-button v-if="canDelete" :icon="h(DeleteOutlined)" danger @click="doDelete">
              删除
            </a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
    <ShareModal ref="shareModalRef" :link="shareLink" />
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, onUnmounted, ref } from 'vue'
import { deletePictureUsingPost, getPictureVoByIdUsingGet } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import {
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  ShareAltOutlined,
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { downloadImage, formatSize, toHexColor } from '@/utils'
import ShareModal from '@/components/ShareModal.vue'
import { SPACE_PERMISSION_ENUM } from '@/constants/space.ts'
import PictureEditWebSocket from '@/utils/pictureEditWebSocket.ts'

interface Props {
  id: string | number
}

const props = defineProps<Props>()
const picture = ref<API.PictureVO>({})

// WebSocket 相关
let ws: PictureEditWebSocket | null = null
// 当前正在编辑的用户
const editingUser = ref<API.UserVO>()
// 在线用户列表
const onlineUsers = ref<API.UserVO[]>([])

// 通用权限检查函数
function createPermissionChecker(permission: string) {
  return computed(() => {
    return (picture.value.permissionList ?? []).includes(permission)
  })
}

// 定义权限检查
const canEdit = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDelete = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)

// 获取图片详情
const fetchPictureDetail = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({
      id: props.id,
    })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
      // 获取图片详情后连接 WebSocket
      initWebSocket()
    } else {
      message.error('获取图片详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取图片详情失败：' + e.message)
  }
}

// 初始化 WebSocket
const initWebSocket = () => {
  if (!props.id) {
    return
  }

  ws = new PictureEditWebSocket(Number(props.id))

  // 监听连接成功
  ws.on('open', () => {
    console.log('WebSocket 连接成功')
  })

  // 监听用户进入编辑
  ws.on('ENTER_EDIT', (data: any) => {
    const user = data.user
    if (user) {
      editingUser.value = user
      message.info(`${user.userName} 开始编辑图片`)
    }
  })

  // 监听编辑操作
  ws.on('EDIT_ACTION', (data: any) => {
    const user = data.user
    const action = data.editAction
    if (user) {
      // 可以根据 action 类型显示不同的提示
      console.log(`${user.userName} 执行了操作: ${action}`)
    }
  })

  // 监听用户退出编辑
  ws.on('EXIT_EDIT', (data: any) => {
    const user = data.user
    if (user && editingUser.value?.id === user.id) {
      editingUser.value = undefined
      message.info(`${user.userName} 退出编辑`)
    }
  })

  // 监听用户加入
  ws.on('INFO', (data: any) => {
    if (data.message && data.user) {
      message.success(data.message)
      // 更新在线用户列表
      updateOnlineUsers(data.user, 'add')
    }
  })

  // 监听连接关闭
  ws.on('close', () => {
    console.log('WebSocket 连接关闭')
  })

  // 监听错误
  ws.on('error', (error: any) => {
    console.error('WebSocket 错误:', error)
  })

  // 连接 WebSocket
  ws.connect()
}

// 更新在线用户列表
const updateOnlineUsers = (user: API.UserVO, action: 'add' | 'remove') => {
  if (action === 'add') {
    if (!onlineUsers.value.find((u) => u.id === user.id)) {
      onlineUsers.value.push(user)
    }
  } else if (action === 'remove') {
    onlineUsers.value = onlineUsers.value.filter((u) => u.id !== user.id)
  }
}

// 组件挂载时获取数据
onMounted(() => {
  fetchPictureDetail()
})

// 组件卸载时断开 WebSocket
onUnmounted(() => {
  if (ws) {
    ws.disconnect()
    ws = null
  }
})

const router = useRouter()

// 编辑
const doEdit = () => {
  router.push({
    path: '/add_picture',
    query: {
      id: picture.value.id,
      spaceId: picture.value.spaceId,
    },
  })
}

// 删除数据
const doDelete = async () => {
  const id = picture.value.id
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
  } else {
    message.error('删除失败')
  }
}

// 下载图片
const doDownload = () => {
  downloadImage(picture.value.url)
}

// ----- 分享操作 ----
const shareModalRef = ref()
// 分享链接
const shareLink = ref<string>()
// 分享
const doShare = () => {
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.value.id}`
  if (shareModalRef.value) {
    shareModalRef.value.openModal()
  }
}
</script>

<style scoped>
#pictureDetailPage {
  margin-bottom: 16px;
}

.online-users {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 8px;
}

.online-count {
  font-size: 14px;
  color: #666;
  margin-left: 8px;
}
</style>
