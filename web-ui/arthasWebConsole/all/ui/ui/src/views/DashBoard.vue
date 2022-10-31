<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { useInterpret, useMachine } from '@xstate/vue';
import { onBeforeMount, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
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
const transformThread = (result: ArthasResResult, end: number) => {
  if (result.type !== "dashboard") return;

  for (let i = 0; i < end && i < result.threads.length; i++) {
    const thread = result.threads[i]
    const map = new Map()
    Object.entries(thread).forEach(([k, v]) => map.set(k, v.toString().trim() || "-"))
    tableResults.unshift(map)
  }
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
const { increase, decrease } = publiC.numberCondition(pri, { min: 1 })
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
      transformThread(result, pri.value)

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
})
</script>

<template>
  <div class="p-2 pointer-events-auto flex flex-col h-full">
    <div class="input-btn-style mb-4 h-32 flex flex-wrap flex-col items-start overflow-auto min-h-[6rem]">
      <div v-for="(cv, ci) in runtimeInfo" :key="ci" class="flex mb-1 pr-2">
        <span class="bg-primary-focus text-primary-content border border-primary-focus w-44 px-2 rounded-l">
          {{ cv[0] }}
        </span>
        <span class="bg-base-200 border border-primary-focus rounded-r px-2 flex-1">
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
      <div id="gc-info" class="w-[40rem] h-80 input-btn-style p-2 mr-4"></div>
      <div class="input-btn-style flex-1 h-80 overflow-auto w-0">
        <div class="flex justify-end mb-2">
          <div class="btn-group">
            <button class="btn btn-sm btn-outline" @click="decrease">-</button>
            <button class="btn btn-sm btn-outline border-x-0" @click="setPri">limit:{{pri}}</button>
            <button class="btn btn-sm btn-outline" @click="increase">+</button>
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="table table-compact w-full">
            <thead>
              <tr>
                <th></th>
                <th v-for="(v,i) in keyList" :key="i" class="normal-case">{{v}}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(map, i) in tableResults" :key="i" class="hover">
                <th>{{i + 1}}</th>
                <td v-for="(key,j) in keyList" :key="j">
                  {{map.get(key)}}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>