<script setup lang="ts">
import { computed, onBeforeMount, onUnmounted, reactive, ref } from 'vue';
import { Disclosure, DisclosureButton, DisclosurePanel } from '@headlessui/vue';
import { publicStore } from "@/stores/public"
import { fetchStore } from '@/stores/fetch';
import { interpret } from 'xstate';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import transformMachine from '@/machines/transformConfigMachine';
// import ClassInput from '@/components/input/ClassInput.vue';
import permachine from '@/machines/perRequestMachine';
import Tree from '@/components/show/Tree.vue';
const fetchS = fetchStore()

const urlStats = ref([] as [
  string,
  Map<"hash" | "unUsedUrls" | "usedUrls" | "parent", string[]>
][])
const tablelResults = reactive([] as Map<string, string | number>[])
const tableResults = reactive([] as Map<string, string | number>[])
const loaderCache = ref({ name: "", hash: "", count: "" } as Record<"name" | "hash" | "count", string>)
const classLoaderTree = reactive([] as TreeNode[])

const hashCode = computed(() => {
  let res = loaderCache.value.hash.trim() === "" ? "" : "-c " + loaderCache.value.hash.trim()
  if (loaderCache.value.hash.trim() === "null") {
    res = `--classLoaderClass ${loaderCache.value.name}`
  }
  return res
})
const selectedClassLoadersUrlStats = ref([] as string[])
const classVal = ref("")
const resourceVal = ref("")
const keylList = [
  "name", "loadedCount", "hash", "parent"
]
const keyList = [
  "name", "numberOfInstance", "loadedCount"
]
const trans = (root: ClassLoaderNode, parent: ClassLoaderNode | null): string[] => {
  let title: (string)[] = []

  let count = root.loadedCount.toString()
  let name = root.name.split('@')[0]
  let hash = root.hash
  title = [count, name, hash]

  return title
}
/**处理Tree */
const dfs = (root: ClassLoaderNode, parent: ClassLoaderNode | null): TreeNode => {
  let children: TreeNode[] = []

  if ("children" in root) {
    if (root.children) {
      children = root.children.map(child => dfs(child, root))
    }
  }
  return {
    children,
    meta: trans(root, parent) as string[]
  }
}

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
const getAllUrlStats = () => fetchS.baseSubmit(interpret(permachine), {
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
            obj.name.split("@")[0],
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

const getClassLoaderTree = () => fetchS.baseSubmit(interpret(permachine), {
  action: "exec",
  command: "classloader -t"
}).then(res => {
  const results = (res as CommonRes).body.results
  classLoaderTree.length = 0
  results.forEach(result => {
    if (result.type === "classloader" && result.tree) {
      result.classLoaders.forEach(classloader => {
        classLoaderTree.push(dfs(classloader, null))
      })
    }
  })
}, err => {
  console.error(err)
})
// const getCategorizedByLoaded = () => {
//   tablelResults.length = 0
//   fetchS.baseSubmit(interpret(permachine), {
//     action: "exec",
//     command: "classloader -l"
//   }).then(res => {
//     const result = (res as CommonRes).body.results[0]
//     if (result.type === "classloader" && !result.tree) {
//       result.classLoaders.forEach(loader => {
//         const map = new Map()
//         for (const key in loader) {
//           //@ts-ignore
//           if(key == "name") map.set(key, loader[key].split("@")[0])
//           else map.set(key, loader[key])
//         }
//         tablelResults.push(map)
//       })
//     }
//   })
// }
const getCategorizedByClassType = () => {
  tableResults.length = 0
  fetchS.baseSubmit(interpret(permachine), {
    action: "exec",
    command: "classloader"
  }).then(res => {
    const result = (res as CommonRes).body.results[0]
    if (result.type === "classloader") {

      for (const name in result.classLoaderStats) {
        const map = new Map()
        for (const key in result.classLoaderStats[name]) {
          map.set(key, result.classLoaderStats[name][key])
        }
        map.set("name", name)
        tableResults.push(map)
      }
    }
  })
}
onBeforeMount(() => {
  getAllUrlStats()
  getClassLoaderTree()
  // getCategorizedByLoaded()
  getCategorizedByClassType()
})

const loadClass = () => {
  let classItem = classVal.value.trim() === "" ? "" : `--load ${classVal.value.trim()}`
  if (classItem === "") return
  return fetchS.baseSubmit(interpret(permachine), {
    action: "exec",
    command: `classloader ${hashCode.value} ${classItem}`
  }).then(res => {
    let result = (res as CommonRes).body.results[0]
    publicStore().$patch({
      isSuccess: true,
      SuccessMessage: JSON.stringify(result)
    })
  })
}
const loadResource = () => {
  let resourceItem = resourceVal.value.trim() === "" ? "" : `-r ${resourceVal.value.trim()}`
  if (resourceItem === "") return
  return fetchS.baseSubmit(interpret(permachine), {
    action: "exec",
    command: `classloader ${hashCode.value} ${resourceItem}`
  }).then(res => {
    let result = (res as CommonRes).body.results[0]
    publicStore().$patch({
      isSuccess: true,
      SuccessMessage: JSON.stringify(result)
    })
  })
}
const getUrlStats = () => {
  fetchS.baseSubmit(interpret(permachine), {
    action: "exec",
    command: `classloader ${hashCode.value}`
  }).then(res => {
    let result = (res as CommonRes).body.results[0]
    if (result.type === "classloader") {
      selectedClassLoadersUrlStats.value = result.urls
    }
  })
}
const selectClassLoader = (data: { hash: string, name: string, count: string }) => {
  loaderCache.value = data
  getUrlStats()

}
</script>

<template>
  <div class="flex flex-col h-full justify-between">
    <div class="flex h-[40vh]">
      <div class="input-btn-style w-2/3 h-full p-4 mb-2 flex flex-col">
        <!-- 后置为了让用户能注意到右上角的refreshicon -->
        <div class="h-[5vh]m mb-4 justify-end flex">
          <button @click="getClassLoaderTree" class="button-style">refresh</button>
        </div>

        <div class="overflow-auto w-full flex-1">
          <div v-for="(tree,i) in classLoaderTree" :key="i">
            <Tree :root="tree">
              <template #meta="{ data, active }">
                <!-- <div class="flex items-center"> -->
                <div class="bg-blue-200 p-2 rounded-r rounded-br mr-2" :class='{
                  "hover:opacity-50 bg-blue-400":active,
                  "bg-red-400":loaderCache.hash=== data[2]
                }'>
                  {{data[1]}}
                  <!-- </div> -->
                </div>
              </template>
              <template #others="{data}">
                <div class="items-center flex">
                  <div class="mr-2">
                    <span class="bg-blue-500 w-44 px-2 rounded-l text-white">
                      count :
                    </span>
                    <span class="border-gray-300 bg-blue-100 rounded-r flex-1 px-1 border bordergre">
                      {{data[0]}}
                    </span>
                  </div>
                  <div class="mr-2">
                    <span class="bg-blue-500 w-44 px-2 rounded-l text-white">
                      hash :
                    </span>
                    <span class="border-gray-300 bg-blue-100 rounded-r flex-1 px-1 border bordergre">
                      {{data[2]}}
                    </span>
                  </div>
                  <!-- <div class="">count:{{data[0]}}</div> -->
                  <button @click="selectClassLoader({name:data[1],hash:data[2],count:data[0]})" class="button-style">
                    select classloader
                  </button>
                </div>
              </template>
            </Tree>
          </div>
        </div>
      </div>
      <div class="input-btn-style w-1/3 ml-2 h-full">
        <div class=" mb-2 flex items-center justify-end"><button class="button-style"
            @click="getAllUrlStats">refresh</button></div>
        <div class="overflow-auto h-[30vh] w-full">
          <Disclosure>
            <DisclosureButton class="w-full bg-blue-500 h-10 p-2 rounded mb-2 ">
              urlStats
            </DisclosureButton>
            <DisclosurePanel static>
              <div class="flex items-center my-2 w-full justify-end">
              </div>
              <div v-for="v in urlStats" :key="v[0]" class="flex flex-col">
                <CmdResMenu :title="v[0]" :map="v[1]" button-width="w-full">
                </CmdResMenu>
              </div>
            </DisclosurePanel>
          </Disclosure>
        </div>
      </div>
    </div>
    <!-- 下面的3格 -->
    <div class="w-full flex-auto flex h-[40vh]">
      <div class="input-btn-style w-1/3 mr-2 overflow-auto">
        <div class="mb-2">
          <div class="overflow-auto">
            <span class="bg-blue-500 w-44 px-2 rounded-l text-white">
              selected classLoader:
            </span>
            <span class="border-gray-300 bg-blue-100 rounded-r flex-1 px-1 border bordergre">
              {{loaderCache.name}}
            </span>
          </div>
          <div class="mr-2">
            <span class="bg-blue-500 w-44 px-2 rounded-l text-white">
              loadedcount :
            </span>
            <span class="border-gray-300 bg-blue-100 rounded-r flex-1 px-1 border bordergre">
              {{loaderCache.count}}
            </span>
          </div>
          <div class="mr-2">
            <span class="bg-blue-500 w-44 px-2 rounded-l text-white">
              hash :
            </span>
            <span class="border-gray-300 bg-blue-100 rounded-r flex-1 px-1 border bordergre">
              {{loaderCache.hash}}
            </span>
          </div>
        </div>

        <div class="flex mb-2 w-full">
          <div class=" cursor-default 
          flex-auto
        overflow-hidden rounded-lg bg-white text-left border 
        focus:outline-none
        hover:shadow-md transition mr-2">
            <input class="w-full border-none py-2 pl-3 pr-10 leading-5 text-gray-900 focus-visible:outline-none"
              v-model="classVal" />
          </div>
          <button @click="loadClass" class="button-style">load class</button>
        </div>
        <div class="flex mb-2 w-full">
          <div class=" cursor-default 
          flex-auto
        overflow-hidden rounded-lg bg-white text-left border 
        focus:outline-none
        hover:shadow-md transition mr-2">
            <input class="w-full border-none py-2 pl-3 pr-10 leading-5 text-gray-900 focus-visible:outline-none"
              v-model="resourceVal" />
          </div>
          <button @click="loadResource" class="button-style">load resource</button>
        </div>
        <!-- <Disclosure> -->
          <!-- <DisclosureButton class="button-style" @click="getUrlStats()">urls</DisclosureButton>
          <DisclosurePanel as="div" static> -->
          <div class="flex justify-between"><h3 class="text-xl flex-1 flex justify-center">urls</h3><button class="button-style" @click="getUrlStats">refresh</button></div>
          <ul class="overflow-auto h-[20vh] mt-2">
            
            <li v-for="(url,i) in selectedClassLoadersUrlStats" :key="i" class="bg-blue-200 mb-2 p-2">{{url}}</li>
          </ul>
          <!-- </DisclosurePanel>
        </Disclosure> -->
      </div>
      <div class="flex flex-col h-full w-2/3">
        <!-- <div class="input-btn-style w-full mr-2 h-1/2">
          <div class="overflow-auto flex-1 h-full">
            <div class="flex justify-end mb-2">
              <button class="button-style" @click="getCategorizedByLoaded">refresh</button>
            </div>
            <table class="border-collapse border border-slate-400 mx-auto">
              <thead>
                <tr>
                  <th class="border border-slate-300 p-2" v-for="(v,i) in keylList" :key="i">{{v}}</th>
                </tr>
              </thead>
              <tbody class="">
                <tr v-for="(map, i) in tablelResults" :key="i">
                  <td class="border border-slate-300 p-2" v-for="(key,j) in keylList" :key="j">
                    {{map.get(key)}}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div> -->
        <div class="input-btn-style w-full mr-2 h-full">
          <div class="overflow-auto flex-1 h-full">
            <div class="flex justify-end mb-2">
              <button class="button-style" @click="getCategorizedByClassType">refresh</button>
            </div>
            <table class="border-collapse border border-slate-400 mx-auto">
              <thead>
                <tr>
                  <th class="border border-slate-300 p-2" v-for="(v,i) in keyList" :key="i">{{v}}</th>
                </tr>
              </thead>
              <tbody class="">
                <tr v-for="(map, i) in tableResults" :key="i">
                  <td class="border border-slate-300 p-2" v-for="(key,j) in keyList" :key="j">
                    {{map.get(key)}}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>