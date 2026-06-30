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
      <Form :model="sysGetByKeyModel" :label-width="150" style=" margin-top: 20px; justify-content: center;">
        <FormItem label="key">
          <Input v-model="sysGetByKeyModel.key" placeholder="Enter key..."></Input>
        </FormItem>
        <FormItem label="value">
          <Input disabled v-model="sysGetByKeyModel.value" placeholder="result value..."></Input>
        </FormItem>
        <FormItem>
          <Button type="primary" @click="getByKey()">查询</Button>
        </FormItem>
      </Form>

    </Col>

  </Row>

  <table>
    <thead>
    <tr>
      <th>序号</th>
      <th>操作</th> <!-- 添加操作栏的表头 -->
      <th>key</th>
      <th>value</th>
    </tr>
    </thead>
    <tbody>
    <tr v-for="(item, index) in tableData" :key="index">
      <td>{{ item.index }}</td>
      <td>
        <Button type="primary" @click="handleClick(item)">修改</Button> <!-- 添加按钮，并绑定点击事件 -->
      </td>
      <td>{{ item.key }}</td>
      <td>{{ item.value }}</td>
    </tr>
    </tbody>

    <Modal
        v-model="modal"
        title="修改"
        @on-ok = "sysUpdteByKey"
        @on-cancel="modalCancel">
      key: {{sysUpdateModel.key}}
      <Input v-model="sysUpdateModel.value" placeholder="Enter new value..."></Input>

    </Modal>
  </table>


</template>

<script>
// 引入自动生成的grpc_web相关的文件

import {
  SystemPropertyClient,
} from '@/assets/proto/ArthasServices_grpc_web_pb';


import { Empty } from 'google-protobuf/google/protobuf/empty_pb';
import { StringKey, StringStringMapValue } from '@/assets/proto/ArthasServices_grpc_web_pb';


export default {
  // eslint-disable-next-line vue/multi-word-component-names
  name: 'pwd',
  inject: ['apiHost'],
  data(){
    return {
      sysPropClient: null,
      jobId: 0,
      sysPropResponse: "www",
      tableData: [],
      sysGetByKeyModel:{
        key: "",
        value: "",
      },
      modal: false,
      sysUpdateModel:{
        key: "",
        value: ""
      }
    };
  },


  created() {
    let hostname = this.apiHost;
    this.sysPropClient = new SystemPropertyClient(hostname);
    this.sendSysPropRequest();
    this.metadata = {"Content-Type": "application/grpc-web-text"};
  },

  methods:{

    modalCancel () {
      this.modal = false;
    },
    handleClick(item){
      this.modal = true;
      this.sysUpdateModel.key = item.key
      this.sysUpdateModel.value = item.value
      console.log(item);
    },
    sysUpdteByKey(){
      var sysPropRequest  = new StringStringMapValue();
     sysPropRequest.getStringstringmapMap()
          .set(this.sysUpdateModel.key, this.sysUpdateModel.value);
      const _this = this;
      this.sysPropClient.update(sysPropRequest, {}, (error, response) => {
        if (!error) {
          // 处理成功响应
          _this.jobId = response.getJobid();
          const type = response.getType();
          if(type == "sysprop" && response.hasStringstringmapvalue()){
            var stringstringmapvalue = response.getStringstringmapvalue();
            var value = stringstringmapvalue.getStringstringmapMap().get(this.sysUpdateModel.key)
            if(_this.sysUpdateModel.value == value){
              this.$Notice.open({
                title: '修改成功',
                desc:  this.sysUpdateModel.key + "  " + "成功修改为: " +this.sysUpdateModel.value
              });
              _this.sendSysPropRequest();
              _this.modal = false;
            }
          }
        } else {
          // 处理错误
          console.error(error);
        }
      });

    },
    getByKey(){
      var sysPropRequest  = new StringKey();
      this.sysGetByKeyModel.key = this.sysGetByKeyModel.key.trim();
      sysPropRequest.setKey(this.sysGetByKeyModel.key);
      const _this = this;
      this.sysPropClient.getByKey(sysPropRequest, {}, (error, response) => {
        if (!error) {
          // 处理成功响应
          _this.jobId = response.getJobid();
          const type = response.getType();
          if(type == "sysprop" && response.hasStringstringmapvalue()){
            var stringstringmapvalue = response.getStringstringmapvalue();
            _this.sysGetByKeyModel.value = stringstringmapvalue.getStringstringmapMap().get(this.sysGetByKeyModel.key)
          }
        } else {
          // 处理错误
          console.error(error);
        }
      });

    },
    sendSysPropRequest(){
      var sysPropRequest  = new Empty();
      this.tableData = []
      const _this = this;
      this.sysPropClient.get(sysPropRequest, {}, (error, response) => {
        if (!error) {
          // 处理成功响应
          _this.jobId = response.getJobid();
          const type = response.getType();
          if(type == "sysprop" && response.hasStringstringmapvalue()){
            var stringstringmapvalue = response.getStringstringmapvalue();
            var sysPropResponse = stringstringmapvalue.getStringstringmapMap();
            var index = 1;
            sysPropResponse.forEach((value, key) => {
              var cur_dir = {}
              cur_dir['index'] = index;
              cur_dir['key'] = key;
              cur_dir['value'] = value;
              _this.tableData.push(cur_dir)
              index = index + 1;
            });
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
