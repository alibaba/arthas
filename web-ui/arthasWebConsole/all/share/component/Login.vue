<template>
  <div class="login-wrapper">
    <h1 class="login-title">登录</h1>
    <form class="login-form" @submit.prevent="login">
      <div class="form-item">
        <label for="username">用户名：</label>
        <input type="text" id="username" v-model.trim="form.username" required>
      </div>
      <div class="form-item">
        <label for="password">密码：</label>
        <input type="password" id="password" v-model.trim="form.password" required>
      </div>
      <button type="submit" class="login-btn">登录</button>
    </form>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue'
import { useRouter } from 'vue-router'

interface Form {
  username: string;
  password: string;
}

export default defineComponent({
  setup() {
    const form = ref<Form>({
      username: '',
      password: ''
    })

    const router = useRouter()

    const login = async () => {
      try {
        const response = await fetch('/api/auth', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(form.value)
        })
        if (response.ok) {
          const json = await response.json()
          sessionStorage.setItem('username', form.value.username)
          sessionStorage.setItem('token', json.data.value)
          await router.push('/')
        } else {
          alert('登录失败')
        }
      } catch (err) {
        alert('登录失败')
      }
    }

    return {
      form,
      login
    }
  }
})
</script>

<style scoped>
.login-wrapper {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.login-title {
  font-size: 24px;
  margin-bottom: 20px;
}

.login-form {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.form-item {
  display: flex;
  margin-bottom: 20px;
}

.form-item label {
  width: 80px;
  text-align: right;
  margin-right: 20px;
}

.form-item input {
  width: 200px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.login-btn {
  width: 100px;
  height: 30px;
  background-color: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>