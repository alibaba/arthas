<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { useInterpret, useMachine } from '@xstate/vue';
import { onBeforeMount, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
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

type EChartsOption = echarts.ComposeOption<
  DatasetComponentOption | PieSeriesOption
>
type GcEChartsOption = echarts.ComposeOption<
  ToolboxComponentOption | TooltipComponentOption | GridComponentOption | LegendComponentOption | BarSeriesOption | LineSeriesOption
>
const fetchS = fetchStore()
const { getPollingLoop, isResult } = fetchS
const { getCommonResEffect } = publicStore()
// const dashboadM = useMachine(machine)
const dashboadM = useInterpret(permachine)
const dashboadResM = useMachine(machine)
const loop = fetchS.pullResultsLoop(dashboadResM)
const toMb = (b: number) => Math.floor(b / 1024 / 1024)
const gcInfos = reactive(new Map<string, string[]>())
const memoryInfo = reactive(new Map<string, string[]>())
const threads = reactive(new Map<string, string[]>())
const runtimeInfo = reactive(new Map<string, string[]>())


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
getCommonResEffect(dashboadResM, body => {
  if (body.results.length > 0 && dashboardId >= 0) {
    const result = body.results.find(v => v.type === "dashboard" && v.jobId === dashboardId)
    if (result && result.type === "dashboard") {

      memoryInfo.clear()
      const heaparr: { value: number, name: string }[] = [
      ]
      result.memoryInfo.heap.filter(v => v.name !== "heap").forEach(v => {
        const arr: string[] = []

        arr.push('max : ' + toMb(v.max))
        arr.push('total : ' + toMb(v.total) )
        arr.push('used : ' + toMb(v.used) )

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

        arr.push('max : ' + toMb(v.max) )
        arr.push('total : ' + toMb(v.total) )
        arr.push('used : ' + toMb(v.used) )
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

      runtimeInfo.clear()
      Object.entries(result.runtimeInfo).forEach(([k, v]) => {
        runtimeInfo.set(k, [v.toString()])
      })


      threads.clear()
      result.threads.filter((v, i) => i < 3).forEach((v) => {
        threads.set(v.name, Object.entries(v).filter(([k, v]) => k !== "name").map(([k, v]) => `${k} : ${v}`))
      })

      gcInfos.clear()
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
  }

})
// 处理初始化请求 
onBeforeMount(async () => {
  dashboadResM.send("INIT")

  if (!fetchS.sessionId) {
    fetchS.baseSubmit(dashboadM,{
        action: "init_session"
      })
  }
  await isResult(dashboadM)

  fetchS.baseSubmit(dashboadM, {
      action: "async_exec",
      command: "dashboard",
      sessionId: undefined
    }).then(
      res=>{
        dashboardId = (res as AsyncRes).body.jobId
        loop.open()
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

  const actor = interpret(machine)
  actor.start()
  actor.send("INIT")
  actor.send({
    type: "SUBMIT",
    value: {
      action: "interrupt_job",
      sessionId:undefined
    } 
  })
  const a2 = interpret(machine)
  a2.start()
  a2.send("INIT")
  a2.send({
    type: "SUBMIT",
    value: {
      action: "close_session",
      sessionId:undefined
    }
  })
})
</script>

<template>
  <div class="p-2">

    <CmdResMenu title="threads" :map="threads" class="w-full flex justify-center" />
    <div class="flex justify-evenly">
      <div id="heapMemory" class="w-80 h-60 flex-1"></div>
      <div id="nonheapMemory" class="w-80 h-60 flex-1"></div>
      <div id="bufferPoolMemory" class="w-80 h-60 flex-1"></div>
    </div>
    <div id="gc-info" class="w-[40rem] h-80 border m-auto rounded-xl p-2"></div>
  </div>
</template>

<style scoped>
</style>