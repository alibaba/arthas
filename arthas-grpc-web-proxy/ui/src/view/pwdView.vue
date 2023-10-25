<template>
  <Row>
    <Col span="5">
      <Card style="width:300px ">
        <div style="text-align:center">
          <h3>JobId</h3>
          <h3>{{ jobId }}</h3>
        </div>
      </Card>

    </Col>
    <Col span="19">
      <Card style="width:300px">
        <div style="text-align:center">
          <h3>working dir</h3>
          <h3>{{ pwdResponse }}</h3>
        </div>
      </Card>
    </Col>

  </Row>



</template>

<script>
// 引入自动生成的grpc_web相关的文件

import {

  PwdClient,
} from '@/assets/proto/ArthasServices_grpc_web_pb';


import { Empty } from 'google-protobuf/google/protobuf/empty_pb';


export default {
  // eslint-disable-next-line vue/multi-word-component-names
  name: 'pwd',
  inject: ['apiHost'],
  data(){
    return {
      pwdClient: null,
      jobId: 0,
      pwdResponse: "www",
    };
  },


  created() {
    let hostname = this.apiHost;
    this.pwdClient = new PwdClient(hostname);
    this.sendPwdRequest();
    this.metadata = {"Content-Type": "application/grpc-web-text"};
  },

  methods:{
    sendPwdRequest(){
      var pwdRequest  = new Empty();
      this.pwdClient.pwd(pwdRequest, {}, (error, response) => {
        if (!error) {
          // 处理成功响应
          this.jobId = response.getJobid();
          const type = response.getType();
          if(type == "pwd" && response.hasStringstringmapvalue()){
            var stringstringmapvalue = response.getStringstringmapvalue();
            var result = stringstringmapvalue.getStringstringmapMap().get("workingDir")
            this.pwdResponse = result
          }
        } else {
          // 处理错误
          console.error(error);
        }
      });
    }

  }
}

</script>



<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h3 {
  margin: 40px 0 0;
}
ul {
  list-style-type: none;
  padding: 0;
}
li {
  display: inline-block;
  margin: 0 10px;
}
a {
  color: #42b983;
}

label {
  margin-right: 10px; /* 标签与输入框之间的右边距 */
  align-items: flex-start; /* 左对齐 */
}

table {
  border-collapse: collapse;
  width: 100%;
}

th, td {
  border: 1px solid #ccc;
  padding: 8px;
  text-align: left;
}

th {
  background-color: #f2f2f2;
}
</style>
