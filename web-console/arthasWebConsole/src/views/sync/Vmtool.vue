<script setup lang="ts">
import ClassInput from '@/components/input/ClassInput.vue';
import Tree from '@/components/show/Tree.vue';
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { useInterpret } from '@xstate/vue';
import { reactive } from 'vue';
const pollResults = reactive<TreeNode[]>([])
const gcMachine = useInterpret(permachine)
const fetchS = fetchStore()
const publicS = publicStore()
const forceGc = () => {

  fetchS.baseSubmit(gcMachine, {
    action: "exec",
    command: "vmtool --action forceGc "
  }).then(
    res => publicS.$patch({
      isSuccess: true,
      SuccessMessage: "GC success!",
    })
  )
}

const getInstance = (classI: any) => {
  pollResults.length = 0
  fetchS.baseSubmit(gcMachine, {
    action: "exec",
    command: `vmtool --action getInstances --className ${classI.value} -x 10`
  }).then(
    res => {
      const result = (res as CommonRes).body.results[0]
      if (result.type === "vmtool") {
        let raw = result.value.split("\n")
        const stk: TreeNode[] = []

        // Tree的构建
        raw.forEach(v => {
          let str = v.trim()
          let match = 0
          for (let s of str) {
            if (s === "[") {
              match++
            } else if (s === "]") {
              match--
            }
          }
          const root = {
            children: [],
            meta: str.substring(0, str.length - 1)
          } as TreeNode

          if (match > 0) {
            stk.push(root)
          } else if (match === 0) {
            let cur = stk.pop()
            if (cur) {
              cur.children!.push(root)
              stk.push(cur)
            } else {
              stk.push(root)
            }

          } else {
            /// 默认每行只会一个]
            //!可能会有bug
            let cur = stk.pop()!
            if (stk.length > 0) {
              let parent = stk.pop()!
              parent.children!.push(cur)
              stk.push(parent)
            } else {
              // 构建结束
              stk.push(cur)
            }

          }

          console.log(JSON.stringify(stk))
        })

        pollResults.unshift(stk[0])
      }
    }
  )
}
</script>

<template>

  <div>
    <button @click.prevent="forceGc" class="bg-blue-500 p-2 hover:bg-blue-300 transition rounded-md">forceGc</button>
  </div>
  <ClassInput :submit-f="getInstance" class="mb-4" />
  <template v-if="pollResults.length > 0">
    <ul class=" pointer-events-auto mt-10">
      <template v-for="(result, i) in pollResults" :key="i">
        <Tree :root="result" class="mt-2" button-class=" ">
          <template #meta="{ data, active }">
            <div class="bg-blue-200 p-2 mb-2 rounded-r rounded-br" :class='{"hover:bg-blue-300 bg-blue-400":active}'>
              {{data}}
            </div>
          </template>
        </Tree>
      </template>
    </ul>
  </template>
</template>

<style scoped>

</style>