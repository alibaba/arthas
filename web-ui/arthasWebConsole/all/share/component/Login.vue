<script lang="ts">
import {defineComponent, ref} from 'vue'
import {useRouter} from 'vue-router'
import logo from "~/assert/arthas.png"

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

<template>
  <div>
    <form class="login-form" @submit.prevent="login">
      <table style="display:block;margin: 0 auto;">
        <tr style="margin-top: 70px;">
          <td style="text-align: center;color: #4381e6; font-size: 30px;width: 400px;">
            <img :src="logo"
                 alt="Arthas"
                 title="Welcome to Arthas web console"
                 style="height: 40px;"
                 class="img-responsive">
          </td>
        </tr>
        <tr style="margin-top: 50px">
          <td style="padding-left: 60px">
            <input id="username" name="username" type="text" placeholder="账号"
                   v-model.trim="form.username" required
                   style="width: 280px;height: 30px;line-height: 30px;border:0;border-bottom: solid 1px #C6C6C6"/>
          </td>
        </tr>
        <tr style="margin-top: 10px;">
          <td style="padding-left: 60px">
            <input id="password" name="password" type="password" placeholder="密码"
                   v-model.trim="form.password" required
                   style="width: 280px;height: 30px;line-height: 30px;border:0;border-bottom: solid 1px #C6C6C6"/>
          </td>
        </tr>
        <tr>
          <td style="text-align: center;width: 400px;padding-top: 20px;">
            <span style="color:#FF3B30"></span>
          </td>
        </tr>
        <tr style="margin-top: 20px;">
          <td style="padding-left: 62px">
            <input id="loginBtn" value="登录" type="submit"
                   style="display:block;width: 280px;height: 40px;line-height: 40px;border-radius: 10px;color: #fff;background-image: linear-gradient(to left,#4381e6,#11C1E6);border: 0px;cursor: pointer;"/>
          </td>
        </tr>
      </table>
    </form>
  </div>
</template>

<style scoped>
body {
  background-color: #F5F6F8;
}

tr {
  color: #C6C6C6;
  font-weight: lighter;
  width: 400px;
  display: block;
  height: 40px;
  line-height: 40px;
}

.login-form {
  margin: 150px auto 0;
  width: 400px;
  height: 420px;
  background-color: #fff;
  border-radius: 5px;
  box-shadow: 10px 10px 20px 10px #E1E2E4
}
</style>