<script setup lang="ts">
import { onMounted, reactive } from 'vue';
type AgentId = string
const agentInfos: ([AgentId, AgentInfo])[] = reactive([])
function getUrlParam(name: string) {
  const urlparam = new URLSearchParams(window.location.search)
  return urlparam.get(name)
}
function tunnelWebConsoleLink(agentId: string, tunnelPort: number, targetServer: string) {
  return `/?targetServer=${targetServer}&port=${tunnelPort}&agentId=${agentId}`;
}

const fetchMyApps = () => {
  const appName = getUrlParam("app") ?? ""
  let url = `/api/tunnelAgentInfo?app=${appName}`
  fetch(url)
    .then((response) => response.json())
    .then((data: Record<AgentId, AgentInfo>) => {
      for (const key in data) {
        agentInfos.push(
          [key, data[key] as AgentInfo]
        )
      }

    })
    .catch((error) => console.error('api error ' + error))

}
onMounted(() => {
  fetchMyApps()
})
</script>

<template>
  <table class="table table-normal w-[100vw]">
    <thead>
      <tr >
        <th class="normal-case">IP</th>
        <th class="normal-case">AgentId</th>
        <th class="normal-case">Version</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(agentInfoRecord) in agentInfos" :key="agentInfoRecord[0]" class="hover">
        <td>
          <a class="btn btn-primary btn-sm"
            :href="tunnelWebConsoleLink(agentInfoRecord[0], agentInfoRecord[1].clientConnectTunnelPort, agentInfoRecord[1].clientConnectHost)">{{
                agentInfoRecord[1].host
            }}</a>
        </td>
        <td>
          {{ agentInfoRecord[0] }}
        </td>
        <td>
          {{ agentInfoRecord[1].arthasVersion }}
        </td>
      </tr>
    </tbody>
  </table>
</template>