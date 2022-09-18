<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import { waitFor } from 'xstate/lib/waitFor';
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { onBeforeMount, onBeforeUnmount, reactive } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import Tree from '@/components/show/Tree.vue';
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const { getPollingLoop, pullResultsLoop, interruptJob, getCommonResEffect } = fetchS
const fetchM = useInterpret(permachine)
const loop = pullResultsLoop(pollingM)
const pollResults = reactive<TreeNode[]>([])
const enhancer = reactive(new Map())

/**
 * 打算引入动态的堆叠图，但是不知道timestamp还有cost 应该是rt，估计得找后端去补这个接口
 */


getCommonResEffect(pollingM, body => {
  if (body.results.length > 0) {
    body.results.forEach(result => {
      if (result.type === "trace") {

        const trans = (root: TraceNode): Map<string, string[]> => {
          /** 用于cmdRes */
          const map = new Map(Object
            .entries(root)
            .filter(([k, v]) => "children" !== k)
            .map(([k, v]) => [k, [v.toString()]]
            )
          ) as Map<string, string[]>
          /**显示简略信息 */
          let title = ""
          if (root.type === "throw") {
            title = "throw"
          } else if (root.type === "thread") {
            title = `${root.timestamp} ${root.threadName}`
          } else {
            title = `[${root.totalCost}ms]${root.className}::${root.methodName}`
          }
          map.set("title", [title])
          return map
        }
        /**处理Tree */
        const dfs = (root: TraceNode): TreeNode => {
          return {
            children: root.children?.map(child => dfs(child))||[],
            meta: trans(root)
          }
        }
        const root: TreeNode = {
          children: result.root?.children?.map(ch => dfs(ch))||[],
          meta: trans(result.root)
        }

        pollResults.unshift(root)
      }
      if (result.type === "enhancer") {
        enhancer.clear()
        enhancer.set("success", [result.success])
        for (const k in result.effect) {
          enhancer.set(k, [result.effect[k as "cost"]])
        }
      }
      if(result.type === "status"){
        console.log(result)
        if(result.statusCode === 0 && enhancer.size > 0) {
          console.log("close!!!")
          loop.close()
        }
      }
    })
  }
})
onBeforeMount(() => {
  pollingM.send("INIT")
})
onBeforeUnmount(() => {
  loop.close()
})
const submit = async (classI: Item, methI: Item) => {
  fetchM.start()
  fetchM.send("INIT")
  fetchM.send({
    type: "SUBMIT",
    value: {
      action: "async_exec",
      command: `trace -n 20 ${classI.value} ${methI.value} --skipJDKMethod false`,
      sessionId:undefined
    }
  })
  const state = await waitFor(fetchM, state => state.hasTag("result"))

  if (state.matches("success")) {
    enhancer.clear()
    pollResults.length = 0
    loop.open()
  }
  fetchM.stop()
}
</script>
  
  <template>
  <MethodInput :submit-f="submit" class="mb-4"></MethodInput>
  <template v-if="pollResults.length > 0 || enhancer.size > 0">
    <CmdResMenu title="enhancer" :map="enhancer" open></CmdResMenu>
    <ul class=" pointer-events-auto mt-10 ">
      <template v-for="(result, i) in pollResults" :key="i">
        <Tree :root="result" class=" border-t-2 mb-4 pt-4" >
          <!-- 具体信息的表达 -->
          <template #meta="{ data }">
            <div class="mb-2 ml-2">
              <CmdResMenu :title="data.get('title')[0]" :map="data" class=""></CmdResMenu>
            </div>
          </template>
        </Tree>
      </template>
    </ul>
  </template>
</template>