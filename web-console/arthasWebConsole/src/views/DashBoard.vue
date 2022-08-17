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
  DatasetComponentOption
} from 'echarts/components';
import {
  PieChart,
  PieSeriesOption
} from 'echarts/charts';
import {
  LabelLayout
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
const fetchS = fetchStore()
const { getPollingLoop } = fetchS
const { getCommonResEffect } = publicStore()
const dashboadM = useMachine(machine)
const dashboadResM = useMachine(machine)
// const interruptM = useMachine(machine)
const res = ref('')
const loop = getPollingLoop(() => {
  console.log(dashboadResM.state.value.context)
  dashboadResM.send({
    type: "SUBMIT",
    value: {
      action: "pull_results",
    } as AsyncReq
  })
})
const isReady = () => waitFor(dashboadM.service, (state) => state.matches('ready'))
const toMb = (b: number) => Math.floor(b / 1024 / 1024)
const gcInfos = reactive(new Map<string, string[]>())
const memoryInfo = reactive(new Map<string, string[]>())
const threads = reactive(new Map<string, string[]>())
const runtimeInfo = reactive(new Map<string, string[]>())


let dashboardId = -1
let heapChart: ECharts
let nonheapChart: ECharts

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
        const usage: number = (v.max > 0 ? (v.used / v.max) : (v.used / v.total)) * 100
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
        const usage: number = (v.max > 0 ? (v.used / v.max) : (v.used / v.total)) * 100
        nonheaparr.push({ value: toMb(v.used), name: `${v.name}: ${toMb(v.used) + 'M'}(${usage.toFixed(2)}%)` })

        arr.push(usage * 100 + '%')

        memoryInfo.set(v.name, arr)
      })
      nonheapChart && nonheapChart.setOption({ series: { data: nonheaparr } } as EChartsOption)


      runtimeInfo.clear()
      Object.entries(result.runtimeInfo).forEach(([k, v]) => {
        runtimeInfo.set(k, [v.toString()])
      })


      threads.clear()
      result.threads.filter((v,i)=>i <3).forEach((v) => {
        threads.set(v.name, Object.entries(v).filter(([k, v]) => k !== "name").map(([k, v]) => `${k} : ${v}`))
      })
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
      command: "dashboard -i 1000"
    } as AsyncReq
  })

  await isReady()

  dashboardId = (dashboadM.state.value.context.response as AsyncRes).body.jobId

  loop.open()
})
onMounted(() => {
  echarts.use(
    [TooltipComponent, LegendComponent, PieChart, SVGRenderer, LabelLayout]
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
          {
            value: 0,
            name: '',
          }
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

  heapoption && heapChart.setOption(heapoption);

  const nchartDom = document.getElementById('nonheapMemory')!;
  nonheapChart = echarts.init(nchartDom);

  nonheapoption && nonheapChart.setOption(nonheapoption);

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
    <CmdResMenu title="runtimeInfo" :map="runtimeInfo" class="w-full" open/>
    <!-- <CmdResMenu title="memory" :map="memoryInfo" class="w-full" /> -->
    <CmdResMenu title="threads" :map="threads" class="w-full" />
    <div class="flex justify-evenly">
      <div id="heapMemory" class="w-80 h-60 flex-1"></div>
      <div id="nonheapMemory" class="w-80 h-60 flex-1"></div>
    </div>
  </div>
</template>

<style scoped>
</style>