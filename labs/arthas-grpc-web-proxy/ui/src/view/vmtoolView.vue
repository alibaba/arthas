<template>


  <Form :model="objectQueryModel" :label-width="150" style=" margin-top: 20px; justify-content: center;">
    <div>
      <Row>
        <Col span="6">
          <FormItem label="className">
            <Input v-model="objectQueryModel.className" placeholder="Enter className..."></Input>
          </FormItem>
        </Col>
        <Col span="6">
          <FormItem label="ClassLoaderHash">
            <Input v-model="objectQueryModel.classLoaderHash" placeholder="Enter classLoaderHash..."></Input>
          </FormItem>
        </Col>
        <Col span="6">
          <FormItem label="classLoaderClass">
            <Input v-model="objectQueryModel.classLoaderClass" placeholder="Enter classLoaderClass..."></Input>
          </FormItem>
        </Col>
        <Col span="6">
          <FormItem label="limit">
            <Input type="number" v-model="objectQueryModel.limit" placeholder="Enter className..."></Input>
          </FormItem>
        </Col>
      </Row>
    </div>

    <div>
      <Row>
        <Col span="12">
          <FormItem label="depth">
            <Input type="number" v-model="objectQueryModel.depth" placeholder="Enter depth..."></Input>
          </FormItem>
        </Col>
        <Col span="12">
          <FormItem label="express">
            <Input v-model="objectQueryModel.express" placeholder="Enter express..."></Input>
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
      <Button type="primary" @click="sendObjectRequest">查询</Button>
      <Button style="margin-left: 8px" @click="clear">清除结果</Button>
    </FormItem>
  </Form>

  <Row>
    <Col span="8"></Col>
    <Col span="8">
      <Tree id="tree" :data="this.treeData"></Tree>
    </Col>
    <Col span="8"></Col>
  </Row>

</template>

<script>
// 引入自动生成的grpc_web相关的文件

import {
  ObjectServiceClient
} from '@/assets/proto/ArthasServices_grpc_web_pb';

import { ObjectQuery } from '@/assets/proto/ArthasServices_grpc_web_pb';


export default {
  // eslint-disable-next-line vue/multi-word-component-names
  name: 'vmtool',
  inject: ['apiHost'],
  data(){
    return {
      objectClient: null,
      metadata: {},
      objectQueryModel: {
        className: "com.taobao.arthas.grpcweb.grpc.objectUtils.ComplexObject",
        classLoaderHash: 0,
        classLoaderClass: "",
        express: "instances",
        depth: 2,
        limit: 3,
      },
      treeData: [],
    };
  },


  created() {
    let hostname = this.apiHost;
    this.objectClient = new ObjectServiceClient(hostname)
    this.metadata = {"Content-Type": "application/grpc-web-text"};
  },

  methods:{
    clear(){
      this.treeData=[];
    },
    resetAllRequestParams(){
      this.objectQueryModel.className = "demo.MathGame"
      this.objectQueryModel.classLoaderClass = "";
      this.objectQueryModel.classLoaderHash = "";
      this.objectQueryModel.express = "instances[0]"
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
