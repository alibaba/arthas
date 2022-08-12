<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, ref } from 'vue';
import { Disclosure, DisclosureButton, DisclosurePanel } from '@headlessui/vue';
import CmdResMenu from '@/components/CmdResMenu.vue';
import { publicStore } from "@/stores/public"
import transformMachine from '@/machines/transformConfigMachine';
const { getCommonResEffect } = publicStore()
const allClassM = useMachine(machine)
const urlStatM = useMachine(machine)
const actor = useMachine(transformMachine)
onBeforeMount(() => {
  allClassM.send("INIT")
  allClassM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: "classloader -a"
    }
  })
  urlStatM.send("INIT")
  urlStatM.send({
    type: "SUBMIT",
    value: {
      action: "exec",
      command: "classloader --url-stat"
    }
  })
})
const map = ref([] as [string, Map<"hash" | "parent" | "classes", string[]>][])
const urlStats = ref([] as [
  string,
  Map<"hash" | "unUsedUrls" | "usedUrls", string[]>
][])

getCommonResEffect(allClassM, body => {
  console.log("all", body)
  body.results.filter(res => res.type === "classloader").reduce((pre, cur) => {
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
})

getCommonResEffect(urlStatM, body => {
  console.log("urlStatM", body)
  const result = body.results[0]
  if (result.type === "classloader" && Object.hasOwn(result, "urlStats")) {
    Object.entries(result.urlStats).forEach(([k, v]) => {
      actor.service.start()
      actor.send("INPUT", {
        data: k
      })
      if (actor.state.value.matches("failure")) {
        publicStore().$patch({
          isErr: true,
          ErrMessage: actor.state.value.context.err
        })
      } else {
        const obj = actor.state.value.context.output as Record<"hash" | "name", string>
        urlStats.value.push([
          obj.name,
          new Map([
            ["hash", [obj.hash]],
            ["unUsedUrls", v.unUsedUrls],
            ["usedUrls", v.usedUrls]
          ])
        ])
      }

    })
  }
})


</script>

<template>
  <div class="p-2 overflow-y-scroll w-full flex flex-col">
    <Disclosure>
      <DisclosureButton class="w-1/3 bg-blue-500 h-10 p-2 rounded mb-2">
        all classloader
      </DisclosureButton>
      <DisclosurePanel>
        <li v-for="v in map" :key="v[0]" class="flex flex-col">
          <CmdResMenu :title="v[0]" :list="['hash', 'parent', 'classes']" :map="v[1]" button-width="w-1/2"></CmdResMenu>
        </li>
      </DisclosurePanel>

    </Disclosure>
    <Disclosure>
      <DisclosureButton class="w-1/3 bg-blue-500 h-10 p-2 rounded mb-2">
        urlStats
      </DisclosureButton>
      <DisclosurePanel>
        <li v-for="v in urlStats" :key="v[0]" class="flex flex-col">
          <CmdResMenu :title="v[0]" :list="['hash', 'unUsedUrls', 'usedUrls']" :map="v[1]" button-width="w-1/2">
          </CmdResMenu>
        </li>
      </DisclosurePanel>

    </Disclosure>
  </div>
</template>

<style scoped>
</style>