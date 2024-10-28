<script setup lang="ts">
import { onMounted, reactive } from 'vue';
import axios from 'axios';

const javaProcessInfos: JavaProcessInfo[] = reactive([])
const urlParams = new URLSearchParams(window.location.search);
const ip = urlParams.get('ip');
const httpPort = urlParams.get('httpPort');
const wsPort = urlParams.get('wsPort');

const getProxyAddress = async () => {
  const requestBody = {
    operation: "findAvailableProxyAddress"
  };

  try {
    const response = await axios.post(window.location.origin + `/api/native-agent-proxy`, requestBody);
    if (response && response.data) {
      return response.data;
    } else {
      console.error('Invalid data format received from server.');
      return null;
    }
  } catch (error) {
    console.error('Error fetching proxy address:', error);
    return null;
  }
}

const fetchNativeAgentList = async () => {

  const proxyAddress = await getProxyAddress();
  if (!proxyAddress) {
    console.error('Failed to get proxy address');
    return;
  }

  let url = `http://`+ proxyAddress + `/api/native-agent-proxy`
  const requestBody = {
    operation: "listProcess",
    agentAddress: ip + ":" + httpPort 
  };
  try {
    const response = await axios.post(url, requestBody);
    if (Array.isArray(response.data)) {
      console.log(response.data)
      javaProcessInfos.splice(0, javaProcessInfos.length, ...response.data);
    } else {
      console.error('Invalid data format received from server.');
    }
  } catch (error) {
    console.error('Error fetching native agent list:', error);
  }
}

const attachJavaProcess =  async (javaProcessInfoRecord: JavaProcessInfo, index: number) => {
  console.log(javaProcessInfoRecord, index)
  let url = `http://`+ ip + `:` + httpPort + `/api/native-agent`
  const requestBody = {
    operation: "attachJvm",
    pid: javaProcessInfoRecord.pid
  };
  try {
    const response = await axios.post(url, requestBody);
    if (response.data) {
      console.log(response.data)
      const newPort = response.data
      // javaProcessInfos[index].arthasServerPort = newPort;
    } else {
      console.error('Invalid data format received from server.');
    }
  } catch (error) {
    console.error('Error fetching native agent list:', error);
  }
}

// const generateMonitorLink = (javaProcessInfoRecord: JavaProcessInfo): string => {
//     return `console.html?ip=` + ip + `&port=` + wsPort;
// };

const doMonitor = async (javaProcessInfoRecord: JavaProcessInfo) => {
  try {
    const proxyAddress = await getProxyAddress();
    if (!proxyAddress) {
      console.error('Failed to get proxy address');
      return;
    }
    let url = `http://`+ proxyAddress + `/api/native-agent-proxy`
    const requestBody = {
      operation: "monitor",
      pid: javaProcessInfoRecord.pid,
      agentAddress: ip + `:` + httpPort
    };
    const response = await axios.post(url, requestBody); 
    if (response.status !== 200) {
      alert('attach失败');
    } else if (response.data === -1) {
        alert('attach失败，端口8563被其他进程占用');
    } else {
      const strRes = proxyAddress.split(':')
      const proxyIp = strRes[0]
      const proxyPort = strRes[1]
      window.location.href = `console.html?ip=${proxyIp}&port=${proxyPort}&nativeAgentAddress=${ip + `:` + wsPort}`;
    }
  } catch (error) {
        console.error('请求出错:', error);
        alert('请求失败，请检查网络或稍后再试。');
  }
}

onMounted(() => {
  fetchNativeAgentList()
})
</script>

<template>
  <table class="table table-normal w-[100vw]">
    <thead>
      <tr >
        <th class="normal-case">Process Name</th>
        <th class="normal-case">Pid</th>
        <th class="normal-case">Option</th>
        <th class="normal-case"><a class="btn btn-primary btn-sm" :href="'/'">back to menu</a></th>
      </tr>
    </thead>
    <tbody>
        <tr v-for="(javaProcessInfoRecord, index) in javaProcessInfos" :key="index" class="hover">
          <td>
            {{javaProcessInfoRecord.processName}}
          </td>
          <td>
            {{ javaProcessInfoRecord.pid}}
          </td>
          <td>
            <!-- <button @click="attachJavaProcess(javaProcessInfoRecord, index)" class="btn btn-primary btn-sm">Attach</button> -->
            <!-- <a class="btn btn-primary btn-sm" :href="generateMonitorLink(javaProcessInfoRecord)">monitor</a> -->
            <a class="btn btn-primary btn-sm" @click="doMonitor(javaProcessInfoRecord)">monitor</a>
          </td>
        </tr>
    </tbody>
  </table>
</template>