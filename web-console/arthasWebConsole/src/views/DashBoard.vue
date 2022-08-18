<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { useMachine } from '@xstate/vue';
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
import { ECharts, number } from 'echarts/core';

// type EChartsOption = echarts.ComposeOption<
//   TooltipComponentOption | LegendComponentOption | PieSeriesOption
// >
type EChartsOption = echarts.ComposeOption<
  DatasetComponentOption | PieSeriesOption
>
type GcEChartsOption = echarts.ComposeOption<
  ToolboxComponentOption | TooltipComponentOption | GridComponentOption | LegendComponentOption | BarSeriesOption | LineSeriesOption
>
const fetchS = fetchStore()
const { getPollingLoop } = fetchS
const { getCommonResEffect } = publicStore()
const dashboadM = useMachine(machine)
const dashboadResM = useMachine(machine)
// const interruptM = useMachine(machine)
const loop = getPollingLoop(() => {
  console.log(dashboadResM.state.value.context)
  dashboadResM.send({
    type: "SUBMIT",
    value: {
      action: "pull_results",
    } as AsyncReq
  })
},5000)
const isReady = () => waitFor(dashboadM.service, (state) => state.matches('ready'))
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

getCommonResEffect(dashboadResM, body => {
  if (body.results.length > 0 && dashboardId >= 0) {
    const result = body.results.find(v => v.type === "dashboard" && v.jobId === dashboardId)
    if (result && result.type === "dashboard") {

      memoryInfo.clear()
      const heaparr: { value: number, name: string }[] = [
      ]
      result.memoryInfo.heap.filter(v => v.name !== "heap").forEach(v => {
        const arr: string[] = []

        arr.push('max : ' + toMb(v.max) + 'M')
        arr.push('total : ' + toMb(v.total) + 'M')
        arr.push('used : ' + toMb(v.used) + 'M')

        const usage: number = v.used / v.max * 100
        heaparr.push({ value: toMb(v.used), name: `${v.name}: ${toMb(v.used) + 'M'}(${usage.toFixed(2)}%)` })

        arr.push(usage + '%')

        memoryInfo.set(v.name, arr)
      })
      heaparr.push({
        value: Math.floor((result.memoryInfo.heap[0].max - result.memoryInfo.heap[0].used) / 1024 / 1024),
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

        arr.push('max : ' + toMb(v.max) + 'M')
        arr.push('total : ' + toMb(v.total) + 'M')
        arr.push('used : ' + toMb(v.used) + 'M')
        const usage: number = (v.used / v.total) * 100
        nonheaparr.push({ value: toMb(v.used), name: `${v.name}: ${toMb(v.used) + 'M'}(${usage.toFixed(2)}%)` })

        arr.push(usage * 100 + '%')

        memoryInfo.set(v.name, arr)
      })
      nonheapChart && nonheapChart.setOption({ series: { data: nonheaparr } } as EChartsOption)

      const bufferPoolarr: {
        value: number, name: string,
      }[] = []
      result.memoryInfo.buffer_pool.filter(v => v.name !== "buffer_pool;").forEach(v => {
        bufferPoolarr.push({ value: toMb(v.used), name: `${v.name}: ${toMb(v.used)}M` })

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
onBeforeMount(async () => {
  dashboadM.send("INIT")
  dashboadResM.send("INIT")

  if (!fetchS.sessionId) {

    dashboadM.send({
      type: "SUBMIT",
      value: {
        action: "init_session"
      }
    })
  }

  await isReady()

  dashboadM.send({
    type: "SUBMIT",
    value: {
      action: "async_exec",
      command: "dashboard"
    } as AsyncReq
  })

  await isReady()

  dashboardId = (dashboadM.state.value.context.response as AsyncRes).body.jobId

  loop.open()
})
onMounted(() => {
  echarts.use(
    [TooltipComponent, LegendComponent, PieChart, SVGRenderer, LabelLayout, ToolboxComponent, GridComponent, BarChart, LineChart, UniversalTransition]
  );

  const chartDom = document.getElementById('heapMemory')!;
  heapChart = echarts.init(chartDom);
  const heapoption: EChartsOption = {
    tooltip: {
      trigger: 'item'
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
          position: 'center'
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
      trigger: 'item'
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
          position: 'center'
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
      trigger: 'item'
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
          position: 'center'
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
  // memoryChart
  heapoption && heapChart.setOption(heapoption);

  const nchartDom = document.getElementById('nonheapMemory')!;
  nonheapChart = echarts.init(nchartDom);

  nonheapoption && nonheapChart.setOption(nonheapoption);

  bufferPoolChart = echarts.init(document.getElementById('bufferPoolMemory')!);

  bufferPooloption && bufferPoolChart.setOption(bufferPooloption);
  // gcInfosChart
  const gcchartDom = document.getElementById('gc-info')!;
  gcChart = echarts.init(gcchartDom);
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

  gcoption && gcChart.setOption(gcoption);
})
onBeforeUnmount(() => {
  loop.close()

  const actor = interpret(machine)
  actor.start()
  actor.send("INIT")
  actor.send({
    type: "SUBMIT",
    value: {
      action: "interrupt_job",
    } as AsyncReq
  })
})
</script>

<template>
  <div class="p-2">
    <!-- <CmdResMenu title="runtimeInfo" :map="runtimeInfo" class="w-full" open /> -->
    <!-- <CmdResMenu title="memory" :map="memoryInfo" class="w-full" /> -->
    <!-- <CmdResMenu :map="gcInfos" title="gcInfos"></CmdResMenu> -->

    <CmdResMenu title="threads" :map="threads" class="w-full flex justify-center" />
    <div class="flex justify-evenly">
      <div id="heapMemory" class="w-80 h-60 flex-1"></div>
      <div id="nonheapMemory" class="w-80 h-60 flex-1"></div>
      <div id="bufferPoolMemory" class="w-80 h-60 flex-1"></div>
    </div>
    <div id="gc-info" class="w-10/12 h-80 border-2 m-auto"></div>
  </div>
</template>

<style scoped>
</style>