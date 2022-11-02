<script setup lang="ts">
import { computed, onBeforeMount, reactive, ref } from 'vue';
import { publicStore } from "@/stores/public"
import { fetchStore } from '@/stores/fetch';
import { interpret } from 'xstate';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import transformMachine from '@/machines/transformConfigMachine';
import permachine from '@/machines/perRequestMachine';
import Tree from '@/components/show/Tree.vue';
const fetchS = fetchStore()

const urlStats = ref([] as [
  string,
  Map<"hash" | "unUsedUrls" | "usedUrls" | "parent", string[]>,
  string
][])
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
            ]),
            obj.hash
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
        map.set("loadedCount", result.classLoaderStats[name].loadedCount)
        map.set("numberOfInstance", result.classLoaderStats[name].numberOfInstance)
        // "loadedCount"|"numberOfInstance"
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
  selectedClassLoadersUrlStats.value = []
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
const resetClassloader = () => {
  selectClassLoader({ hash: "", name: "", count: "" })
}
</script>

<template>
  <div class="flex flex-col h-full justify-between">
    <div class="flex h-[40vh]">
      <div class="card rounded-box border transition-all duration-500 bg-base-100 mr-2 " :class='{
        "w-full": loaderCache.hash === "",
        "w-2/3": loaderCache.hash !== ""
      }'>
        <div class="card-body h-full p-4 mb-2 ">
          <!-- 后置为了让用户能注意到右上角的refreshicon -->
          <div class="h-[5vh] mb-4 justify-end flex">
            <button @click="resetClassloader" class="btn btn-primary btn-sm mr-1">reset</button>
            <button @click="getClassLoaderTree" class="btn btn-primary btn-sm">refresh</button>
          </div>

          <div class="overflow-auto w-full flex-1">
            <div v-for="(tree, i) in classLoaderTree" :key="i">
              <Tree :root="tree">
                <template #meta="{ data, active }">
                  <!-- <div class="flex items-center"> -->
                  <div class="bg-info  px-2 rounded-r rounded-br mr-2 text-info-content" :class='{
                    "hover:opacity-50": active,
                    "bg-success text-success-content": loaderCache.hash === data[2]
                  }'>
                    {{ data[1] }}
                    <!-- </div> -->
                  </div>
                </template>
                <template #others="{ data }">
                  <div class="items-center flex">
                    <div class="mr-2">
                      <span class="bg-primary-focus text-primary-content border border-primary-focus px-2 rounded-l ">
                        count :
                      </span>
                      <span class="bg-base-200 border border-primary-focus rounded-r px-1 ">
                        {{ data[0] }}
                      </span>
                    </div>
                    <div class="mr-2">
                      <span class="bg-primary-focus px-2 rounded-l text-primary-content border border-primary-focus">
                        hash :
                      </span>
                      <span class="bg-base-200 rounded-r flex-1 px-1 border border-primary-focus">
                        {{ data[2] }}
                      </span>
                    </div>
                    <!-- <div class="">count:{{data[0]}}</div> -->
                    <button @click="selectClassLoader({ name: data[1], hash: data[2], count: data[0] })"
                      class="btn btn-primary btn-xs btn-outline opacity-0 group-hover:opacity-100"
                      v-if="data[2] !== 'null'">
                      select classloader
                    </button>
                  </div>
                </template>
              </Tree>
            </div>
          </div>
        </div>
      </div>
      <div class="card rounded-box border bg-base-100" :class='{
        "w-0 border-none": loaderCache.hash === "",
        "input-btn-style w-1/3": loaderCache.hash !== ""
      }'>
        <div class="card-body ml-2 overflow-y-scroll transition-all duration-500">

          <div class="mb-2">
            <div class="overflow-auto">
              <span class="bg-primary-focus px-2 rounded-l text-primary-content border border-primary-focus">
                selected classLoader:
              </span>
              <span class="bg-base-200 rounded-r px-1 border border-primary-focus">
                {{ loaderCache.name }}
              </span>
            </div>
            <div class="mr-2">
              <span class="bg-primary-focus px-2 rounded-l text-primary-content border border-primary-focus">
                loadedcount :
              </span>
              <span class="bg-base-200 rounded-r px-1 border border-primary-focus">
                {{ loaderCache.count }}
              </span>
            </div>
            <div class="mr-2">
              <span class="bg-primary-focus px-2 rounded-l text-primary-content border border-primary-focus">
                hash :
              </span>
              <span class="bg-base-200 rounded-r px-1 border border-primary-focus">
                {{ loaderCache.hash }}
              </span>
            </div>
          </div>
          <template v-if="loaderCache.hash.trim() !== ''">
            <div class="flex mb-2 w-full">
              <div class=" cursor-default 
          flex-auto
        overflow-hidden rounded-lg bg-white text-left border 
        focus-within:outline outline-2
        hover:shadow-md transition mr-2">
                <input class="w-full border-none py-2 pl-3 pr-10 leading-5 text-gray-900 focus-visible:outline-none"
                  v-model="classVal" />
              </div>
              <button @click="loadClass" class="btn btn-primary btn-sm btn-outline">load class</button>
            </div>
            <div class="flex w-full">
              <div class=" cursor-default 
          flex-auto
        overflow-hidden rounded-lg bg-white text-left border 
        focus-within:outline outline-2
        hover:shadow-md transition mr-2">
                <input class="w-full border-none py-2 pl-3 pr-10 leading-5 text-gray-900 focus-visible:outline-none"
                  v-model="resourceVal" />
              </div>
              <button @click="loadResource" class="btn btn-primary btn-sm btn-outline">load resource</button>
            </div>
            <div class="h-0 border my-2"></div>
            <div class="flex justify-between">
              <h3 class="text-xl flex-1 flex justify-center">urls</h3><button class="btn btn-primary btn-sm"
                @click="getUrlStats">refresh</button>
            </div>
            <ul class="mt-2 w-full flex flex-col">
              <li v-for="(url, i) in selectedClassLoadersUrlStats" :key="i"
                class="bg-blue-200 mb-2 p-2 break-all w-full">
                {{ url }}</li>
            </ul>
          </template>
          <!-- </div> -->
        </div>
      </div>
    </div>
    <!-- 下面的3格 -->
    <div class="w-full flex-auto flex h-[40vh] mt-2">
      <div class="card w-1/3 mr-2 bg-base-100 border">
        <div class="card-body overflow-y-scroll">
          <div class=" mb-2 flex items-center justify-end">
            <h3 class="text-xl flex-1 flex justify-center">urlStats</h3>
            <button class="btn btn-primary btn-sm" @click="getAllUrlStats">refresh</button>
          </div>
          <div v-for="v in urlStats" :key="v[0]" class="flex flex-col">
            <CmdResMenu :title="v[0]" :map="v[1]" button-width="w-full" :button-accent="v[2] === loaderCache.hash">
            </CmdResMenu>
          </div>

        </div>
      </div>
      <div class="card h-full w-2/3 border bg-base-100">
        <div class="card-body overflow-auto">
          <div class="flex justify-around mb-2">
            <h3 class="text-xl">statistics categorized by class type</h3>
            <button class="btn btn-primary btn-sm" @click="getCategorizedByClassType">refresh</button>
          </div>
          <div class="overflow-auto">
            <table class="table w-full table-compact">
              <thead>
                <tr>
                  <th></th>
                  <th class=" normal-case" v-for="(v, i) in keyList" :key="i" :class="{ 'group-first:z-0': i == 0 }">
                    {{ v }}
                  </th>
                </tr>
              </thead>
              <tbody class="">
                <tr v-for="(map, i) in tableResults" :key="i">
                  <th>{{ i + 1 }}</th>
                  <template v-for="(key, j) in keyList">
                    <td class="">
                      {{ map.get(key) }}
                    </td>
                  </template>

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