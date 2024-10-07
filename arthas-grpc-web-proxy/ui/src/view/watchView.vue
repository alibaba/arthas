<template>

  <Form :model="watchRequestModel" :label-width="150" style=" margin-top: 20px; justify-content: center;">
    <div>
      <Row>
        <Col span="6">
          <FormItem label="classPattern">
            <Input v-model="watchRequestModel.classPattern" placeholder="Enter className..."></Input>
          </FormItem>
        </Col>
        <Col span="6">
          <FormItem label="methodPattern">
            <Input v-model="watchRequestModel.methodPattern" placeholder="Enter metheName..."></Input>
          </FormItem>
        </Col>
        <Col span="6">
          <FormItem label="express">
            <Input v-model="watchRequestModel.express" placeholder="Enter express..."></Input>
          </FormItem>
        </Col>
        <Col span="6">
          <FormItem label="conditionExpress">
            <Input v-model="watchRequestModel.conditionExpress" placeholder="Enter conditionExpress..."></Input>
          </FormItem>
        </Col>
      </Row>
    </div>

    <div>
      <Row>
        <Col span="8">
          <FormItem label="situation">
            <RadioGroup v-model="watchRequestModel.situation">
              <Radio label="isBefore">isBefore</Radio>
              <Radio label="isFinish">isFinish</Radio>
              <Radio label="isException">isException</Radio>
              <Radio label="isSuccess">isSuccess</Radio>
            </RadioGroup>
          </FormItem>
        </Col>
        <Col span="6">
          <FormItem label="expand">
            <Input type="number" v-model="watchRequestModel.expand" placeholder="Enter expand..."></Input>
          </FormItem>
        </Col>
        <Col span="6">
          <FormItem label="sizeLimit">
            <Input type="number" v-model="watchRequestModel.sizeLimit" placeholder="Enter sizeLimit..."></Input>
          </FormItem>
        </Col>
        <Col span="4">
          <FormItem label="isRegEx">
            <i-switch v-model="watchRequestModel.isRegEx" size="large">
              <template #open>
                <span>true</span>
              </template>
              <template #close>
                <span>false</span>
              </template>
            </i-switch>
          </FormItem>
        </Col>
      </Row>
    </div>

    <div>
      <Row>
        <Col span="6">
          <FormItem label="numberOfLimit">
            <Input type="number" v-model="watchRequestModel.numberOfLimit" placeholder="Enter numberOfLimit..."></Input>
          </FormItem>
        </Col>
        <Col span="6">
          <FormItem label="excludeClassPattern">
            <Input type="text" v-model="watchRequestModel.excludeClassPattern" placeholder="Enter excludeClassPattern..."></Input>
          </FormItem>
        </Col>
        <Col span="3">
          <FormItem label="verbose">
            <i-switch v-model="watchRequestModel.verbose" size="large">
              <template #open>
                <span>true</span>
              </template>
              <template #close>
                <span>false</span>
              </template>
            </i-switch>
          </FormItem>
        </Col>
        <Col span="9">
          <FormItem label="maxNumOfMatchedClass">
            <Input type="number" v-model="watchRequestModel.maxNumOfMatchedClass" placeholder="Enter maxNumOfMatchedClass..."></Input>
          </FormItem>
        </Col>
      </Row>
    </div>
<!--    <FormItem label="listenerId">-->
<!--      <Input type="number" v-model="watchRequestModel.listenerId" placeholder="Enter listenerId..."></Input>-->
<!--    </FormItem>-->

<!--    <FormItem label="jobId">-->
<!--      <Input type="number" v-model="watchRequestModel.jobId" placeholder="Enter jobId..."></Input>-->
<!--    </FormItem>-->

    <FormItem>
      <Button type="primary" @click="watch" v-bind:disabled="!this.watchEnable">{{this.submitText}}</Button>
      <Button style="margin-left: 8px" @click="stopWatchRequest">Cancel</Button>
      <Button style="margin-left: 8px" @click="clear">清除结果</Button>
    </FormItem>
  </Form>

  <table>
    <thead>
    <tr>
      <th>jobid</th>
      <th>resultid</th>
      <th>ts</th>
      <th>accessPoint</th>
      <th>className</th>
      <th>methodName</th>
      <th>cost</th>
      <th>value</th>
      <th>查看当前结果信息</th> <!-- 添加操作栏的表头 -->
    </tr>
    </thead>
    <tbody>
    <tr v-for="(item, index) in tableData" :key="index">
      <td>{{ item.jobId }}</td>
      <td>{{ item.resultId }}</td>
      <td>{{ item.ts }}</td>
      <td>{{ item.accessPoint }}</td>
      <td>{{ item.className }}</td>
      <td>{{ item.methodName }}</td>
      <td>{{ item.cost }}</td>
      <!--      <td class="preserve-whitespace">-->
      <td>
        <Tree id="tree" :data="item.value"></Tree>
      </td>
      <td>
        <Button type="primary" @click="handleClick(item)">查看结果</Button> <!-- 添加按钮，并绑定点击事件 -->
      </td>
    </tr>
    </tbody>

    <Modal
        v-model="modal"
        title="信息查看"
        @on-cancel="modalCancel">

      <Tag color="primary">JobId: {{ this.objectQueryModel.jobId }}</Tag>
      <Tag color="primary">resultId: {{ this.objectQueryModel.resultId }}</Tag>

      <Form>
        <div>
          <Row>
            <Col span="12">
              <FormItem label="express">
                <Input v-model="this.objectQueryModel.resultExpress" placeholder="Enter express..."></Input>
              </FormItem>
            </Col>
            <Col span="12">
              <FormItem label="expand">
                <Input type="number" v-model="this.objectQueryModel.depth" placeholder="Enter conditionExpress..."></Input>
              </FormItem>
            </Col>
          </Row>
        </div>
      </Form>
      <Button type="warning" @click="sendObjectRequest">重新查询</Button> <!-- 添加按钮，并绑定点击事件 -->
      <h3>value结果:(可以点击重新查询按钮反复筛选查看)</h3>
      <Tree id="treeInModal" :data="this.treeData"></Tree>

    </Modal>
  </table>


</template>

<script>
// 引入自动生成的grpc_web相关的文件

import {
  WatchClient,
  ObjectServiceClient
} from '@/assets/proto/ArthasServices_grpc_web_pb';

import { WatchRequest, ObjectQuery } from '@/assets/proto/ArthasServices_grpc_web_pb';
import {Col} from "view-ui-plus";


export default {
  // eslint-disable-next-line vue/multi-word-component-names
  name: 'watchView',
  components: {Col},
  inject: ['apiHost'],
  data(){
    return {
      watchClient: null,
      objectClient: null,

      modal: false,
      objectQueryModel: {
        className: "demo.MathGame",
        classLoaderHash: 0,
        classLoaderClass: "",
        express: "instances[0]",
        depth: 2,
        limit: 1,

        jobId: 0,
        resultId: 0,
        resultExpress: "",
      },

      metadata: {},
      watchStream: null,
      changeWatchStream: null,
      isWatching: false,
      submitText:"开始watch",
      watchEnable:true,
      watchRequestModel: {
        classPattern: "demo.MathGame",
        methodPattern: "primeFactors",
        express: "{params, target, returnObj}",
        conditionExpress: "",
        isBefore: false,
        isFinish: true,
        isException: false,
        isSuccess: false,
        situation: "isFinish",
        expand: 2,
        sizeLimit: 10 * 1024 * 1024,
        isRegEx: false,
        numberOfLimit: 10,
        excludeClassPattern: "",
        listenerId: 0,
        verbose: false,
        maxNumOfMatchedClass: 50,
        jobId: 0
      },

      tableData: [], // 存储表格数据的数组
      treeData: [],
    };
  },


  created() {
    let hostname = this.apiHost;
    this.watchClient = new WatchClient(hostname);
    this.objectClient = new ObjectServiceClient(hostname)
    this.metadata = {"Content-Type": "application/grpc-web-text"};
  },

  methods:{
    modalCancel () {
      this.modal = false;
    },


    handleClick(item) {
      // 在这里处理按钮点击事件，可以使用item对象获取当前行的信息
      this.objectQueryModel.className = "com.taobao.arthas.grpcweb.grpc.service.GrpcJobController"
      this.objectQueryModel.resultExpress = "{params, target, returnObj}"
      this.objectQueryModel.jobId = item.jobId;
      this.objectQueryModel.resultId = item.resultId;
      this.objectQueryModel.type = item.type;
      this.objectQueryModel.express = "instances[0].{jobs}.get(0).get(" + item.jobId + "L).{listener}.{results}.get(0).get(" + item.resultId + "L)"
      let value = item.value[0];
      let copiedObject = JSON.parse(JSON.stringify(value));
      this.treeData = [copiedObject];
      this.modal = true;
    },


    watch(){
      if(this.isWatching){
        this.changeWatchRequest();
      }else {
        this.sendWatchRequest();
      }
    },

    sendWatchRequest(){
      this.watchRequestModel.isBefore = false;
      this.watchRequestModel.isFinish = false;
      this.watchRequestModel.isSuccess = false;
      this.watchRequestModel.isException = false;
      if(this.watchRequestModel.situation == "isBefore"){
        this.watchRequestModel.isBefore = true;
      }else if(this.watchRequestModel.situation == "isFinish"){
        this.watchRequestModel.isFinish = true;
      }else if(this.watchRequestModel.situation == "isSuccess"){
        this.watchRequestModel.isSuccess = true;
      }else {
        this.watchRequestModel.isException = true;
      }
      const watchRequest = new WatchRequest();
      watchRequest.setClasspattern(this.watchRequestModel.classPattern)
          .setMethodpattern(this.watchRequestModel.methodPattern)
          .setExpress(this.watchRequestModel.express)
          .setConditionexpress(this.watchRequestModel.conditionExpress)
          .setIsbefore(this.watchRequestModel.isBefore)
          .setIsfinish(this.watchRequestModel.isFinish)
          .setIsexception(this.watchRequestModel.isException)
          .setIssuccess(this.watchRequestModel.isSuccess)
          .setExpand(this.watchRequestModel.expand)
          .setSizelimit(this.watchRequestModel.sizeLimit)
          .setIsregex(this.watchRequestModel.isRegEx)
          .setNumberoflimit(this.watchRequestModel.numberOfLimit)
          .setExcludeclasspattern(this.watchRequestModel.excludeClassPattern)
          .setListenerid(this.watchRequestModel.listenerId)
          .setVerbose(this.watchRequestModel.verbose)
          .setMaxnumofmatchedclass(this.watchRequestModel.maxNumOfMatchedClass)
          .setJobid(this.watchRequestModel.jobId);

      this.watchStream = this.watchClient.watch(watchRequest,{});
      let _this = this
      // 持续获取流数据并处理
      this.watchStream.on('data', function(response) {
        const jobId = response.getJobid();
        const type = response.getType();
        const resultId = response.getResultid();
        if(type == "watch" && response.hasWatchresponse()){
          _this.isWatching = true;
          const watchResponse = response.getWatchresponse();
          var data = _this.getObject(watchResponse.getValue());
          data['expand'] = true;
          var newData = {
            jobId: jobId,
            resultId: resultId,
            type: type,
            ts: watchResponse.getTs(),
            accessPoint: watchResponse.getAccesspoint(),
            className: watchResponse.getClassname(),
            methodName: watchResponse.getMethodname(),
            cost: watchResponse.getCost(),
            value: [data],
          };
          _this.tableData.unshift(newData);
          _this.watchRequestModel.jobId = jobId;
          _this.submitText = "动态修改条件"
          // _this.watchStream = stream;
        }else {
          console.log("收到的不是watchResponse: ----->")
          console.log('type:', type);
          console.log('message:', response.getStringvalue());
          _this.$Notice.info({
            title: 'watch tips',
            desc: response.getStringvalue()
          });
        }
      });

      this.watchStream.on('status', function(status) {
        console.log("status.code " + status.code);
        console.log("status.details " + status.details);
        console.log("status.metadata " + status.metadata.toString());
      });

      this.watchStream.on('end', function(end) {
        console.log("end: " + end)
        // stream end signal
        _this.watchStream.cancel()
        _this.isWatching = false
        _this.submitText = "开始watch"
        _this.$Notice.info({
          title: 'watch结束',
          desc: 'watch结束'
        });
      });

    },

    changeWatchRequest(){
      this.watchRequestModel.isBefore = false;
      this.watchRequestModel.isFinish = false;
      this.watchRequestModel.isSuccess = false;
      this.watchRequestModel.isException = false;
      if(this.watchRequestModel.situation == "isBefore"){
        this.watchRequestModel.isBefore = true;
      }else if(this.watchRequestModel.situation == "isFinish"){
        this.watchRequestModel.isFinish = true;
      }else if(this.watchRequestModel.situation == "isSuccess"){
        this.watchRequestModel.isSuccess = true;
      }else {
        this.watchRequestModel.isException = true;
      }
      const watchRequest = new WatchRequest();
      watchRequest.setClasspattern(this.watchRequestModel.classPattern)
          .setMethodpattern(this.watchRequestModel.methodPattern)
          .setExpress(this.watchRequestModel.express)
          .setConditionexpress(this.watchRequestModel.conditionExpress)
          .setIsbefore(this.watchRequestModel.isBefore)
          .setIsfinish(this.watchRequestModel.isFinish)
          .setIsexception(this.watchRequestModel.isException)
          .setIssuccess(this.watchRequestModel.isSuccess)
          .setExpand(this.watchRequestModel.expand)
          .setSizelimit(this.watchRequestModel.sizeLimit)
          .setIsregex(this.watchRequestModel.isRegEx)
          .setNumberoflimit(this.watchRequestModel.numberOfLimit)
          .setExcludeclasspattern(this.watchRequestModel.excludeClassPattern)
          .setListenerid(this.watchRequestModel.listenerId)
          .setVerbose(this.watchRequestModel.verbose)
          .setMaxnumofmatchedclass(this.watchRequestModel.maxNumOfMatchedClass)
          .setJobid(this.watchRequestModel.jobId);

      this.changeWatchStream = this.watchClient.watch(watchRequest,{});
      let _this = this
      // 持续获取流数据并处理
      this.changeWatchStream.on('data', function(response) {
        const jobId = response.getJobid();
        const type = response.getType();
        const resultId = response.getResultid();
        if(type != "watch" ){
          console.log('jobId:', jobId);
          console.log('resultId:', resultId);
          console.log('message:', response.getStringvalue());
          _this.$Notice.info({
            title: 'SUCCESS',
            desc: '修改成功'
          });
        }
      });

      this.changeWatchStream.on('status', function(status) {
        console.log("status.code " + status.code);
        console.log("status.details " + status.details);
        console.log("status.metadata " + status.metadata.toString());
      });

      this.changeWatchStream.on('end', function(end) {
        console.log("end: " + end)
        // stream end signal
        _this.changeWatchStream.cancel()
      });
    },

    stopWatchRequest(){
      if(this.isWatching && this.watchStream!=null){
        this.watchStream.cancel();
        this.isWatching = false;
        this.$Notice.warning({
          title: 'Notification',
          desc: '手动停止watch'
        });
      }
      this.submitText = "开始watch"
    },

    clear(){
      this.tableData = []
    },

    sendObjectRequest(){
      const objectRequest = new ObjectQuery();
      objectRequest.setClassname(this.objectQueryModel.className)
          .setLimit(this.objectQueryModel.limit)
          .setDepth(this.objectQueryModel.depth)
          .setJobid(this.objectQueryModel.jobId)
          .setResultid(this.objectQueryModel.resultId)
          .setExpress(this.objectQueryModel.express)
          .setResultexpress(this.objectQueryModel.resultExpress)

      this.objectClient.query(objectRequest, {}, (error, response) => {
        if (!error) {
          this.treeData = []
          // 处理成功响应
          console.log("response", response)
          console.log("response.sucess", response.getSuccess())
          console.log("response.message", response.getMessage())
          const objectList = response.getObjectsList()
          objectList.forEach(item =>{
            const data = this.getObject(item);
            data['expand'] = true;
            this.treeData.push(data)
          })

        } else {
          // 处理错误
          console.error(error);
        }
      });

    },

    getBasicvalue(obj){
      const aMap = {}
      let value;
      let type;
      if(obj.hasInt()){
        value = obj.getInt();
        type = "java.lang.Integer";
      }else if(obj.hasLong()){
        value = obj.getLong()
        type = "java.lang.Long";
      }else if(obj.hasFloat()){
        value = obj.getFloat()
        type= "java.lang.Float";
      }else if(obj.hasDouble()){
        value =obj.getDouble()
        type = "java.lang.Double";
      }else if(obj.hasBoolean()){
        value = obj.getBoolean()
        type = "java.lang.Boolean"
      }else if(obj.hasString()){
        value =obj.getString()
        type = "java.lang.String"
      }
      aMap['title'] = value + " (@" +type;
      return aMap
    },

    getArrayElements(obj){
      const aMap = {}
      let title = "element";
      try {
        title = obj.getName()
      }catch (e){
        try {
          title = obj.getClassname()
        }catch (e){
          console.log()
        }
      }
      if(obj.hasObjectvalue()){
        aMap['title'] = title +  " (@Object";
        aMap['children'] = []
        aMap['children'].push(this.getObject(obj.getObjectvalue()))
      } else if(obj.hasBasicvalue()){
        const basicValue = obj.getBasicvalue()
        if(title == "element" || this.isBasicType(title)){
          aMap['title'] = this.getBasicvalue(basicValue)['title'];
        }else{
          aMap['title'] = title + " (@Basic";
          aMap['children'] = []
          aMap['children'].push(this.getBasicvalue(basicValue))
        }
      }else if(obj.hasArrayvalue()){
        const arrayValue = obj.getArrayvalue()
        aMap['title'] = title + " (@ArrayList";
        aMap['children'] = []
        const elementsList = arrayValue.getElementsList();
        elementsList.forEach(item=>{
          aMap['children'].push(this.getArrayElements(item))
        })
      }else if(obj.hasNullvalue()){
        aMap['title'] = title + " (@null";
        aMap['children'] = []
        aMap['children'].push({"title":"(null)" + obj.getNullvalue().getClassname()})
      }else if(obj.hasUnexpandedobject()){
        aMap['title'] = title +" (@Unexpand";
        aMap['children'] = [];
        aMap['children'].push({"title":" (Unexpand) " +obj.getUnexpandedobject().getClassname()})
      }
      return aMap;
    },


    getObject(obj){
      const aMap = {}
      let title = "";
      try {
        title = obj.getName()
      }catch (e){
        try {
          title = obj.getClassname()
        }catch (e){
          console.log()
        }
      }

      if(obj.hasObjectvalue()){
        aMap['title'] = title +  " (@Object";
        aMap['children'] = []
        aMap['children'].push(this.getObject(obj.getObjectvalue()))
      } else if(obj.hasBasicvalue()){
        const basicValue = obj.getBasicvalue()
        if(title == "" || this.isBasicType(title)){
          aMap['title'] = this.getBasicvalue(basicValue)['title'];
        } else{
          aMap['title'] = title+ " (@Basic";
          aMap['children'] = []
          aMap['children'].push(this.getBasicvalue(basicValue))
        }
      }else if(obj.hasArrayvalue()){
        const arrayValue = obj.getArrayvalue()
        aMap['title'] =title +  " (@ArrayList";
        aMap['children'] = []
        const elementsList = arrayValue.getElementsList();
        elementsList.forEach(item=>{
          aMap['children'].push(this.getArrayElements(item))
        })
      }else if(obj.hasNullvalue()){
        aMap['title'] = title + " (@null";
        aMap['children'] = []
        aMap['children'].push({"title":"(@null)" + obj.getNullvalue().getClassname()})
      }else if(obj.hasCollection()){
        aMap['title'] = title+ " (@Collection";
        aMap['children'] = []
        const javaObjectList = obj.getCollection().getElementsList()
        javaObjectList.forEach(item =>{
          aMap['children'].push(this.getObject(item))
        })
      }else if(obj.hasMap()){
        aMap['title'] = title + " (@Map";
        aMap['children'] = [];
        const entriesList = obj.getMap().getEntriesList()
        entriesList.forEach(item =>{
          const bMap = {}
          const keyMap = {}
          const valueMap = {}
          bMap['title'] = "Entry"
          bMap['children'] = []
          keyMap['title'] = "key"
          keyMap['children']= []
          keyMap['children'].push(this.getObject(item.getKey()))
          bMap['children'].push(keyMap)
          valueMap['title'] = "value"
          valueMap['children']= []
          valueMap['children'].push(this.getObject(item.getValue()))
          bMap['children'].push(valueMap)
          aMap['children'].push(bMap)
        })
      }else if(obj.hasUnexpandedobject()){
        aMap['title'] = title +" (@Unexpand";
        aMap['children'] = [];
        aMap['children'].push({"title":" (@Unexpand) " + obj.getUnexpandedobject().getClassname()})
      }else if(obj.hasFields()){
        aMap['title'] = title;
        aMap['children'] = []
        const fieldsList = obj.getFields().getFieldsList();
        fieldsList.forEach(item =>{
          aMap['children'].push(this.getObject(item))
        })
      }
      return aMap;
    },

    isBasicType(type){
      if(type == "java.lang.String" || type == "java.lang.Integer" || type == "java.lang.Long"
          || type == "java.lang.Float" || type == "java.lang.Double" || type == "java.lang.Boolean"){
        return true;
      }
      return false;
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
#tree{
  margin-left: 50px;
  white-space: pre-wrap; /* 保留换行符并折叠连续的空白字符 */
  text-align: left;
}
</style>
