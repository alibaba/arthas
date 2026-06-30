<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { onBeforeMount, onBeforeUnmount, reactive, ref } from 'vue';
import Tree from '@/components/show/Tree.vue';
import Enhancer from '@/components/show/Enhancer.vue';
import { publicStore } from '@/stores/public';
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const { pullResultsLoop, getCommonResEffect } = fetchS
const fetchM = useInterpret(permachine)
const loop = pullResultsLoop(pollingM)
const pollResults = reactive<TreeNode[]>([])
const enhancer = ref(undefined as EnchanceResult | undefined)
const publiC = publicStore()
const excludeClass = ref("")
const enabled = ref(true)
const trans = (root: TraceNode, parent: TraceNode | null): string[] => {
  let title: (string)[] = []

  if (root.type === "throw") {
    const lineNumber = root.lineNumber <= 0 ? "" : `#${root.lineNumber}`
    title = ["throw:" + root.exception, "lineNumber", lineNumber, `[${root.message}]`]
  } else if (root.type === "thread") {

    title = [
      "ts=" + root.timestamp,
      "thread_name=" + root.threadName,
      "daemon=" + root.daemon.toString(),
      "priority=" + root.priority.toString(),
      "threadId=" + root.threadId.toString(), `TCCL=${root.classloader}`]
  } else {
    const lineNumber = root.lineNumber <= 0 ? "" : `#${root.lineNumber}`
    let percentage = ""

    if (parent && parent.type === "method") percentage = `${(root.totalCost / parent.totalCost * 100).toFixed(2)}%, `

    if (root.times <= 1) {
      console.log(
        root.cost,
        root.totalCost,
      )
      if (parent && parent.type === "method") percentage = `${(root.cost / parent.totalCost * 100).toFixed(2)}%, `
      title = [`[${percentage}${publiC.nanoToMillis(root.cost)}ms]`, lineNumber, `${root.className}:${root.methodName}`]
    } else {
      if (parent && parent.type === "method") percentage = `${(root.totalCost / parent.totalCost * 100).toFixed(2)}%, `
      title = [
        `[`,
        percentage,
        `min=${publiC.nanoToMillis(root.minCost)}ms, max =${publiC.nanoToMillis(root.maxCost)}ms, total=${publiC.nanoToMillis(root.totalCost)}ms, count=${root.times}]`,
        lineNumber,
        `${root.className}:${root.methodName}`]
    }
  }
  return title
}
/**处理Tree */
const dfs = (root: TraceNode, parent: TraceNode | null): TreeNode => {
  return {
    children: root.children?.map(child => dfs(child, root)) || [],
    meta: trans(root, parent) as string[]
  }
}
getCommonResEffect(pollingM, body => {
  if (body.results.length > 0) {
    body.results.forEach(result => {
      if (result.type === "trace") {

        const root: TreeNode = {
          children: result.root?.children?.map(ch => dfs(ch, null)) || [],
          meta: trans(result.root, null)
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
          console.log("close!!!")
          // loop.close()
        }
      }
    })
  }
})

onBeforeMount(() => {
  pollingM.send("INIT")
  fetchS.asyncInit()
})
onBeforeUnmount(() => {
  loop.close()
})

const setExclude = publicStore().inputDialogFactory(excludeClass,
  (raw) => raw,
  (input) => input.value.toString()
)
const submit = (data: { classItem: Item, methodItem: Item, conditon: string, count: number }) => {
  let condition = data.conditon.trim() == "" ? "" : `'${data.conditon.trim()}'`
  // let express = data.express.trim() == "" ? "" : `'${data.express.trim()}'`
  let n = data.count > 0 ? `-n ${data.count}` : ""
  let exclude = excludeClass.value == "" ? "" : `--exclude-class-pattern ${excludeClass.value}`
  let method = data.methodItem.value === "" ? "*" : data.methodItem.value
  let skipJDKMethod = enabled.value ? "" : "--skipJDKMethod false"
  return fetchS.baseSubmit(fetchM, {
    action: "async_exec",
    command: `trace ${data.classItem.value} ${method} ${skipJDKMethod} ${condition} ${n} ${exclude}`,
    sessionId: undefined
  }).then(() => {
    enhancer.value = undefined
    pollResults.length = 0
    loop.open()
  })
}
</script>
  
<template>
  <MethodInput :submit-f="submit" class="mb-2" ncondition ncount>
    <template #others>

      <label class="label cursor-pointer btn-sm ml-2">
          <span class="label-text uppercase font-bold mr-1">skip JDK Method</span>
          <input v-model="enabled" type="checkbox" class="toggle"/>
        </label>
      <button class="btn btn-outline btn-sm ml-2" @click="setExclude">exclude: {{excludeClass}}</button>
    </template>
  </MethodInput>
  <template v-if="pollResults.length > 0 || enhancer">
    <Enhancer :result="enhancer" v-if="enhancer"></Enhancer>
    <ul class=" pointer-events-auto mt-2">
      <template v-for="(result, i) in pollResults" :key="i">
        <Tree :root="result" class=" border-t-2 mb-4 pt-4">
          <!-- 具体信息的表达 -->
          <template #meta="{ data, active }">
            <div class="bg-info  px-2 rounded-r rounded-br mr-2 text-info-content" :class='{
                  "hover:opacity-50":active,
                }'>
              {{data.join(" ")}}
            </div>
          </template>
        </Tree>
      </template>
    </ul>
  </template>
</template>