<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, onUnmounted, reactive, ref } from 'vue';
import { Disclosure, DisclosureButton, DisclosurePanel } from '@headlessui/vue';
import { publicStore } from "@/stores/public"
import { fetchStore } from '@/stores/fetch';
import { interpret } from 'xstate';

import CmdResMenu from '@/components/show/CmdResMenu.vue';
import transformMachine from '@/machines/transformConfigMachine';
import ClassInput from '@/components/input/ClassInput.vue';
import permachine from '@/machines/perRequestMachine';

const { getCommonResEffect } = publicStore()
const classInfoM = useMachine(machine)
const classMethodInfoM = useMachine(machine)
const dumpM = useMachine(machine)
const { getPollingLoop } = fetchStore()
const fetchS = fetchStore()

const map = ref([] as [string, Map<"hash" | "parent" | "classes", string[]>][])
const urlStats = ref([] as [
  string,
  Map<"hash" | "unUsedUrls" | "usedUrls" | "parent", string[]>
][])
const classDetailMap = reactive(new Map<string, string[]>())
const classFields = reactive(new Map<string, string[]>())
const classMethodMap = reactive(new Map<string, string[]>())
const dumpMap = reactive(new Map<string, string[]>())
const json_to_obj = (str: string) => {
  const actor = interpret(transformMachine)
  actor.start()

  actor.send("INPUT", {
    data: str
  })

  return fetchS.isResult(actor).then(
    state => {
      if (state.matches("success")) {
        return Promise.resolve(state.context.output)
      } else {
        publicStore().$patch({
          isErr: true,
          ErrMessage: actor.state.context.err
        })
        return Promise.reject(1)
      }
    }
  ).catch(
    err => {
      return Promise.reject(2)
    }
  )
}
const getUrlStats = () => fetchS.baseSubmit(interpret(permachine), {
  action: "exec",
  command: "classloader --url-stat"
}).then(res => {
  let result = (res as CommonRes).body.results[0]
  if (result.type === "classloader" && Object.hasOwn(result, "urlStats")) {
    urlStats.value.length = 0
    Object.entries(result.urlStats).forEach(([k, v]) => {
      json_to_obj(k).then(
        obj => {
          urlStats.value.push([
            obj.name,
            new Map([
              ["parent", [obj.parent]],
              ["hash", [obj.hash]],
              ["unUsedUrls", v.unUsedUrls],
              ["usedUrls", v.usedUrls]
            ])
          ])
        }
      ).catch(err => {
        console.error(err)
      })
    })
  }
})
const urlStatsLoop = getPollingLoop(getUrlStats
)
const getAllClass = () => fetchS.baseSubmit(interpret(permachine), {
  action: "exec",
  command: "classloader -a"
}).then(res => {
  const results = (res as CommonRes).body.results
  results.filter(res => res.type === "classloader").reduce((pre, cur) => {
    if (cur.type === "classloader" && Object.hasOwn(cur, "classSet")) {
      const classSet = cur.classSet
      const classes = classSet.classes
      if (classSet.segment === 0) {
        const listMap = new Map<"hash" | "parent" | "classes", string[]>([
          ["hash", [classSet.classloader.hash]],
          ["parent", [classSet.classloader.parent]],
          ["classes", classes]
        ])
        map.value.push([classSet.classloader.name, listMap])
      } else {
        const listMap = map.value[map.value.length - 1][1]
        listMap.set("classes", [...listMap.get("classes")!, ...classes])
      }
    }
    return pre
  }, [] as string[][])
}, err => {
  console.error(err)
})


getCommonResEffect(classInfoM, body => {
  const result = body.results[0]
  if (result.type === "sc" && result.detailed === true && result.withField === true) {

    classDetailMap.clear()
    classFields.clear()

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
})
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

onBeforeMount(() => {
  classInfoM.send("INIT")
  classMethodInfoM.send("INIT")
  dumpM.send("INIT")
})
onUnmounted(() => {
  urlStatsLoop.close()
})

const getClassInfo = (data: { classItem: Item; loaderItem: Item }) => {
  let item = data.classItem
  let classLoader = data.loaderItem.value === "" ? "" : `-c ${data.loaderItem.value}`
  classInfoM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: `sc -d -f ${item.value} ${classLoader}`
    }
  })
  classMethodInfoM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: `sm -d ${item.value} ${classLoader}`
    }
  })
  dumpM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: `dump ${item.value} ${classLoader}`
    }
  })
}

</script>

<template>
  <Disclosure>
    <DisclosureButton class="w-1/3 bg-blue-500 h-10 p-2 rounded mb-2" @click.pervent="getAllClass">
      all classloader
    </DisclosureButton>
    <DisclosurePanel>
      <li v-for="v in map" :key="v[0]" class="flex flex-col">
        <CmdResMenu :title="v[0]" :list="['hash', 'parent', 'classes']" :map="v[1]" button-width="w-1/2"></CmdResMenu>
      </li>
    </DisclosurePanel>

  </Disclosure>
  <Disclosure>
    <DisclosureButton class="w-1/3 bg-blue-500 h-10 p-2 rounded mb-2 " @click="getUrlStats">
      urlStats
    </DisclosureButton>
    <DisclosurePanel>
      <!-- <div class="flex items-center my-2 w-10/12 justify-end">
        <div class="mr-4">是否实时更新</div>
        <PlayStop :play-fn="urlStatsPlay" :stop-fn="urlStatsStop" :default-enabled="urlStatsLoop.isOn()"
          class="w-10 h-10"></PlayStop>
      </div> -->
      <div v-for="v in urlStats" :key="v[0]" class="flex flex-col">
        <CmdResMenu :title="v[0]" :map="v[1]" button-width="w-1/2" open>
        </CmdResMenu>
      </div>
    </DisclosurePanel>
  </Disclosure>
  <Disclosure>
    <DisclosureButton class="w-1/3 bg-blue-500 h-10 p-2 rounded mb-2  ">
      classInfo
    </DisclosureButton>
    <DisclosurePanel class="border-t-2 py-4 mt-4">
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
    </DisclosurePanel>
  </Disclosure>
</template>

<style scoped>

</style>