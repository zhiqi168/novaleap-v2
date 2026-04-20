<template>
  <footer class="footer-landing">
    <section class="footer-band">
      <div class="footer-inner">
        <div class="footer-main">
          <div class="brand-row">
            <span class="brand-title">NovaLeap</span>
            <span class="brand-slogan">让成长更有节奏，让学习更轻松</span>
          </div>
          <nav class="footer-links">
            <RouterLink
              v-for="item in footerLinks"
              :key="item.path"
              :to="item.path"
              class="footer-link"
            >
              {{ item.name }}
            </RouterLink>
          </nav>
        </div>

        <div class="public-card">
          <p class="public-title">关注公众号</p>
          <img
            :src="supportQr"
            alt="NovaLeap 公众号二维码"
            class="public-qr"
          />
        </div>
      </div>
    </section>

    <section class="icp-footer">
      <div class="icp-inner">
        <p>© 2026 NovaLeap. 本网站原创内容（题目答案解析、图谱数据、UI界面设计、交互逻辑、视觉风格、功能架构及全部程序代码）均为本站版权所有，未经书面授权禁止任何形式的复制、转载或仿冒。</p>
        <p>本网站当前使用境外服务器部署，暂不涉及 ICP 备案号展示</p>
      </div>
    </section>
  </footer>
</template>

<script setup>
import supportQr from '@/assets/support-qr.jpg'

const footerLinks = [
  { name: '拾光题库', path: '/questions' },
  { name: '灵感手记', path: '/notes' },
  { name: '简历工坊', path: '/resume' },
  { name: '知跃陪练', path: '/coach' },
  { name: '星愿墙', path: '/wishes' },
  { name: '休闲时刻', path: '/game' },
]
</script>

<style scoped>
.footer-landing {
  position: relative;
  margin-top: 0;
  border-top: 1px solid var(--border-soft);
  border-radius: 0;
  overflow: hidden;
  box-shadow: none;
}

.footer-band {
  background: linear-gradient(90deg, #252933 0%, #232730 50%, #252933 100%);
  color: rgba(225, 230, 238, 0.82);
}

.footer-inner {
  max-width: 1260px;
  margin: 0 auto;
  padding: 18px clamp(16px, 3.8vw, 40px) 14px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.footer-main {
  display: grid;
  gap: 12px;
}

.brand-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  color: rgba(245, 248, 255, 0.96);
}

.brand-title {
  font-size: clamp(22px, 2.2vw, 32px);
  font-weight: 700;
  color: rgba(247, 249, 255, 0.98);
}

.brand-slogan {
  font-size: 14px;
  color: rgba(198, 206, 220, 0.72);
}

.footer-links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 20px;
  color: rgba(196, 204, 218, 0.78);
  font-size: 14px;
}

.footer-link {
  transition: color 0.24s ease;
}

.footer-link:hover {
  color: #ffffff;
}

.public-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 144px;
  padding: 0;
}

.public-title {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 500;
  color: rgba(190, 199, 214, 0.9);
  letter-spacing: 0.04em;
}

.public-qr {
  width: 112px;
  height: 112px;
  border-radius: 8px;
  background: #f2f4f7;
  border: 1px solid rgba(255, 255, 255, 0.14);
  object-fit: cover;
}

.icp-footer {
  background: linear-gradient(90deg, #23262f 0%, #21242c 50%, #23262f 100%);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.icp-inner {
  max-width: 1260px;
  margin: 0 auto;
  padding: 12px clamp(16px, 3.8vw, 40px) 14px;
  display: grid;
  gap: 6px;
  color: rgba(162, 172, 188, 0.78);
  font-size: 12px;
  line-height: 1.55;
}

.icp-inner p {
  margin: 0;
}

.dark .public-qr {
  opacity: 0.95;
  filter: brightness(0.96);
}

@media (max-width: 768px) {
  .footer-inner {
    flex-direction: column;
    align-items: flex-start;
  }

  .public-card {
    width: 132px;
    align-items: flex-start;
  }

  .brand-row {
    display: block;
  }

  .brand-slogan {
    display: block;
    margin-top: 2px;
  }

  .icp-inner {
    font-size: 11px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .footer-link {
    transition: none;
  }
}
</style>
