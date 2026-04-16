import axios from "axios";
import {message} from "ant-design-vue";

// 区分开发和生产环境
const DEV_BASE_URL = "http://localhost:8123";
// const PROD_BASE_URL = "http://81.69.229.63";
// 创建 Axios 实例
const myAxios = axios.create({
    baseURL: DEV_BASE_URL,
    timeout: 30000,
    withCredentials: true,
    headers: {
        'Cache-Control': 'no-cache',
        'Pragma': 'no-cache',
        'Expires': '0',
    },
});

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    // 添加时间戳防止缓存
    if (config.method === 'post') {
      config.headers = {
        ...config.headers,
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'X-Request-ID': Date.now().toString() + Math.random().toString(36).substring(7),
      };

      // 🔍 调试：打印请求详情
      if (config.url?.includes('/picture/list/page/vo')) {
        console.log('===== Axios请求拦截器 =====')
        console.log('URL:', config.url)
        console.log('Method:', config.method)
        console.log('Headers:', config.headers)

        // 🔧 修复：详细检查请求体
        if (config.data) {
          console.log('📥 Request Data 原始值:', config.data)
          console.log('📥 Request Data 类型:', typeof config.data)
          console.log('📥 Request Data 构造函数:', config.data.constructor.name)

          // 检查是否是Proxy对象
          if (config.data.constructor.name === 'Object' || typeof config.data === 'object') {
            console.log('📥 Request Data JSON序列化:', JSON.stringify(config.data))
            console.log('📥 Request Data 键值对:')
            Object.keys(config.data).forEach(key => {
              console.log(`  - ${key}:`, config.data[key], `类型:`, typeof config.data[key])
            })
          }
        } else {
          console.log('⚠️ Request Data 为空或undefined!')
        }

        console.log('完整配置:', config)
      }
    }
    return config
  },
  function (error) {
    // Do something with request error
    return Promise.reject(error)
  },
)

// 全局响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response

    // 🔍 调试：打印分页响应详情
    if (response.config.url?.includes('/picture/list/page/vo')) {
      console.log('===== Axios响应拦截器 =====')
      console.log('Response URL:', response.config.url)
      console.log('Response Status:', response.status)
      console.log('Response Data:', data)
      if (data.data && data.data.records) {
        console.log('Records Length:', data.data.records.length)
        console.log('Total:', data.data.total)
        console.log('Current:', data.data.current)
        console.log('Size:', data.data.size)
        console.log('前3条记录ID:', data.data.records.slice(0, 3).map((r: any) => r.id))
      }
    }

    // 未登录
    if (data.code === 40100) {
      // 不是获取用户信息的请求，并且用户目前不是已经在用户登录页面，则跳转到登录页面
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        message.warning('请先登录')
        window.location.href = `/user/login?redirect=${window.location.href}`
      }
    }
    return response
  },
  function (error) {
    // Any status codes that falls outside the range of 2xx cause this function to trigger
    // Do something with response error
    return Promise.reject(error)
  },
)

export default myAxios;
