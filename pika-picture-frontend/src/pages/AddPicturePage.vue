<template>
  <div id="addPicturePage">
    <h2 style="margin-bottom: 16px">
      {{ route.query?.id ? '修改图片' : '创建图片' }}
    </h2>
    <!-- 编辑状态提示 -->
    <a-alert
      v-if="isEditing && editingUser"
      :message="`${editingUser.userName} 正在编辑此图片`"
      type="warning"
      show-icon
      closable
      style="margin-bottom: 16px"
    >
      <template #description>
        <a-space>
          <span>为了防止冲突，建议等待</span>
          <a-tag color="warning">{{ editingUser.userName }}</a-tag>
          <span>完成编辑后再操作</span>
        </a-space>
      </template>
    </a-alert>
    <a-typography-paragraph v-if="spaceId" type="secondary">
      保存至空间：<a :href="`/space/${spaceId}`" target="_blank">{{ spaceId }}</a>
    </a-typography-paragraph>

    <!-- 左右分栏布局 -->
    <div class="split-layout">
      <!-- 左侧：上传区域 -->
      <div class="left-panel">
        <a-card title="上传图片" :bordered="false">
          <!-- 选择上传方式 -->
          <a-tabs v-model:activeKey="uploadType">
            <a-tab-pane key="file" tab="文件上传">
              <!-- 图片上传组件 -->
              <PictureUpload :picture="picture" :spaceId="spaceId" :onSuccess="onSuccess" />
            </a-tab-pane>
            <a-tab-pane key="url" tab="URL 上传" force-render>
              <!-- URL 图片上传组件 -->
              <UrlPictureUpload :picture="picture" :spaceId="spaceId" :onSuccess="onSuccess" />
            </a-tab-pane>
          </a-tabs>

          <!-- 图片编辑工具栏 -->
          <div v-if="picture" class="edit-bar">
            <a-space size="middle" direction="vertical" style="width: 100%">
              <a-button :icon="h(EditOutlined)" @click="doEditPicture" :disabled="isEditing && !isCurrentUserEditing" block>
                {{ isCurrentUserEditing ? '继续编辑' : '进入编辑' }}
              </a-button>
              <a-button
                v-if="isCurrentUserEditing"
                type="primary"
                danger
                @click="doExitEdit"
                block
              >
                退出编辑
              </a-button>
              <a-button type="primary" :icon="h(FullscreenOutlined)" @click="doImagePainting" :disabled="isEditing && !isCurrentUserEditing" block>
                AI 扩图
              </a-button>
            </a-space>
            <ImageCropper
              ref="imageCropperRef"
              :imageUrl="picture?.url"
              :picture="picture"
              :spaceId="spaceId"
              :space="space"
              :onSuccess="onCropSuccess"
            />
            <ImageOutPainting
              ref="imageOutPaintingRef"
              :picture="picture"
              :spaceId="spaceId"
              :onSuccess="onImageOutPaintingSuccess"
            />
          </div>
        </a-card>
      </div>

      <!-- 右侧：编辑表单 -->
      <div class="right-panel">
        <a-card title="图片信息" :bordered="false">
          <!-- 图片信息表单 -->
          <a-form
            v-if="picture"
            name="pictureForm"
            layout="vertical"
            :model="pictureForm"
            @finish="handleSubmit"
          >
            <a-form-item name="name" label="名称">
              <a-input v-model:value="pictureForm.name" placeholder="请输入名称" allow-clear />
            </a-form-item>
            <a-form-item name="introduction" label="简介">
              <a-textarea
                v-model:value="pictureForm.introduction"
                placeholder="请输入简介"
                :auto-size="{ minRows: 3, maxRows: 6 }"
                allow-clear
              />
            </a-form-item>
            <a-form-item name="category" label="分类">
              <a-auto-complete
                v-model:value="pictureForm.category"
                placeholder="请输入分类"
                :options="categoryOptions"
                allow-clear
              />
            </a-form-item>
            <a-form-item name="tags" label="标签">
              <a-select
                v-model:value="pictureForm.tags"
                mode="tags"
                placeholder="请输入标签"
                :options="tagOptions"
                allow-clear
              />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" size="large" block>
                {{ route.query?.id ? '保存修改' : '保存信息' }}
              </a-button>
            </a-form-item>
          </a-form>
          <!-- 未上传时的提示 -->
          <a-empty v-else description="请先上传图片">
            <a-button type="primary" @click="() => uploadType = 'file'">去上传</a-button>
          </a-empty>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import PictureUpload from '@/components/PictureUpload.vue'
import { computed, h, onMounted, onUnmounted, reactive, ref, watchEffect } from 'vue'
import { message } from 'ant-design-vue'
import {
  editPictureUsingPost,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController.ts'
import { useRoute, useRouter } from 'vue-router'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import ImageCropper from '@/components/ImageCropper.vue'
import { EditOutlined, FullscreenOutlined } from '@ant-design/icons-vue'
import ImageOutPainting from '@/components/ImageOutPainting.vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'
import PictureEditWebSocket from '@/utils/pictureEditWebSocket.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'

const router = useRouter()
const route = useRoute()

const picture = ref<API.PictureVO>()
// 初始化 pictureForm，确保所有字段都存在
const pictureForm = reactive<API.PictureEditRequest>({
  id: undefined,
  name: '',
  introduction: '',
  category: '',
  tags: [],
})
const uploadType = ref<'file' | 'url'>('file')
// 空间 id
const spaceId = computed(() => {
  return route.query?.spaceId
})

// WebSocket 相关
let ws: PictureEditWebSocket | null = null
// 是否正在编辑
const isEditing = ref(false)
// 当前编辑的用户
const editingUser = ref<API.UserVO>()
// 登录用户信息
const loginUserStore = useLoginUserStore()
const loginUser = computed(() => loginUserStore.loginUser)

// 判断是否是当前用户在编辑
const isCurrentUserEditing = computed(() => {
  return editingUser.value?.id === loginUser.value?.id
})

/**
 * 图片上传成功
 * @param newPicture
 */
const onSuccess = (newPicture: API.PictureVO) => {
  console.log('===== 图片上传成功 =====')
  console.log('newPicture:', newPicture)
  console.log('newPicture.id:', newPicture.id)
  picture.value = newPicture

  // 初始化 pictureForm，确保所有字段都有值
  pictureForm.name = newPicture.name || ''
  pictureForm.introduction = newPicture.introduction || ''
  pictureForm.category = newPicture.category || ''
  pictureForm.tags = newPicture.tags || []

  console.log('picture.value:', picture.value)
  console.log('pictureForm:', pictureForm)
  console.log('pictureForm.id:', pictureForm.id)
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  console.log('===== 表单提交 =====')
  console.log('表单数据 values:', values)
  console.log('picture.value:', picture.value)
  console.log('picture.value.id:', picture.value.id)
  console.log('pictureForm 当前值:', pictureForm)
  console.log('pictureForm.name:', pictureForm.name)
  console.log('pictureForm.introduction:', pictureForm.introduction)
  console.log('pictureForm.category:', pictureForm.category)
  console.log('pictureForm.tags:', pictureForm.tags)

  const pictureId = picture.value.id
  if (!pictureId) {
    console.error('❌ pictureId 为空，无法提交')
    message.error('图片ID为空，请先上传图片')
    return
  }
  console.log('✅ 准备提交，pictureId:', pictureId)

  // 直接使用 pictureForm 的数据，而不是 values
  const requestData: API.PictureEditRequest = {
    id: pictureId,
    name: pictureForm.name,
    introduction: pictureForm.introduction,
    category: pictureForm.category,
    tags: pictureForm.tags,
  }

  console.log('最终提交的数据:', requestData)

  const res = await editPictureUsingPost(requestData)
  console.log('接口返回:', res)
  // 操作成功
  if (res.data.code === 0 && res.data.data) {
    message.success('保存成功')
    // 跳转到图片详情页
    router.push({
      path: `/picture/${pictureId}`,
    })
  } else {
    message.error('保存失败，' + res.data.message)
  }
}

const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])

/**
 * 获取标签和分类选项
 * @param values
 */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
    categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
  } else {
    message.error('获取标签分类列表失败，' + res.data.message)
  }
}

onMounted(() => {
  getTagCategoryOptions()
  // 获取图片数据后初始化 WebSocket
  getOldPicture().then(() => {
    const pictureId = route.query?.id
    if (pictureId) {
      initWebSocket()
    }
  })
})

// 组件卸载时断开 WebSocket
onUnmounted(() => {
  if (ws) {
    // 退出编辑状态
    if (isCurrentUserEditing.value) {
      doExitEdit()
    }
    ws.disconnect()
    ws = null
  }
})

// 获取老数据
const getOldPicture = async () => {
  // 获取到 id
  const id = route.query?.id
  if (id) {
    const res = await getPictureVoByIdUsingGet({
      id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data
      pictureForm.name = data.name
      pictureForm.introduction = data.introduction
      pictureForm.category = data.category
      pictureForm.tags = data.tags
    }
  }
}

// ----- 图片编辑器引用 ------
const imageCropperRef = ref()

// 编辑成功事件
const onCropSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

// ----- AI 扩图引用 -----
const imageOutPaintingRef = ref()

// 打开 AI 扩图弹窗
const doImagePainting = async () => {
  imageOutPaintingRef.value?.openModal()
}

// AI 扩图保存事件
const onImageOutPaintingSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

// 初始化 WebSocket
const initWebSocket = () => {
  const pictureId = route.query?.id
  if (!pictureId) {
    return
  }

  ws = new PictureEditWebSocket(Number(pictureId))

  // 监听连接成功
  ws.on('open', () => {
    console.log('WebSocket 连接成功')
  })

  // 监听用户进入编辑
  ws.on('ENTER_EDIT', (data: any) => {
    const user = data.user
    if (user) {
      editingUser.value = user
      isEditing.value = true
      message.info(`${user.userName} 开始编辑图片`)
    }
  })

  // 监听编辑操作
  ws.on('EDIT_ACTION', (data: any) => {
    const user = data.user
    const action = data.editAction
    if (user) {
      console.log(`${user.userName} 执行了操作: ${action}`)
    }
  })

  // 监听用户退出编辑
  ws.on('EXIT_EDIT', (data: any) => {
    const user = data.user
    if (user && editingUser.value?.id === user.id) {
      editingUser.value = undefined
      isEditing.value = false
      message.info(`${user.userName} 退出编辑`)
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

// 进入编辑
const doEditPicture = () => {
  if (!picture.value?.id) {
    message.warning('请先上传图片')
    return
  }

  if (isEditing.value && !isCurrentUserEditing.value) {
    message.warning('其他用户正在编辑，请稍后再试')
    return
  }

  // 进入编辑模式
  imageCropperRef.value?.openModal()

  // 发送进入编辑消息
  if (ws && !isCurrentUserEditing.value) {
    ws.sendMessage({
      messageType: 'ENTER_EDIT',
    })
  }
}

// 退出编辑
const doExitEdit = () => {
  if (ws && isCurrentUserEditing.value) {
    ws.sendMessage({
      messageType: 'EXIT_EDIT',
    })
    // 清除本地状态
    editingUser.value = undefined
    isEditing.value = false
  }
}

// 获取空间信息
const space = ref<API.SpaceVO>()

// 获取空间信息
const fetchSpace = async () => {
  // 获取数据
  if (spaceId.value) {
    const res = await getSpaceVoByIdUsingGet({
      id: spaceId.value,
    })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    }
  }
}

watchEffect(() => {
  fetchSpace()
})
</script>

<style scoped>
#addPicturePage {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

/* 左右分栏布局 */
.split-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* 左侧面板：上传区域 */
.left-panel {
  flex: 0 0 480px;
  max-width: 480px;
}

/* 右侧面板：编辑表单 */
.right-panel {
  flex: 1;
  min-width: 0; /* 防止内容溢出 */
}

/* 编辑工具栏 */
#addPicturePage .edit-bar {
  margin-top: 16px;
}

/* 响应式布局：小屏幕上下排列 */
@media (max-width: 992px) {
  .split-layout {
    flex-direction: column;
  }

  .left-panel,
  .right-panel {
    flex: 1;
    max-width: 100%;
  }
}

/* 图片上传组件样式 */
.picture-upload :deep(.ant-upload) {
  width: 100% !important;
  height: 100% !important;
  min-width: 152px;
  min-height: 152px;
}

.picture-upload img {
  max-width: 100%;
  max-height: 480px;
}

.ant-upload-select-picture-card i {
  font-size: 32px;
  color: #999;
}

.ant-upload-select-picture-card .ant-upload-text {
  margin-top: 8px;
  color: #666;
}
</style>
