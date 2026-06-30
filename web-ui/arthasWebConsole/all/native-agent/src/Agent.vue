<script setup lang="ts">
import { onMounted, reactive } from 'vue';
import axios from 'axios';

const agentInfos: NativeAgentInfo[] = reactive([])
const fetchNativeAgentList = async () => {
  let url = window.location.origin + `/api/native-agent`
  const requestBody = {
    operation: "listNativeAgent"
  };
  try {
    const response = await axios.post(url, requestBody);
    if (Array.isArray(response.data)) {
      agentInfos.splice(0, agentInfos.length, ...response.data);
    } else {
      console.error('Invalid data format received from server.');
    }
  } catch (error) {
    console.error('Error fetching native agent list:', error);
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
        <th class="normal-case">IP</th>
        <th class="normal-case">http-port</th>
        <th class="normal-case">ws-port</th>
        <th class="normal-case">Option</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(agentInfoRecord) in agentInfos" :key="agentInfoRecord.ip" class="hover">
        <td>
          {{agentInfoRecord.ip }}
        </td>
        <td>
          {{ agentInfoRecord.httpPort }}
        </td>
        <td>
          {{ agentInfoRecord.wsPort }}
        </td>
        <td>
          <a class="btn btn-primary btn-sm" :href="'processes.html?ip=' + agentInfoRecord.ip + '&httpPort=' + agentInfoRecord.httpPort + '&wsPort=' + agentInfoRecord.wsPort">view java processes info</a>
        </td>
      </tr>
    </tbody>
  </table>
</template>