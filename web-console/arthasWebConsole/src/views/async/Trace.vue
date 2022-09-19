<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import { waitFor } from 'xstate/lib/waitFor';
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { onBeforeMount, onBeforeUnmount, reactive, ref } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import Tree from '@/components/show/Tree.vue';
import Enhancer from '@/components/show/Enhancer.vue';
import { count } from 'console';
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const { pullResultsLoop, getCommonResEffect } = fetchS
const fetchM = useInterpret(permachine)
const loop = pullResultsLoop(pollingM)
const pollResults = reactive<TreeNode[]>([])
// const enhancer = reactive(new Map())
const enhancer = ref(undefined as EnchanceResult | undefined)
/**
 * 打算引入动态的堆叠图，但是不知道timestamp还有cost 应该是rt，估计得找后端去补这个接口
 */

// let statusCount = 0
getCommonResEffect(pollingM, body => {
  if (body.results.length > 0) {
    body.results.forEach(result => {
      if (result.type === "trace") {

        // const trans = (root: TraceNode): Map<string, string[]> => {
        //   /** 用于cmdRes */
        //   const map = new Map(Object
        //     .entries(root)
        //     .filter(([k, v]) => "children" !== k)
        //     .map(([k, v]) => [k, [v.toString()]]
        //     )
        //   ) as Map<string, string[]>
        //   /**显示简略信息 */
        //   let title = ""
        //   if (root.type === "throw") {
        //     title = "throw"
        //   } else if (root.type === "thread") {
        //     title = `${root.timestamp} ${root.threadName}`
        //   } else {
        //     title = `[${root.totalCost}ms]${root.className}::${root.methodName}`
        //   }
        //   map.set("title", [title])
        //   return map
        // }
        const trans = (root: TraceNode): string[] => {
          let title: (string)[] = []
          if (root.type === "throw") {
            title = ["throw:" + root.exception, "lineNumber", "#" + root.lineNumber, `[${root.message}]`]
          } else if (root.type === "thread") {

            title = [
              root.timestamp, 
              "thread_name="+root.threadName, 
              "daemon="+root.daemon.toString(), 
              "priority="+root.priority.toString(), 
              "threadId="+root.threadId.toString(), `TCCL=${root.classloader}`]
          } else {
            title = [`[${root.totalCost}ms]`, `${root.className}:${root.methodName}`]
          }
          return title
        }
        /**处理Tree */
        const dfs = (root: TraceNode): TreeNode => {
          return {
            children: root.children?.map(child => dfs(child)) || [],
            meta: trans(root)
          }
        }
        const root: TreeNode = {
          children: result.root?.children?.map(ch => dfs(ch)) || [],
          meta: trans(result.root)
        }

        pollResults.unshift(root)
      }
      if (result.type === "enhancer") {
        enhancer.value = result
      }
      if (result.type === "status") {
        console.log(result)
        // 自动关停，目前有bug，应为interrupt也会出现statusCode，应该计数，目前还没办法解决
        if (result.statusCode === 0) {
          // statusCount--
          console.log("close!!!")
          // loop.close()
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
      command: `trace ${classI.value} ${methI.value} --skipJDKMethod false`,
      sessionId: undefined
    }
  })
  const state = await waitFor(fetchM, state => state.hasTag("result"))

  if (state.matches("success")) {
    enhancer.value = undefined
    pollResults.length = 0
    loop.open()
  }
  fetchM.stop()
}
</script>
  
<template>
  <MethodInput :submit-f="submit" class="mb-2"></MethodInput>
  <template v-if="pollResults.length > 0 || enhancer">
    <Enhancer :result="enhancer" v-if="enhancer"></Enhancer>
    <ul class=" pointer-events-auto mt-2 ">
      <template v-for="(result, i) in pollResults" :key="i">
        <Tree :root="result" class=" border-t-2 mb-4 pt-4">
          <!-- 具体信息的表达 -->
          <template #meta="{ data }">
            <button class="mb-2 ml-2 p-2 button-style rounded-l-none">
              <!-- <CmdResMenu :title="data.get('title')[0]" :map="data" class=""></CmdResMenu> -->
              {{data.join(";")}}
            </button>
          </template>
        </Tree>
      </template>
    </ul>
  </template>
</template>