<template>
  <div id="basicLayout">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header">
        <GlobalHeader />
      </a-layout-header>
      <a-layout>
        <div class="sider-wrapper">
          <!-- 左侧菜单背景 -->
          <div class="sider-background">
            <!-- 荷叶装饰 -->
            <div class="lotus-leaf lotus-1"></div>
            <div class="lotus-leaf lotus-2"></div>
            <div class="lotus-leaf lotus-3"></div>
            <!-- 小鱼装饰 -->
            <div class="fish fish-1">🐟</div>
            <div class="fish fish-2">🐠</div>
          </div>
          <GlobalSider class="sider" />
        </div>
        <a-layout-content class="content">
          <!-- 湖泊背景 -->
          <div class="lake-background">
            <!-- 芦苇丛 -->
            <div class="reeds reeds-left">
              <div class="reed reed-1"></div>
              <div class="reed reed-2"></div>
              <div class="reed reed-3"></div>
              <div class="reed reed-4"></div>
              <div class="reed reed-5"></div>
            </div>
            <div class="reeds reeds-right">
              <div class="reed reed-6"></div>
              <div class="reed reed-7"></div>
              <div class="reed reed-8"></div>
              <div class="reed reed-9"></div>
              <div class="reed reed-10"></div>
            </div>
          </div>
          <div class="content-wrapper">
            <router-view />
          </div>
        </a-layout-content>
      </a-layout>
      <a-layout-footer class="footer">
        <a href="https://www.csdn.net" target="_blank"> CSDN </a>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalSider from "@/components/GlobalSider.vue";
</script>

<style scoped>
#basicLayout .header {
  padding-inline: 20px;
  background: transparent;
  color: unset;
  margin-bottom: 1px;
}

/* 左侧菜单包装器 */
.sider-wrapper {
  position: relative;
  width: 200px;
  height: 100%;
  z-index: 1;
}

/* 左侧菜单背景 */
.sider-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg,
    rgba(32, 178, 170, 0.15) 0%,
    rgba(64, 164, 223, 0.2) 30%,
    rgba(30, 144, 255, 0.25) 60%,
    rgba(25, 118, 210, 0.3) 100%
  );
  z-index: 0;
  overflow: visible;
}

/* 水波纹效果 */
.sider-background::before {
  content: '';
  position: absolute;
  top: 0;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(ellipse at center,
    rgba(255, 255, 255, 0.1) 0%,
    transparent 70%
  );
  animation: waterRipple 8s ease-in-out infinite;
}

@keyframes waterRipple {
  0%, 100% {
    transform: translateY(0) scale(1);
    opacity: 0.5;
  }
  50% {
    transform: translateY(-20px) scale(1.1);
    opacity: 0.8;
  }
}

/* 荷叶 */
.lotus-leaf {
  position: absolute;
  background: radial-gradient(ellipse at center, #6b8e23, #4a7c2c);
  border-radius: 50%;
  opacity: 0.7;
  animation: leafFloat 6s ease-in-out infinite;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 3;
}

/* 荷叶纹理 */
.lotus-leaf::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 60%;
  height: 1px;
  background: rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%) rotate(45deg);
}

.lotus-leaf::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 60%;
  height: 1px;
  background: rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%) rotate(-45deg);
}

.lotus-1 {
  width: 60px;
  height: 50px;
  top: 100px;
  right: -10px;
  animation-delay: 0s;
}

.lotus-2 {
  width: 45px;
  height: 38px;
  top: 250px;
  left: -5px;
  animation-delay: 2s;
}

.lotus-3 {
  width: 55px;
  height: 45px;
  top: 400px;
  right: -15px;
  animation-delay: 4s;
}

@keyframes leafFloat {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-10px) rotate(3deg);
  }
}

/* 小鱼 */
.fish {
  position: absolute;
  font-size: 20px;
  opacity: 0.6;
  animation: fishSwim 15s linear infinite;
  filter: drop-shadow(1px 1px 2px rgba(0, 0, 0, 0.3));
  z-index: 4;
}

.fish-1 {
  top: 150px;
  left: -30px;
  animation-delay: 0s;
  animation-duration: 12s;
}

.fish-2 {
  top: 300px;
  left: -30px;
  animation-delay: 6s;
  animation-duration: 18s;
}

@keyframes fishSwim {
  0% {
    transform: translateX(0) scaleX(1);
  }
  49% {
    transform: translateX(250px) scaleX(1);
  }
  50% {
    transform: translateX(250px) scaleX(-1);
  }
  99% {
    transform: translateX(0) scaleX(-1);
  }
  100% {
    transform: translateX(0) scaleX(1);
  }
}

#basicLayout .sider {
  background: transparent;
  backdrop-filter: blur(5px);
  border-right: 1px solid rgba(255, 255, 255, 0.3);
  position: relative;
  z-index: 2;
  height: 100%;
}

/* 菜单容器背景 */
#basicLayout .sider :deep(.ant-layout-sider) {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
}

#basicLayout :deep(.ant-menu-root) {
  border-bottom: none !important;
  border-inline-end: none !important;
  background: transparent !important;
}

/* 菜单项样式 */
#basicLayout :deep(.ant-menu-item) {
  background: transparent !important;
  color: #2c3e50 !important;
}

#basicLayout :deep(.ant-menu-item:hover) {
  background: rgba(32, 178, 170, 0.2) !important;
  color: #1890ff !important;
}

#basicLayout :deep(.ant-menu-item-selected) {
  background: rgba(32, 178, 170, 0.3) !important;
  color: #1890ff !important;
  font-weight: 600;
}

#basicLayout .content {
  padding: 0;
  position: relative;
  overflow: hidden;
  margin-bottom: 28px;
}

/* 湖泊背景 */
.lake-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg,
    rgba(135, 206, 235, 0.3) 0%,
    rgba(64, 164, 223, 0.4) 20%,
    rgba(30, 144, 255, 0.5) 50%,
    rgba(25, 118, 210, 0.6) 80%,
    rgba(13, 71, 161, 0.7) 100%
  );
  z-index: 0;
}

/* 湖面波纹效果 */
.lake-background::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 200%;
  height: 100%;
  background: repeating-linear-gradient(
    90deg,
    transparent,
    transparent 50px,
    rgba(255, 255, 255, 0.03) 50px,
    rgba(255, 255, 255, 0.03) 100px
  );
  animation: waterWave 20s linear infinite;
}

@keyframes waterWave {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-50%);
  }
}

/* 芦苇容器 */
.reeds {
  position: absolute;
  bottom: 0;
  width: 150px;
  height: 300px;
  z-index: 1;
}

.reeds-left {
  left: 20px;
}

.reeds-right {
  right: 20px;
}

/* 单个芦苇 */
.reed {
  position: absolute;
  bottom: 0;
  width: 4px;
  background: linear-gradient(to top, #2d5016, #4a7c2c, #6b8e23);
  border-radius: 2px 2px 0 0;
  transform-origin: bottom center;
  animation: reedSway 3s ease-in-out infinite;
}

/* 芦苇叶子 */
.reed::before {
  content: '';
  position: absolute;
  width: 20px;
  height: 30px;
  background: linear-gradient(135deg, #4a7c2c, #6b8e23);
  border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
  top: -10px;
  left: -8px;
  transform: rotate(-30deg);
}

.reed::after {
  content: '';
  position: absolute;
  width: 15px;
  height: 25px;
  background: linear-gradient(135deg, #5a8c3c, #7c9e33);
  border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
  top: -5px;
  left: -6px;
  transform: rotate(20deg);
}

/* 左侧芦苇位置和动画 */
.reed-1 {
  height: 180px;
  left: 20px;
  animation-delay: 0s;
}

.reed-2 {
  height: 220px;
  left: 50px;
  animation-delay: 0.5s;
}

.reed-3 {
  height: 160px;
  left: 80px;
  animation-delay: 1s;
}

.reed-4 {
  height: 200px;
  left: 110px;
  animation-delay: 1.5s;
}

.reed-5 {
  height: 140px;
  left: 130px;
  animation-delay: 2s;
}

/* 右侧芦苇位置和动画 */
.reed-6 {
  height: 190px;
  left: 10px;
  animation-delay: 0.3s;
}

.reed-7 {
  height: 230px;
  left: 40px;
  animation-delay: 0.8s;
}

.reed-8 {
  height: 170px;
  left: 70px;
  animation-delay: 1.3s;
}

.reed-9 {
  height: 210px;
  left: 100px;
  animation-delay: 1.8s;
}

.reed-10 {
  height: 150px;
  left: 120px;
  animation-delay: 2.3s;
}

/* 芦苇摇摆动画 */
@keyframes reedSway {
  0%, 100% {
    transform: rotate(-5deg);
  }
  50% {
    transform: rotate(5deg);
  }
}

/* 内容包装器 */
.content-wrapper {
  position: relative;
  z-index: 1;
  padding: 28px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 8px;
  margin: 28px;
  min-height: calc(100vh - 200px);
}

#basicLayout .footer {
  background: rgba(239, 239, 239, 0.9);
  backdrop-filter: blur(10px);
  padding: 16px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: right;
  padding-right: 20px;
  z-index: 10;
}
</style>
