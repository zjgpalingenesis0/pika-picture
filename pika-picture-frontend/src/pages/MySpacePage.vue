<template>
  <div id="mySpacePage">
    <h2>我的空间</h2>

    <!-- 空间类型切换 -->
    <a-tabs v-model:activeKey="spaceType">
      <a-tab-pane key="private" tab="私有空间">
        <a-list
          :data-source="privateSpaces"
          :loading="loading"
          item-layout="horizontal"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #title>
                  <a-space>
                    <a-avatar :src="item.user?.userAvatar" />
                    <div>
                      <div class="space-name">{{ item.spaceName }}</div>
                      <div class="space-level">
                        <a-tag :color="getSpaceLevelColor(item.spaceLevel)">
                          {{ getSpaceLevelText(item.spaceLevel) }}
                        </a-tag>
                      </div>
                    </div>
                  </a-space>
                </template>
                <template #description>
                  <a-space>
                    <span>{{ item.totalCount || 0 }}/{{ item.maxCount || 0 }} 张图片</span>
                    <span>{{ formatSize(item.totalSize || 0) }}/{{ formatSize(item.maxSize || 0) }}</span>
                  </a-space>
                </template>
              </a-list-item-meta>
              <template #actions>
                <a-space>
                  <a-button type="link" @click="viewSpace(item)">查看</a-button>
                  <a-button type="link" @click="editSpace(item)">编辑</a-button>
                  <a-button type="link" danger @click="deleteSpace(item.id)">删除</a-button>
                </a-space>
              </template>
            </a-list-item>
          </template>
          <template #footer>
            <div v-if="!hasPrivateSpace" class="no-data">
              <a-empty description="暂无私有空间">
                <a-button type="primary" href="/add_space">创建空间</a-button>
              </a-empty>
            </div>
          </template>
        </a-list>
      </a-tab-pane>

      <a-tab-pane key="team" tab="团队空间">
        <a-list
          :data-source="teamSpaces"
          :loading="loading"
          item-layout="horizontal"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #avatar>
                  <a-avatar :src="item.space?.user?.userAvatar || item.user?.userAvatar" />
                </template>
                <template #title>
                  <a-space>
                    <div>
                      <div class="space-name">{{ item.space.spaceName }}</div>
                      <div class="space-info">
                        <a-tag :color="getSpaceLevelColor(item.space.spaceLevel)">
                          {{ getSpaceLevelText(item.space.spaceLevel) }}
                        </a-tag>
                        <a-tag color="blue">团队</a-tag>
                      </div>
                    </div>
                    <div class="space-role">
                      角色：
                      <a-tag color="green">{{ getRoleText(item.spaceRole) }}</a-tag>
                    </div>
                  </a-space>
                </template>
                <template #description>
                  <a-space>
                    <span>创建者：{{ item.space?.user?.userName || '-' }}</span>
                    <span>{{ item.totalCount || 0 }}/{{ item.maxCount || 0 }} 张图片</span>
                    <span>{{ formatSize(item.totalSize || 0) }}/{{ formatSize(item.maxSize || 0) }}</span>
                  </a-space>
                </template>
              </a-list-item-meta>
              <template #actions>
                <a-space>
                  <a-button type="link" @click="viewSpace(item.space)">查看</a-button>
                  <a-button
                    v-if="canEditSpace(item)"
                    type="link"
                    @click="exitSpace(item)"
                  >
                    退出
                  </a-button>
                </a-space>
              </template>
            </a-list-item>
          </template>
          <template #footer>
            <div v-if="!hasTeamSpace" class="no-data">
              <a-empty description="暂无团队空间">
                <a-button type="primary" href="/space_analyze">查看空间列表</a-button>
              </a-empty>
            </div>
          </template>
        </a-list>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController.ts'
import { listMyTeamSpaceUsingPost, listSpaceUserUsingPost } from '@/api/spaceUserController.ts'
import { message, Modal } from 'ant-design-vue'
import { SPACE_LEVEL_ENUM, SPACE_LEVEL_MAP, SPACE_TYPE_ENUM, SPACE_ROLE_MAP, SPACE_ROLE_ENUM } from '@/constants/space.ts'
import { formatSize } from '@/utils'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const loginUser = computed(() => loginUserStore.loginUser)

const loading = ref(false)
const spaceType = ref<'private' | 'team'>('private')

// 私有空间列表
const privateSpaces = ref<API.SpaceVO[]>([])
// 团队空间列表（带角色信息）
const teamSpaces = ref<(API.SpaceVO & { spaceRole?: string })[]>([])

// 是否有私有空间
const hasPrivateSpace = computed(() => privateSpaces.value.length > 0)
// 是否有团队空间
const hasTeamSpace = computed(() => teamSpaces.value.length > 0)

// 获取私有空间
const fetchPrivateSpaces = async () => {
  if (!loginUser.value?.id) return

  loading.value = true
  try {
    const res = await listSpaceVoByPageUsingPost({
      userId: loginUser.value.id,
      spaceType: SPACE_TYPE_ENUM.PRIVATE,
      current: 1,
      pageSize: 20,
    })
    console.log('fetchPrivateSpaces 响应:', res.data)
    if (res.data.code === 0 && res.data.data?.records) {
      console.log('fetchPrivateSpaces records:', res.data.data.records)
      res.data.data.records.forEach((space, index) => {
        console.log(`空间 ${index}: id=${space.id}, spaceName=${space.spaceName}`)
      })
      privateSpaces.value = res.data.data.records
    }
  } catch (error) {
    console.error('获取私有空间失败:', error)
    message.error('获取私有空间失败')
  } finally {
    loading.value = false
  }
}

// 获取团队空间
const fetchTeamSpaces = async () => {
  if (!loginUser.value?.id) return

  loading.value = true
  try {
    // 获取我参与的团队空间
    const teamRes = await listMyTeamSpaceUsingPost()
    if (teamRes.data.code === 0 && teamRes.data.data) {
      // 为每个团队空间获取角色信息
      const spacesWithRoles = await Promise.all(
        teamRes.data.data.map(async (spaceUser: API.SpaceUserVO) => {
          // 获取空间成员信息以确定角色
          const memberRes = await listSpaceUserUsingPost({
            spaceId: spaceUser.spaceId,
          })
          if (memberRes.data.code === 0 && memberRes.data.data) {
            const members = memberRes.data.data
            // 找到当前用户的角色
            const currentUserMember = members.find((m: API.SpaceUserVO) => m.userId === loginUser.value?.id)
            return {
              ...spaceUser,
              spaceRole: currentUserMember?.spaceRole || 'viewer',
            }
          }
          return spaceUser
        })
      )
      teamSpaces.value = spacesWithRoles
    }
  } catch (error) {
    message.error('获取团队空间失败')
  } finally {
    loading.value = false
  }
}

// 查看空间
const viewSpace = (space: API.SpaceVO) => {
  router.push({
    path: `/space/${space.id}`,
  })
}

// 编辑空间
const editSpace = (space: API.SpaceVO) => {
  router.push({
    path: '/add_space',
    query: {
      id: space.id,
    },
  })
}

// 删除私有空间
const deleteSpace = async (spaceId: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该空间吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      // TODO: 调用删除空间的API
      message.success('删除成功')
      await fetchPrivateSpaces()
    },
  })
}

// 退出团队空间
const exitSpace = async (spaceWithRole: API.SpaceVO & { spaceRole?: string }) => {
  Modal.confirm({
    title: '确认退出',
    content: '确定要退出该团队空间吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      // TODO: 调用退出团队空间的API
      message.success('退出成功')
      await fetchTeamSpaces()
    },
  })
}

// 判断是否可以编辑空间
const canEditSpace = (space: API.SpaceVO & { spaceRole?: string }) => {
  // 私有空间，所有者是当前用户才能编辑
  if (spaceType.value === 'private') {
    return space.userId === loginUser.value?.id
  }
  // 团队空间，管理员可以编辑
  return space.spaceRole === SPACE_ROLE_ENUM.ADMIN
}

// 获取空间级别颜色
const getSpaceLevelColor = (level: number) => {
  const colorMap: Record<number, string> = {
    [SPACE_LEVEL_ENUM.COMMON]: 'default',
    [SPACE_LEVEL_ENUM.PROFESSIONAL]: 'blue',
    [SPACE_LEVEL_ENUM.FLAGSHIP]: 'gold',
  }
  return colorMap[level] || 'default'
}

// 获取空间级别文本
const getSpaceLevelText = (level: number) => {
  return SPACE_LEVEL_MAP[level] || '未知'
}

// 获取角色文本
const getRoleText = (role?: string) => {
  if (!role) return '-'
  return SPACE_ROLE_MAP[role] || role
}

onMounted(() => {
  // 同时加载私有空间和团队空间
  fetchPrivateSpaces()
  fetchTeamSpaces()
})
</script>

<style scoped>
#mySpacePage {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.space-name {
  font-weight: 500;
  font-size: 16px;
}

.space-info {
  display: flex;
  gap: 8px;
  align-items: center;
}

.space-role {
  margin-top: 4px;
}

.no-data {
  padding: 40px 0;
}
</style>
