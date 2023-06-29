<script setup lang="ts">

</script>

<template>
  <div style="">
    <form action="/login" method="post" class="login">
      <table style="display:block;margin: 0 auto;">
        <tr style="margin-top: 70px;">
          <td style="text-align: center;color: #4381e6; font-size: 30px;width: 400px;">
            <img src="/static/image/logo.png"
                 alt="Arthas"
                 title="Welcome to Arthas web console"
                 style="height: 40px;"
                 class="img-responsive">
          </td>
        </tr>
        <tr style="margin-top: 50px">
          <td style="padding-left: 60px">
            <input name="username" type="text" placeholder="账号" style="width: 280px;height: 30px;line-height: 30px;border:0;border-bottom: solid 1px #C6C6C6"/>
          </td>
        </tr>
        <tr style="margin-top: 10px;">
          <td  style="padding-left: 60px">
            <input name="password" type="password" placeholder="密码" style="width: 280px;height: 30px;line-height: 30px;border:0;border-bottom: solid 1px #C6C6C6"/>
          </td>
        </tr>
        <tr>
          <td  style="text-align: center;width: 400px;padding-top: 20px;">
            <span style="color:#FF3B30"></span>
          </td>
        </tr>
        <tr style="margin-top: 20px;">
          <td style="padding-left: 62px">
            <input id="loginBtn" value="登录" type="submit" style="display:block;width: 280px;height: 40px;line-height: 40px;border-radius: 10px;color: #fff;background-image: linear-gradient(to left,#4381e6,#11C1E6);border: 0;cursor: pointer;"/>
          </td>
        </tr>
      </table>
    </form>
  </div>
</template>

<style>
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

.login {
  margin: 150px auto 0;
  width: 400px;
  height: 420px;
  background-color: #fff;
  border-radius: 5px;
  box-shadow: 10px 10px 20px 10px #E1E2E4
}
</style>