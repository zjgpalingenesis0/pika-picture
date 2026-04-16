<template>
  <div id="globalHeader">
    <!-- 蓝天白云背景 -->
    <div class="sky-background">
      <div class="cloud cloud-1"></div>
      <div class="cloud cloud-2"></div>
      <div class="cloud cloud-3"></div>
      <div class="cloud cloud-4"></div>
      <div class="cloud cloud-5"></div>
    </div>

    <a-row :wrap="false">
      <a-col flex="200px">
        <router-link to="/">
          <div class="title-bar">
            <img class="logo" src="../assets/weifeng.webp" alt="logo" />
            <div class="title">微风图库</div>
          </div>
        </router-link>
      </a-col>
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="current"
          mode="horizontal"
          :items="items"
          @click="doMenuClick"
        />
      </a-col>
      <!-- 用户信息展示栏 -->
      <a-col flex="120px">
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <router-link to="/my_space">
                      <UserOutlined />
                      我的空间
                    </router-link>
                  </a-menu-item>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import { HomeOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'
import { MenuProps, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogoutUsingPost } from '@/api/userController.ts'

const loginUserStore = useLoginUserStore()

// 未经过滤的菜单项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/add_picture',
    label: '创建图片',
    title: '创建图片',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/pictureManage',
    label: '图片管理',
    title: '图片管理',
  },
  {
    key: '/admin/spaceManage',
    label: '空间管理',
    title: '空间管理',
  },
]

// 根据权限过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    // 管理员才能看到 /admin 开头的菜单
    if (menu?.key?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const items = computed(() => filterMenus(originItems))

const router = useRouter()
// 当前要高亮的菜单项
const current = ref<string[]>([])
// 监听路由变化，更新高亮菜单项
router.afterEach((to, from, next) => {
  current.value = [to.path]
})

// 路由跳转事件
const doMenuClick = ({ key }) => {
  router.push({
    path: key,
  })
}

// 用户注销
const doLogout = async () => {
  const res = await userLogoutUsingPost()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
/* 确保容器有相对定位 */
#globalHeader {
  position: relative;
  min-height: 64px;
}

/* 蓝天背景容器 */
.sky-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(to bottom, #87CEEB 0%, #B0E0E6 50%, #E0F6FF 100%);
  overflow: hidden;
  z-index: 0;
  border-radius: 4px;
}

/* 云朵基础样式 */
.cloud {
  position: absolute;
  background: white;
  border-radius: 50%;
  opacity: 0.8;
  animation: moveClouds linear infinite;
}

.cloud::before,
.cloud::after {
  content: '';
  position: absolute;
  background: white;
  border-radius: 50%;
}

/* 云朵1 - 大而慢 */
.cloud-1 {
  width: 100px;
  height: 40px;
  top: 15%;
  animation-duration: 40s;
}

.cloud-1::before {
  width: 50px;
  height: 50px;
  top: -25px;
  left: 15px;
}

.cloud-1::after {
  width: 60px;
  height: 60px;
  top: -35px;
  left: 40px;
}

/* 云朵2 - 中等大小 */
.cloud-2 {
  width: 80px;
  height: 32px;
  top: 45%;
  animation-duration: 30s;
  animation-delay: -10s;
}

.cloud-2::before {
  width: 40px;
  height: 40px;
  top: -20px;
  left: 10px;
}

.cloud-2::after {
  width: 50px;
  height: 50px;
  top: -30px;
  left: 30px;
}

/* 云朵3 - 小而快 */
.cloud-3 {
  width: 60px;
  height: 24px;
  top: 70%;
  animation-duration: 25s;
  animation-delay: -5s;
  opacity: 0.6;
}

.cloud-3::before {
  width: 30px;
  height: 30px;
  top: -15px;
  left: 8px;
}

.cloud-3::after {
  width: 40px;
  height: 40px;
  top: -20px;
  left: 20px;
}

/* 云朵4 - 中等 */
.cloud-4 {
  width: 90px;
  height: 36px;
  top: 25%;
  animation-duration: 35s;
  animation-delay: -15s;
  opacity: 0.7;
}

.cloud-4::before {
  width: 45px;
  height: 45px;
  top: -22px;
  left: 12px;
}

.cloud-4::after {
  width: 55px;
  height: 55px;
  top: -32px;
  left: 35px;
}

/* 云朵5 - 小 */
.cloud-5 {
  width: 70px;
  height: 28px;
  top: 55%;
  animation-duration: 28s;
  animation-delay: -20s;
  opacity: 0.5;
}

.cloud-5::before {
  width: 35px;
  height: 35px;
  top: -18px;
  left: 10px;
}

.cloud-5::after {
  width: 45px;
  height: 45px;
  top: -25px;
  left: 25px;
}

/* 云朵移动动画 */
@keyframes moveClouds {
  0% {
    left: 100%;
    transform: translateX(0);
  }
  100% {
    left: -150px;
    transform: translateX(0);
  }
}

/* 标题栏样式优化 */
#globalHeader .title-bar {
  display: flex;
  align-items: center;
  position: relative;
  z-index: 1;
}

/* 确保内容在云朵之上 */
#globalHeader .ant-row {
  position: relative;
  z-index: 1;
}

.title {
  color: #2c3e50;
  font-size: 18px;
  font-weight: 600;
  margin-left: 16px;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.5);
}

.logo {
  height: 48px;
  filter: drop-shadow(2px 2px 4px rgba(0, 0, 0, 0.1));
}

/* 用户登录状态样式 */
.user-login-status {
  position: relative;
  z-index: 1;
}

/* 菜单样式优化 */
:deep(.ant-menu) {
  background: transparent !important;
}

:deep(.ant-menu-item) {
  color: #2c3e50 !important;
  font-weight: 500;
}

:deep(.ant-menu-item-selected) {
  color: #1890ff !important;
  font-weight: 600;
}
</style>
