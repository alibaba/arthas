<script setup lang="ts">
import ClassInput from '@/components/input/ClassInput.vue';
import Tree from '@/components/show/Tree.vue';
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { useInterpret } from '@xstate/vue';
import { reactive, ref } from 'vue';
const pollResults = reactive<TreeNode[]>([])
const gcMachine = useInterpret(permachine)
const fetchS = fetchStore()
const publicS = publicStore()
const depth = ref(1)
const {increase, decrease} = publicS.numberCondition(depth,{min:1})
const setDepth = publicS.inputDialogFactory(
  depth,
  (raw) => {
    let valRaw = parseInt(raw)
    return Number.isNaN(valRaw) ? 1 : valRaw
  },
  (input) => input.value.toString(),
)
const getInstance = (data:{classItem:Item}) => {
  pollResults.length = 0
  fetchS.baseSubmit(gcMachine, {
    action: "exec",
    command: `vmtool --action getInstances --className ${data.classItem.value} -x ${depth.value}`
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

  <ClassInput :submit-f="getInstance" class="mb-4" >
    <template #others>
      <div class="btn-group ml-2">
        <button class="btn btn-outline btn-sm" @click.prevent="decrease">-</button>
        <button class="btn btn-outline btn-sm border-x-0" @click.prevent="setDepth">depth:{{depth}}</button>
        <button class="btn btn-outline btn-sm" @click.prevent="increase">+</button>
      </div>
    </template>
  </ClassInput>
  <template v-if="pollResults.length > 0">
    <ul class=" pointer-events-auto mt-10">
      <template v-for="(result, i) in pollResults" :key="i">
        <Tree :root="result" class="mt-2">
          <template #meta="{ data }">
            <div class="bg-info p-1 mb-1 rounded-r rounded-br">
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