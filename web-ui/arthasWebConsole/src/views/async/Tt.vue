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
import * as echarts from 'echarts/core';
import {
  TitleComponent,
  TitleComponentOption,
  ToolboxComponent,
  ToolboxComponentOption,
  TooltipComponent,
  TooltipComponentOption,
  GridComponent,
  GridComponentOption,
  LegendComponent,
  LegendComponentOption,
  DataZoomComponent,
  DataZoomComponentOption
} from 'echarts/components';
import {
  BarChart,
  BarSeriesOption,
  LineChart,
  LineSeriesOption
} from 'echarts/charts';
import {
  UniversalTransition
} from 'echarts/features';
import {
  SVGRenderer
} from 'echarts/renderers';
import { ECharts, number } from 'echarts/core';

echarts.use(
  [TitleComponent, ToolboxComponent, TooltipComponent, GridComponent, LegendComponent, DataZoomComponent, BarChart, LineChart, SVGRenderer, UniversalTransition]
);

type EChartsOption = echarts.ComposeOption<
  TitleComponentOption | ToolboxComponentOption | TooltipComponentOption | GridComponentOption | LegendComponentOption | DataZoomComponentOption | BarSeriesOption | LineSeriesOption
>
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
  count: number,
  myChart?: ECharts,
  categories: number[],
  data: number[],
  cur: number,
  max: number,
} = {
  max: 0,
  cur: 0,
  count: 20,
  myChart: undefined,
  categories: [],
  data: []
}
const chartOption = {
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'cross',
      label: {
        backgroundColor: '#283b56'
      }
    }
  },
  legend: {},
  toolbox: {
    show: true,
    feature: {
      dataView: { readOnly: false },
    }
  },
  xAxis: [
    {
      type: 'category',
      boundaryGap: true,
      data: chartContext.categories
    }
  ],
  yAxis: [
    {
      type: 'value',
      scale: true,
      name: 'cost(ms)',
      max: 0,
      min: 0,
      boundaryGap: [0.2, 0.2]
    }
  ],
  series: [
    {
      name: 'cost',
      type: 'bar',
      xAxisIndex: 0,
      yAxisIndex: 0,
      data: chartContext.data
    }
  ]
};
const updateChart = (tf: TimeFragment) => {
  while (chartContext.cur > chartContext.count) {
    chartContext.data.shift()
    chartContext.categories.shift()
    chartContext.cur--
  }
  chartContext.data.push(tf.cost)
  chartContext.categories.push(tf.index)
  chartContext.cur++
  chartContext.max = Math.max(...chartContext.data)
  chartContext.myChart!.setOption<EChartsOption>({
    xAxis: [
      {
        data: chartContext.categories
      }
    ],
    yAxis: [
      {
        max: chartContext.max,
      }
    ],
    series: [
      {
        data: chartContext.data
      }
    ]
  });
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
  })
onBeforeMount(() => {
  pollingM.send("INIT")
  fetchS.asyncInit()
})
onMounted(() => {
  const chartDom = document.getElementById('ttchart')!;
  chartContext.myChart = echarts.init(chartDom);
  chartContext.myChart.on("click", e => {
    console.dir(e)
  })
  chartOption && chartContext.myChart.setOption(chartOption)
})
onBeforeUnmount(() => {
  loop.close()
})
const submit = async (data: { classItem: Item, methodItem: Item, count: number, conditon: string }) => {
  let n = data.count > 0 ? `-n ${data.count}` : ""
  let condition = data.conditon
  fetchS.baseSubmit(fetchM, {
    action: "async_exec",
    command: `tt -t ${data.classItem.value} ${data.methodItem.value} ${n} ${condition}`,
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
  }).catch(err=>{
    console.error(err)
  })
}

</script>

<template>
  <MethodInput :submit-f="submit" ncount ncondition>
  </MethodInput>
  <div class="flex items-center border-t-2 pt-4 mt-4 justify-between">
    <div class="mr-2">searching records</div>
    <div
      class="flex-1 overflow-hidden rounded-lg bg-white text-left border focus-within:outline outline-2 hover:shadow-md transition">
      <input type="text" v-model="inputVal"
        class="w-full border-none py-2 pl-3 pr-10 h-full text-gray-900  focus:outline-none">
    </div>
    <button @click="searchTt" class="mx-2 button-style">search</button>
  </div>
  <div class="flex justify-end">
    <button class="button-style my-4" @click="alltt">
      all records
    </button>
  </div>
  <div class="pointer-events-auto">
    <div id="ttchart" class="w-full h-60 input-btn-style mb-4"></div>
    <div class="text-gray-500">
      <CmdResMenu title="invoked result" :map="trigerRes" v-if="trigerRes.size > 0">
        <template #headerAside>
          <div class="flex mt-2 justify-end mr-1">
            <button @click="reTrigger(cacheIdx)" class="button-style p-1">invoke</button>
          </div>
        </template>
      </CmdResMenu>
    </div>
    <template v-if="enhancer|| tableResults.length > 0">
      <Enhancer :result="enhancer" v-if="enhancer"></Enhancer>
      <div class="w-full flex justify-center items-center ">
        <table class="border-collapse border border-slate-400 table-fixed">
          <thead>
            <tr>
              <th class="border border-slate-300 p-1" v-for="(v,i) in keyList" :key="i">{{v}}</th>
              <th class="border border-slate-300 p-1">invoke</th>
            </tr>
          </thead>
          <tbody class="">
            <tr v-for="(map, i) in tableResults" :key="i">
              <td class="border border-slate-300 p-1" v-for="(key,j) in keyList" :key="j">
                <template v-if=" key !== 'params'">
                  {{map.get(key)}}
                </template>

                <div class="flex flex-col" v-else>
                  <div v-for="(row, k) in map.get(key)" :key="k">
                    {{row}}
                  </div>
                </div>
              </td>
              <td class="border border-slate-300 ">
                <button class="button-style" @click="reTrigger(map.get('index')!)">invoke</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>

</template>

<style scoped>

</style>