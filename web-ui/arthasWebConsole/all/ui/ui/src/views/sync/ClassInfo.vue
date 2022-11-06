<script setup lang="ts">import machine from '@/machines/consoleMachine';
import { publicStore } from '@/stores/public';
import { useMachine } from '@xstate/vue';
import { reactive } from 'vue';
import ClassInput from '@/components/input/ClassInput.vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import { fetchStore } from '@/stores/fetch';
import { interpret } from 'xstate';
import permachine from '@/machines/perRequestMachine';
const classMethodInfoM = useMachine(machine)
const dumpM = useMachine(machine)
const classDetailMap = reactive(new Map<string, string[]>())
const classFields = reactive(new Map<string, string[]>())
const classMethodMap = reactive(new Map<string, string[]>())
const dumpMap = reactive(new Map<string, string[]>())
const { getCommonResEffect } = publicStore()
const fetchS = fetchStore()
getCommonResEffect(classMethodInfoM, body => {
  classMethodMap.clear()
  body.results.forEach(result => {
    if (result.type === "sm" && result.detail == true) {
      classMethodMap.set(result.methodInfo.methodName, Object.entries(result.methodInfo).filter(([k, v]) => k !== "methodName").map(([k, v]) => {
        let res = k + ' : '
        if (!["exceptions", "parameters", "annotations"].includes(k)) res += v.toString()
        else res += JSON.stringify(v)
        return res
      }))

    }
  })
})
getCommonResEffect(dumpM, body => {
  dumpMap.clear()
  body.results.forEach(result => {
    if (result.type === "dump") {
      result.dumpedClasses.forEach(obj => {
        dumpMap.set(obj.name, Object.entries(obj).filter(([k, v]) => k !== "name").map(([k, v]) => {
          let res = k + ' : '
          if (k === "classloader") res += JSON.stringify(v)
          else res += v
          return res
        }))
      })
    }
  })
})
const getClassInfo = (data: { classItem: Item; loaderItem: Item }) => {
  let item = data.classItem
  let classLoader = data.loaderItem.value === "" ? "" : `-c ${data.loaderItem.value}`

  classDetailMap.clear()
  classFields.clear()

  fetchS.baseSubmit(interpret(permachine), {
    action: "exec",
    command: `sc -d -f ${item.value} ${classLoader}`
  }).then(
    res => {
      const result = (res as CommonRes).body.results[0]
      if (result.type === "sc" && result.detailed === true && result.withField === true) {
        Object.entries(result.classInfo).filter(([k, v]) => k !== "fields").forEach(([k, v]) => {
          let value: string[] = []
          if (!["interfaces", "annotations", "classloader", "superClass"].includes(k)) value.push(v.toString())
          else value = v as string[]
          classDetailMap.set(k, value)
        })

        result.classInfo.fields.forEach(field => {
          classFields.set(field.name, Object.entries(field).filter(([k, v]) => k !== "name").map(([k, v]) => {
            if (k === "value") v = JSON.stringify(v)
            return `${k}: ${v}`
          }))
        })
      }
    }
  )
  fetchS.baseSubmit(interpret(permachine), {
    action: "exec",
    command: `sm -d ${item.value} ${classLoader}`
  }).then(res => {
    const result = (res as CommonRes).body.results[0]
    if (result.type === "sm" && result.detail == true) {
      classMethodMap.set(result.methodInfo.methodName, Object.entries(result.methodInfo).filter(([k, v]) => k !== "methodName").map(([k, v]) => {
        let res = k + ' : '
        if (!["exceptions", "parameters", "annotations"].includes(k)) res += v.toString()
        else res += JSON.stringify(v)
        return res
      }))

    }
  })
  fetchS.baseSubmit(interpret(permachine), {
    action: "exec",
    command: `dump ${item.value} ${classLoader}`
  }).then(res => {
    const result = (res as CommonRes).body.results[0]
    if (result.type === "dump") {
      result.dumpedClasses.forEach(obj => {
        dumpMap.set(obj.name, Object.entries(obj).filter(([k, v]) => k !== "name").map(([k, v]) => {
          let res = k + ' : '
          if (k === "classloader") res += JSON.stringify(v)
          else res += v
          return res
        }))
      })
    }
  })
  // classInfoM.send({
  //   type: "SUBMIT",
  //   value: {
  //     action: "exec",
  //     command: `sc -d -f ${item.value} ${classLoader}`
  //   }
  // })
  // classMethodInfoM.send({
  //   type: "SUBMIT",
  //   value: {
  //     action: "exec",
  //     command: `sm -d ${item.value} ${classLoader}`
  //   }
  // })
  // dumpM.send({
  //   type: "SUBMIT",
  //   value: {
  //     action: "exec",
  //     command: `dump ${item.value} ${classLoader}`
  //   }
  // })
}
</script>

<template>
  <ClassInput :submit-f="getClassInfo"></ClassInput>
  <div>
    <template v-if="classDetailMap.size !== 0">
      <h4 class="grid place-content-center mb-2 text-3xl mt-4">classInfo</h4>
      <CmdResMenu :map="classFields" title="fields"></CmdResMenu>
      <CmdResMenu :map="classDetailMap" title="detail"></CmdResMenu>
    </template>
    <template v-if="classMethodMap.size !== 0">
      <CmdResMenu :map="classMethodMap" title="methods"></CmdResMenu>
    </template>
    <template v-if="dumpMap.size !== 0">
      <CmdResMenu :map="dumpMap" title="dump"></CmdResMenu>
    </template>
  </div>
</template>

<style scoped>

</style>