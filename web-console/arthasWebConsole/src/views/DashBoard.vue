<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { useInterpret, useMachine } from '@xstate/vue';
import { onBeforeMount, onBeforeUnmount, onMounted, reactive, ref, transformVNodeArgs } from 'vue';
import { waitFor } from 'xstate/lib/waitFor';
import { interpret } from "xstate"
import CmdResMenu from '@/components/show/CmdResMenu.vue';
import * as echarts from 'echarts/core';
import {
  TooltipComponent,
  TooltipComponentOption,
  LegendComponent,
  LegendComponentOption,
  DatasetComponentOption,
  GridComponentOption,
  ToolboxComponentOption,
  GridComponent,
  ToolboxComponent
} from 'echarts/components';
import {
  BarChart,
  BarSeriesOption,
  LineChart,
  LineSeriesOption,
  PieChart,
  PieSeriesOption
} from 'echarts/charts';
import {
  LabelLayout, UniversalTransition
} from 'echarts/features';
import {
  SVGRenderer
} from 'echarts/renderers';
import { dispose, ECharts } from 'echarts/core';
import permachine from '@/machines/perRequestMachine';
import { resolve } from 'path';

type EChartsOption = echarts.ComposeOption<
  DatasetComponentOption | PieSeriesOption
>
type GcEChartsOption = echarts.ComposeOption<
  ToolboxComponentOption | TooltipComponentOption | GridComponentOption | LegendComponentOption | BarSeriesOption | LineSeriesOption
>
const fetchS = fetchStore()
const { getCommonResEffect } = publicStore()
const dashboadM = useInterpret(permachine)
const dashboadResM = useMachine(machine)
const loop = fetchS.pullResultsLoop(dashboadResM)
const toMb = (b: number) => Math.floor(b / 1024 / 1024)
const gcInfos = reactive(new Map<string, string[]>())
const memoryInfo = reactive(new Map<string, string[]>())
const threads = reactive(new Map<string, string[]>())
const runtimeInfo = reactive(new Map<keyof RuntimeInfo, string>())
const pri = ref(3)
const publiC = publicStore()
const tableResults = reactive([] as Map<string, string>[])
const keyList: (keyof ThreadStats)[] = [
  "id",
  "name",
  "cpu",
  "daemon",
  "deltaTime",
  "group",
  "interrupted",
  "priority",
  "state",
  "time",
]

let dashboardId = -1
let heapChart: ECharts
let nonheapChart: ECharts
let bufferPoolChart: ECharts
let gcChart: ECharts
const clearChart = (...charts: ECharts[]) => {
  charts.forEach(chart => {
    if (chart !== null && chart !== undefined) chart.dispose()
  })
}
const transformMemory = (result: ArthasResResult) => {
  if (result.type === "dashboard") {

    const heaparr: { value: number, name: string }[] = [
    ]
    result.memoryInfo.heap.filter(v => v.name !== "heap").forEach(v => {
      const arr: string[] = []

      arr.push('max : ' + toMb(v.max))
      arr.push('total : ' + toMb(v.total))
      arr.push('used : ' + toMb(v.used))

      const usage: number = (v.max > 0 ? (v.used / v.max) : (v.used / v.total)) * 100
      heaparr.push({ value: toMb(v.used), name: `${v.name}(${usage.toFixed(2)}%)` })

      arr.push(usage + '%')

      memoryInfo.set(v.name, arr)
    })
    heaparr.push({
      value: Math.floor((result.memoryInfo.heap[0].max > 0 ? (result.memoryInfo.heap[0].max - result.memoryInfo.heap[0].used) : (result.memoryInfo.heap[0].total - result.memoryInfo.heap[0].used)) / 1024 / 1024),
      name: "free",
    })
    heapChart && heapChart.setOption({
      series: {
        data: heaparr
      }
    } as EChartsOption)

    const nonheaparr: {
      value: number, name: string,
    }[] = []
    result.memoryInfo.nonheap.filter(v => v.name !== "nonheap").forEach(v => {
      const arr: string[] = []

      arr.push('max : ' + toMb(v.max))
      arr.push('total : ' + toMb(v.total))
      arr.push('used : ' + toMb(v.used))
      const usage: number = (v.used / v.total) * 100
      nonheaparr.push({ value: toMb(v.used), name: `${v.name}(${usage.toFixed(2)}%)` })

      arr.push(usage * 100 + '%')

      memoryInfo.set(v.name, arr)
    })
    nonheapChart && nonheapChart.setOption({ series: { data: nonheaparr } } as EChartsOption)

    const bufferPoolarr: {
      value: number, name: string,
    }[] = []
    result.memoryInfo.buffer_pool.filter(v => v.name !== "buffer_pool;").forEach(v => {
      bufferPoolarr.push({ value: toMb(v.used), name: `${v.name}` })

    })
    bufferPoolChart && bufferPoolChart.setOption({ series: { data: bufferPoolarr } } as EChartsOption)
  }
}
const transformThread = (result: ArthasResResult) => {
  if (result.type !== "dashboard") return;
  result.threads.filter((v, i) => i < pri.value).forEach(thread => {
    // threads.set(v.name, Object.entries(v).filter(([k, v]) => k !== "name").map(([k, v]) => `${k} : ${v}`))
    const map = new Map()
    Object.entries(thread).map(([k, v]) => map.set(k, v.toString() || "-"))
    tableResults.unshift(map)
  })
}
const transformGc = (result: ArthasResResult) => {
  if (result.type !== "dashboard") return;
  const gcCountData: number[] = []

  const gcTimeData: number[] = []
  const gcxdata: string[] = []
  result.gcInfos.forEach(v => {
    // gcInfos.set(v.name, [v.collectionCount.toString(), v.collectionTime.toString()])
    gcxdata.push(v.name)
    gcCountData.push(v.collectionCount)
    gcTimeData.push(v.collectionTime)
  })
  gcChart.setOption({
    xAxis: {
      type: 'category',
      axisTick: {
        alignWithLabel: true
      },
      // prettier-ignore
      data: gcxdata
    }, series: [{
      name: "collectionCount",
      type: 'bar',
      data: gcCountData
    }, {
      name: "collectionTime",
      type: 'bar',
      data: gcTimeData
    }]
  } as GcEChartsOption)
}
const setPri = publiC.inputDialogFactory(
  pri,
  (raw) => {
    let valRaw = parseInt(raw)
    return Number.isNaN(valRaw) ? 3 : valRaw
  },
  (input) => input.value.toString(),
)
const transformRuntimeInfo = (result: ArthasResResult) => {
  if (result.type !== "dashboard") return;
  for (const key in result.runtimeInfo as RuntimeInfo) {
    runtimeInfo.set(key as keyof RuntimeInfo, result.runtimeInfo[key as keyof RuntimeInfo].toString())
  }
}
getCommonResEffect(dashboadResM, body => {
  if (body.results.length > 0 && dashboardId >= 0) {
    const result = body.results.find(v => v.type === "dashboard" && v.jobId === dashboardId)
    if (result && result.type === "dashboard") {

      memoryInfo.clear()
      transformMemory(result)

      runtimeInfo.clear()
      transformRuntimeInfo(result)

      threads.clear()
      tableResults.length = 0
      transformThread(result)

      gcInfos.clear()
      transformGc(result)
    }
  }

})
// 处理初始化请求 
onBeforeMount(async () => {
  dashboadResM.send("INIT")

  fetchS
    .asyncInit()
    .finally(
      () => {
        fetchS.baseSubmit(dashboadM, {
          action: "async_exec",
          command: "dashboard",
          sessionId: undefined
        }).then(
          res => {
            dashboardId = (res as AsyncRes).body.jobId
            loop.open()
          }
        )
      }
    )

})
// 处理dom
onMounted(() => {
  // init

  const clearDom = (...doms: HTMLElement[]) => {
    doms.forEach(dom => {
      dispose(dom)
    })
  }
  clearChart(nonheapChart, heapChart, bufferPoolChart, gcChart)
  const heapDom = document.getElementById('heapMemory')!
  const nonheapDom = document.getElementById('nonheapMemory')!
  const bufferPoolDom = document.getElementById('bufferPoolMemory')!
  const gcDom = document.getElementById('gc-info')!
  clearDom(heapDom, nonheapDom, bufferPoolDom, gcDom)
  echarts.use(
    [TooltipComponent, LegendComponent, PieChart, SVGRenderer, LabelLayout, ToolboxComponent, GridComponent, BarChart, LineChart, UniversalTransition]
  );



  const heapoption: EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}:{c}M {d}'
    },
    legend: {
      top: '5%',
      left: 'center'
    },
    series: [
      {
        name: 'heap memory',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: true,
        label: {
          show: false,
          position: 'center',
        },
        labelLine: {
          show: false
        },
        data: [
        ]
      }
    ]
  };
  const nonheapoption: EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}:{c}M'
    },
    legend: {
      top: '5%',
      left: 'center'
    },
    series: [
      {
        name: 'nonheap memory',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        label: {
          show: false,
          position: 'center',
          formatter: '{b}:{c}M'
        },
        labelLine: {
          show: false
        },
        data: [
        ]
      }
    ]
  };
  const bufferPooloption: EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{c}M'
    },
    legend: {
      top: '5%',
      left: 'center'
    },
    series: [
      {
        name: 'buffer_pool memory',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: true,
        label: {
          show: false,
          position: 'outside',
        },
        labelLine: {
          show: false
        },
        data: [
          {
            value: 0,
            name: '',
          }
        ]
      }
    ]
  };
  const colors = ['#5470C6', '#91CC75'];
  const gcoption: GcEChartsOption = {
    color: colors,
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    grid: {
      right: '20%'
    },
    legend: {
      data: ['collectionCount', 'collectionTime']
    },
    xAxis: [
      {
        type: 'category',
        axisTick: {
          alignWithLabel: true
        },
        // prettier-ignore
        data: []
      }
    ],
    toolbox: {
      feature: {
        dataView: { show: true, readOnly: true },
      }
    },
    yAxis: [
      {
        type: 'value',
        name: 'collectionCount',
        position: 'left',
        alignTicks: true,
        axisLine: {
          show: true,
          lineStyle: {
            color: colors[0]
          }
        },
        axisLabel: {
          formatter: '{value}'
        }
      },
      {
        type: 'value',
        name: 'collectionTime',
        position: 'right',
        alignTicks: true,
        axisLine: {
          show: true,
          lineStyle: {
            color: colors[1]
          }
        },
        axisLabel: {
          formatter: '{value} ms'
        }
      },
    ],
    series: [
      {
        name: 'collectionCount',
        type: 'bar',
        data: [
        ]
      },
      {
        name: 'collectionTime',
        type: 'bar',
        yAxisIndex: 1,
        data: [
        ]
      },
    ]
  };


  heapChart = echarts.init(heapDom);
  heapoption && heapChart.setOption(heapoption);

  nonheapChart = echarts.init(nonheapDom);
  nonheapoption && nonheapChart.setOption(nonheapoption);

  bufferPoolChart = echarts.init(bufferPoolDom);
  bufferPooloption && bufferPoolChart.setOption(bufferPooloption);

  // gcInfosChart

  gcChart = echarts.init(gcDom);


  gcoption && gcChart.setOption(gcoption);
})
onBeforeUnmount(async () => {
  loop.close()
  clearChart(nonheapChart, heapChart, bufferPoolChart, gcChart)

  fetchS
    .interruptJob()

})
</script>

<template>
  <div class="p-2 pointer-events-auto flex flex-col h-full">
    <div class="input-btn-style mb-4 h-32 flex flex-wrap flex-col items-start">
      <div v-for="(cv, ci) in runtimeInfo" :key="ci" class="flex mb-1 w-1/3 pr-2">
        <span class="bg-blue-500 w-44 px-2 rounded-l">
          {{ cv[0] }}
        </span>
        <span class="border-gray-300 bg-blue-100 rounded-r flex-1 pl-2 border bordergre">
          {{cv[1]}}
        </span>
      </div>
    </div>
    <!-- <CmdResMenu title="threads" :map="threads" class="w-full flex justify-center" /> -->
    <div class="flex justify-evenly mb-4 flex-1 h-80">
      <div id="heapMemory" class="w-80 h-80 flex-1 input-btn-style mr-4"></div>
      <div id="nonheapMemory" class="w-80 h-80 flex-1 input-btn-style mr-4"></div>
      <div id="bufferPoolMemory" class="w-80 h-80 flex-1 input-btn-style"></div>
    </div>
    <div class="w-full flex justify-start items-start flex-1">
      <div id="gc-info" class="w-[40rem] h-80 input-btn-style p-2 mr-2"></div>
      <div class="input-btn-style overflow-auto flex-1 h-80">
        <div class="flex justify-end mb-2">
          
        <button class="input-btn-style" @click="setPri">limit:{{pri}}</button>
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
</template>

<style scoped>

</style>