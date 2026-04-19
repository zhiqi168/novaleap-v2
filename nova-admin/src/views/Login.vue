<template>
  <div class="h-screen w-screen bg-admin-bg flex items-center justify-center p-4">
    <div class="w-full max-w-md bg-admin-card rounded-xl shadow-lg border border-gray-100 p-8">
      <div class="text-center mb-8">
        <div class="w-12 h-12 rounded-xl bg-admin-primary/10 text-admin-primary flex items-center justify-center mx-auto mb-4">
          <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
            />
          </svg>
        </div>
        <h1 class="text-2xl font-bold text-admin-text">Nova Admin</h1>
        <p class="text-sm text-admin-muted mt-1">Admin console sign in</p>
      </div>

      <form class="space-y-5" @submit.prevent="handleLogin">
        <div>
          <label class="block text-sm font-medium text-admin-text mb-1">Username</label>
          <input
            v-model="form.username"
            type="text"
            required
            autocomplete="username"
            placeholder="Email or username"
            class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:border-admin-primary transition-colors text-sm"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-admin-text mb-1">Password</label>
          <input
            v-model="form.password"
            type="password"
            required
            autocomplete="current-password"
            placeholder="Enter password"
            class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:border-admin-primary transition-colors text-sm"
          />
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-admin-primary hover:bg-admin-primary/90 text-white font-medium py-2 rounded-lg transition-colors flex justify-center items-center text-sm disabled:opacity-75"
        >
          <span v-if="loading" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-2"></span>
          {{ loading ? 'Signing in...' : 'Sign in' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminLogin } from '@/api/admin'
import { setAdminToken } from '@/composables/adminAuthStorage'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

const handleLogin = async () => {
  if (!form.username || !form.password) {
    return
  }

  loading.value = true
  try {
    const res = await adminLogin(form)
    if (res.code !== 200) {
      alert(res.msg || 'Login failed')
      return
    }

    setAdminToken(res.data.token)
    router.push('/dashboard')
  } catch (error) {
    alert(error.message || 'Network or API error')
  } finally {
    loading.value = false
  }
}
</script>
