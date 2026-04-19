<template>
  <div class="auth-entry-wrap">
    <div class="auth-fab-group">
      <button class="auth-fab auth-fab-login" type="button" @click="showLoginTip = true">
        <svg viewBox="0 0 24 24" fill="none" class="auth-fab-icon">
          <path d="M11 4H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
          <path d="m17 16 4-4-4-4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
          <path d="M21 12H9" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
      <button class="auth-fab auth-fab-register" type="button" @click="openRegister()">
        <svg viewBox="0 0 24 24" fill="none" class="auth-fab-icon">
          <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
          <circle cx="8.5" cy="7" r="4" stroke="currentColor" stroke-width="1.8" />
          <path d="M20 8v6M17 11h6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
        </svg>
      </button>
    </div>

    <div v-if="showLoginTip" class="login-tip" @animationend="showLoginTip = false">
      当前页面就是登录入口
    </div>

    <transition name="drawer-fade">
      <div v-if="visible" class="register-overlay" @click.self="closePanel">
        <div class="register-panel">
          <div class="register-head">
            <h3>免费注册</h3>
            <button class="close-btn" type="button" @click="closePanel">×</button>
          </div>

          <div class="register-tabs">
            <button type="button" :class="{ active: mode === 'email' }" @click="mode = 'email'">邮箱注册</button>
            <button type="button" :class="{ active: mode === 'phone' }" @click="mode = 'phone'">手机号注册</button>
            <button type="button" :class="{ active: mode === 'qq' }" @click="mode = 'qq'">QQ 登录</button>
          </div>

          <form v-if="mode === 'email'" @submit.prevent="handleEmailRegister" class="form-block">
            <input v-model.trim="emailForm.email" type="email" placeholder="邮箱地址" required />
            <input v-model="emailForm.password" type="password" placeholder="设置密码" required />
            <input v-model.trim="emailForm.nickname" type="text" placeholder="昵称（可选）" />
            <button :disabled="loading" type="submit">{{ loading ? '提交中...' : '注册并登录' }}</button>
          </form>

          <form v-else-if="mode === 'phone'" @submit.prevent="handlePhoneRegister" class="form-block">
            <input v-model.trim="phoneForm.phone" type="tel" placeholder="手机号（11位）" required />
            <input v-model="phoneForm.password" type="password" placeholder="设置密码" required />
            <input v-model.trim="phoneForm.nickname" type="text" placeholder="昵称（可选）" />
            <button :disabled="loading" type="submit">{{ loading ? '提交中...' : '注册并登录' }}</button>
          </form>

          <form v-else @submit.prevent="handleQqLogin" class="form-block">
            <input v-model.trim="qqForm.qq" type="text" placeholder="QQ 号" required />
            <input v-model.trim="qqForm.nickname" type="text" placeholder="昵称（可选）" />
            <button :disabled="loading" type="submit">{{ loading ? '连接中...' : 'QQ 一键登录' }}</button>
          </form>

          <p v-if="error" class="error-text">{{ error }}</p>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useAuth } from '@/composables/useAuth'

const { registerByEmail, registerByPhone, qqLogin } = useAuth()

const visible = ref(false)
const showLoginTip = ref(false)
const mode = ref('email')
const loading = ref(false)
const error = ref('')

const emailForm = reactive({
  email: '',
  password: '',
  nickname: ''
})

const phoneForm = reactive({
  phone: '',
  password: '',
  nickname: ''
})

const qqForm = reactive({
  qq: '',
  nickname: ''
})

const openRegister = () => {
  error.value = ''
  mode.value = 'email'
  visible.value = true
}

const closePanel = () => {
  visible.value = false
  error.value = ''
}

const handleEmailRegister = async () => {
  if (!emailForm.email || !emailForm.password) return
  loading.value = true
  error.value = ''
  try {
    await registerByEmail(emailForm.email, emailForm.password, emailForm.nickname)
    visible.value = false
  } catch (e) {
    error.value = e.message || '邮箱注册失败'
  } finally {
    loading.value = false
  }
}

const handlePhoneRegister = async () => {
  if (!/^1\d{10}$/.test(phoneForm.phone)) {
    error.value = '手机号格式不正确'
    return
  }
  if (!phoneForm.password) return
  loading.value = true
  error.value = ''
  try {
    await registerByPhone(phoneForm.phone, phoneForm.password, phoneForm.nickname)
    visible.value = false
  } catch (e) {
    error.value = e.message || '手机号注册失败'
  } finally {
    loading.value = false
  }
}

const handleQqLogin = async () => {
  if (!/^[1-9]\d{4,12}$/.test(qqForm.qq)) {
    error.value = 'QQ号格式不正确'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await qqLogin(qqForm.qq, qqForm.nickname)
    visible.value = false
  } catch (e) {
    error.value = e.message || 'QQ登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-entry-wrap {
  position: fixed;
  right: 18px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 60;
}

.auth-fab-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.auth-fab {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(8px);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.2);
  color: #0f172a;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.auth-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.24);
}

.auth-fab-login {
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.92), rgba(236, 241, 255, 0.86));
}

.auth-fab-register {
  background: linear-gradient(145deg, rgba(255, 239, 245, 0.92), rgba(232, 246, 255, 0.88));
}

.auth-fab-icon {
  width: 22px;
  height: 22px;
}

.login-tip {
  position: absolute;
  right: 58px;
  top: 0;
  white-space: nowrap;
  background: rgba(15, 23, 42, 0.9);
  color: #fff;
  border-radius: 10px;
  font-size: 12px;
  padding: 8px 10px;
  animation: tipFade 1.8s ease forwards;
}

@keyframes tipFade {
  0% { opacity: 0; transform: translateY(4px); }
  15% { opacity: 1; transform: translateY(0); }
  85% { opacity: 1; transform: translateY(0); }
  100% { opacity: 0; transform: translateY(-4px); }
}

.register-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  justify-content: flex-end;
  background: rgba(15, 23, 42, 0.2);
}

.register-panel {
  width: min(420px, 92vw);
  height: 100%;
  background: linear-gradient(180deg, #f3f5fa, #eff1f6 55%, #fff);
  box-shadow: -24px 0 60px rgba(15, 23, 42, 0.18);
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.register-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.register-head h3 {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  color: #334155;
  font-size: 20px;
}

.register-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.register-tabs button {
  border-radius: 10px;
  border: 1px solid #dbe4f2;
  background: #fff;
  color: #475569;
  height: 36px;
  font-size: 13px;
  transition: all 0.2s ease;
}

.register-tabs button.active {
  border-color: #7c8efb;
  color: #263fbf;
  background: #eef2ff;
}

.form-block {
  display: grid;
  gap: 12px;
  margin-top: 8px;
}

.form-block input {
  height: 44px;
  border-radius: 12px;
  border: 1px solid #d9e1ef;
  padding: 0 12px;
  outline: none;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.94);
}

.form-block input:focus {
  border-color: #7c8efb;
  box-shadow: 0 0 0 4px rgba(124, 142, 251, 0.14);
}

.form-block button {
  margin-top: 4px;
  height: 44px;
  border-radius: 12px;
  color: #fff;
  font-weight: 600;
  background: linear-gradient(95deg, #5164e0, #df6bb4);
}

.form-block button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.error-text {
  color: #b91c1c;
  font-size: 13px;
  background: #fee2e2;
  border-radius: 10px;
  padding: 8px 10px;
}

.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.2s ease;
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

@media (max-width: 960px) {
  .auth-entry-wrap {
    right: 12px;
    top: auto;
    bottom: 16px;
    transform: none;
  }
}
</style>
