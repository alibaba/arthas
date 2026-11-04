<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { onBeforeMount, onBeforeUnmount, onMounted, reactive, ref, } from 'vue';
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import Enhancer from '@/components/show/Enhancer.vue';
import { publicStore } from '@/stores/public';
import LineVue from '@/components/charts/Line.vue';
import { LineChartOption } from '@/echart';

// type EChartsOption = echarts.ComposeOption<
//   TitleComponentOption | ToolboxComponentOption | TooltipComponentOption | GridComponentOption | LegendComponentOption | DataZoomComponentOption | BarSeriesOption | LineSeriesOption
// >
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const { pullResultsLoop, getPullResultsEffect } = fetchS
const fetchM = useInterpret(permachine)
const loop = pullResultsLoop(pollingM)
const enhancer = ref(undefined as EnchanceResult | undefined)
const trigerRes = reactive(new Map<string, string[]>)
const cacheIdx = ref("-1")
const inputVal = ref("")
const keyList: tfkey[] = [
  "index",
  "timestamp",
  "className",
  "methodName",
  "cost",
  "object",
  "params",
  "returnObj",
  "throwExp",
  // 暂时隐藏这两个属性，不够宽了
  // "return",
  // "throw",
]
const tableResults = reactive([] as Map<string, string>[])
// const timeFragmentSet = new Set()
type tfkey = keyof TimeFragment

const chartContext: {
  categories: number[],
  data: number[],
} = reactive({
  categories: [],
  data: []
})
const chartOption = reactive<LineChartOption>({
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'cross',
      label: {
        backgroundColor: '#283b56'
      }
    }
  },
  legend: {
  },
  dataZoom: {
    type: "inside",
    minValueSpan: 30,
    maxValueSpan: 30,
    start: 50,
    end: 100,
    throttle: 0,
    zoomLock: true
  },
  toolbox: {
    show: true,
    feature: {
      dataView: { readOnly: false },
    }
  },
  xAxis: {
    type: 'category',
    boundaryGap: true,
    data: chartContext.categories
  },
  yAxis:
  {
    type: 'value',
    scale: true,
    name: 'cost(ms)',
    min: 0,
    boundaryGap: [0.2, 0.2]
  },
  series: {
    name: 'cost',
    type: 'line',
    xAxisIndex: 0,
    yAxisIndex: 0,
    data: chartContext.data
  }
});
const updateChart = (tf: TimeFragment) => {
  chartContext.data.push(tf.cost)
  chartContext.categories.push(tf.index)
}
const transform = (tf: TimeFragment) => {
  const map = new Map()
  Object.keys(tf).forEach((k) => {
    let val: string | string[] = []
    if ((k) === "params") {
      tf.params.forEach(para => {
        // 以后可能会有bug
        for (const key in para) {
          // @ts-ignore
          val.push(`${key}:${para[key].toString()}`)
        }
      })
    } else {
      val = (tf[k as tfkey].toString())
    }

    map.set(k, val)
  })
  updateChart(tf)
  return map
}
getPullResultsEffect(
  pollingM,
  result => {
    if (result.type === "tt") {
      result.timeFragmentList.forEach(tf => {
        console.log(tf.index)
        // if(!timeFragmentSet.has(tf.index)){
        //   timeFragmentSet.add(tf.index)
        tableResults.unshift(transform(tf))
        // }

      })
    }
    if (result.type == "enhancer") {
      enhancer.value = result
    }
  })
onBeforeMount(() => {
  pollingM.send("INIT")
  fetchS.asyncInit()
})
onBeforeUnmount(() => {
  loop.close()
})
const submit = async (data: { classItem: Item, methodItem: Item, count: number,express:string }) => {
  let n = data.count > 0 ? `-n ${data.count}` : ""
  let express =  data.express.trim() !== ""? `-w ${data.express}`:""
  fetchS.baseSubmit(fetchM, {
    action: "async_exec",
    command: `tt -t ${data.classItem.value} ${data.methodItem.value} ${n} ${express}`,
    sessionId: undefined
  }).then(res => {
    enhancer.value = undefined
    loop.open()
  })
}
const alltt = () => fetchS.baseSubmit(fetchM, {
  action: "exec",
  command: `tt -l`
}).then((res) => {
  let result = (res as CommonRes).body.results[0]
  trigerRes.clear()
  tableResults.length = 0
  // 因为要all 所以清空之前的记录
  chartContext.categories.length = 0
  chartContext.data.length = 0

  if (result.type === "tt") {
    result.timeFragmentList.forEach(tf => {
      tableResults.unshift(transform(tf))
    })
  }
})

const reTrigger = (idx: string) => fetchS.baseSubmit(fetchM, {
  action: "exec",
  command: `tt -i ${idx} -p`,
}).then(
  res => {
    let result = (res as CommonRes).body.results[0]

    if (result.type === "tt") {
      trigerRes.clear()
      cacheIdx.value = idx
      let tf = result.replayResult

      Object.keys(tf).forEach((k) => {
        let val: string[] = []
        if ((k as keyof TimeFragment) === "params") {
          tf.params.forEach(para => {
            val.push(JSON.stringify(para))
          })
        } else {
          val.push(tf[k as keyof TimeFragment].toString())
        }
        trigerRes.set(k as tfkey, val)
      })

      trigerRes.set("sizeLimit", [result.sizeLimit.toString()])
      trigerRes.set("replayNo", [result.replayNo.toString()])
    }
  }, () => {
    trigerRes.clear()
  }
)

const searchTt = () => {
  let condition = inputVal.value.trim() !== "" ? `'${inputVal.value}'` : ''
  return fetchS.baseSubmit(fetchM, {
    action: "exec",
    command: `tt -s ${condition}`
  }).then(res => {
    tableResults.length = 0
    trigerRes.clear()
    let result = (res as CommonRes).body.results[0]
    if (result.type === "tt") {
      if (result.timeFragmentList.length === 0) {
        publicStore().$patch({
          isErr: true,
          ErrMessage: "not found"
        })
        return
      }
      result.timeFragmentList.forEach(tf => {
        tableResults.unshift(transform(tf))
      })
    }
  }).catch(err => {
    console.error(err)
  })
}

</script>

<template>
  <MethodInput :submit-f="submit" ncount nexpress>
  </MethodInput>
  <div class="divider"></div>
  <div class="flex items-center justify-between">
    <div class="mr-2">searching records</div>
    <div
      class="flex-1 overflow-hidden rounded-lg bg-white text-left border focus-within:outline outline-2 hover:shadow-md transition">
      <input type="text" v-model="inputVal"
        class="w-full border-none py-2 pl-3 pr-10 h-full text-gray-900  focus:outline-none">
    </div>
    <button @click="searchTt" class="mx-2 btn btn-primary btn-sm btn-outline">search</button>
  </div>
  <div class="flex justify-end">
    <button class="btn btn-primary btn-sm btn-outline my-4 mr-2" @click="alltt">
      all records
    </button>
  </div>
  <div class="pointer-events-auto">
    <LineVue :option="chartOption" class="w-full h-64 mb-4"></LineVue>
    <div class="text-gray-500">
      <CmdResMenu title="invoked result" :map="trigerRes" v-if="trigerRes.size > 0">
        <template #headerAside>
          <div class="flex mt-2 justify-end mr-1">
            <button @click="reTrigger(cacheIdx)" class="btn btn-primary btn-outline btn-xs p-1">invoke</button>
          </div>
        </template>
      </CmdResMenu>
    </div>
    <Enhancer :result="enhancer" v-if="enhancer">
    </Enhancer>
    <div class="overflow-x-auto w-full">
      <table class="table table-compact w-full">
        <thead>
          <tr>
            <th></th>
            <th class="normal-case" v-for="(v, i) in keyList" :key="i">{{ v }}</th>
          </tr>
        </thead>
        <tbody class="">
          <tr v-for="(map, i) in tableResults" :key="i">
            <th class="">
              <button class="btn btn-primary btn-sm btn-outline" @click="reTrigger(map.get('index')!)">invoke</button>
            </th>
            <td class="" v-for="(key, j) in keyList" :key="j">
              <div class="flex flex-col" v-if="key == 'params'">
                <div v-for="(row, k) in map.get(key)" :key="k">
                  {{ row }}
                </div>
              </div>
              <template v-else-if="['returnObj', 'throwExp'].includes(key)">
                <pre><code>{{map.get(key)}}</code></pre>
              </template>
              <template v-else>
                {{ map.get(key) }}
              </template>
              <!-- <template v-else-if="key == 'returnObject'"></template> -->

            </td>
          </tr>
        </tbody>
        <tfoot>
          <tr>
            <th></th>
            <th class="normal-case" v-for="(v, i) in keyList" :key="i">{{ v }}</th>
          </tr>
        </tfoot>
      </table>
    </div>
  </div>

</template>

<style scoped>

</style>