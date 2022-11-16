<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { useInterpret, useMachine } from '@xstate/vue';
import { onBeforeMount, onBeforeUnmount, reactive, ref } from 'vue';
import permachine from '@/machines/perRequestMachine';
import Bar from '@/components/charts/Bar.vue';
import { BarChartOption, CircleChartOption } from '@/echart';
import Circle from '@/components/charts/Circle.vue';
const fetchS = fetchStore()
const { getCommonResEffect } = publicStore()
const dashboadM = useInterpret(permachine)
const dashboadResM = useMachine(machine)
const loop = fetchS.pullResultsLoop(dashboadResM)
const toMb = (b: number) => Math.floor(b / 1024 / 1024)
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

const colors = ['#5470C6', '#91CC75'];
const gcChartContext = reactive<{ xData: string[], collectionCount: number[], collectionTime: number[] }>({
  xData: [],
  collectionCount: [],
  collectionTime: []
})
const gcoption = reactive<BarChartOption>({
  color: colors,
  title:{
    text: "GC",
  },
  grid: {
    right: '20%'
  },
  legend: {
    data: ['collectionCount', 'collectionTime']
  },
  xAxis: [{
    type: 'category',
    axisTick: {
      alignWithLabel: true
    },
    data: gcChartContext.xData
  }],
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
      data: gcChartContext.collectionCount
    },
    {
      name: 'collectionTime',
      type: 'bar',
      yAxisIndex: 1,
      data: gcChartContext.collectionTime
    },
  ]
});
const bufferPoolContext = reactive<{
  data: {
    value: number, name: string,
  }[]
}>({
  data: []
})
const heapoptionContext = reactive<{
  data: {
    value: number, name: string,
  }[]
}>({
  data: []
})
const nonheapContext = reactive<{
  data: {
    value: number, name: string,
  }[]
}>({
  data: []
})
const heapoption = reactive<CircleChartOption>({
  title:{
    text:"heap"
  },
  tooltip: {
    trigger: 'item',
    formatter: '{b}:{c}M {d}'
  },
  legend: {
    top: 'center',
    left: 'right',
    orient: "vertical"
  },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      center:['35%','50%'],
      avoidLabelOverlap: true,
      label: {
        show: false,
        position: 'center',
      },
      labelLine: {
        show: false
      },
      data: heapoptionContext.data
    }
  ]
});
const nonheapoption = reactive<CircleChartOption>({
  title:{
    text:"nonheap"
  },
  tooltip: {
    trigger: 'item',
    formatter: '{b}:{c}M'
  },
  legend: {
    top: 'center',
    left: 'right',
    orient: "vertical"
  },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      center:['35%','50%'],
      avoidLabelOverlap: false,
      label: {
        show: false,
        position: 'center',
        formatter: '{b}:{c}M'
      },
      labelLine: {
        show: false
      },
      data: nonheapContext.data
    }
  ]
});
const bufferPooloption = reactive<CircleChartOption>({
  title: {
    text: "buffer_pool"
  },
  tooltip: {
    trigger: 'item',
    formatter: '{c}M'
  },
  legend: {
    top: 'center',
    left: 'right',
    orient: 'vertical'
  },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      center:['35%','50%'],
      avoidLabelOverlap: true,
      label: {
        show: false,
        position: 'outside',
      },
      labelLine: {
        show: false
      },
      data: bufferPoolContext.data
    }
  ]
});
const transformMemory = (result: ArthasResResult) => {
  if (result.type === "dashboard") {

    // const heaparr: { value: number, name: string }[] = [
    // ]
    heapoptionContext.data.length = 0
    result.memoryInfo.heap.filter(v => v.name !== "heap").forEach(v => {
      const arr: string[] = []

      // arr.push('max : ' + toMb(v.max))
      // arr.push('total : ' + toMb(v.total))
      // arr.push('used : ' + toMb(v.used))

      const usage: number = (v.max > 0 ? (v.used / v.max) : (v.used / v.total)) * 100
      heapoptionContext.data.push({ value: toMb(v.used), name: `${v.name}(${usage.toFixed(2)}%)` })

      // arr.push(usage + '%')

      // memoryInfo.set(v.name, arr)
    })
    heapoptionContext.data.push({
      value: Math.floor((result.memoryInfo.heap[0].max > 0 ? (result.memoryInfo.heap[0].max - result.memoryInfo.heap[0].used) : (result.memoryInfo.heap[0].total - result.memoryInfo.heap[0].used)) / 1024 / 1024),
      name: "free",
    })
    // heapoptionContext.data = heaparr

    nonheapContext.data.length = 0
    result.memoryInfo.nonheap.filter(v => v.name !== "nonheap").forEach(v => {
      const arr: string[] = []

      // arr.push('max : ' + toMb(v.max))
      // arr.push('total : ' + toMb(v.total))
      // arr.push('used : ' + toMb(v.used))
      const usage: number = (v.used / v.total) * 100
      nonheapContext.data.push({ value: toMb(v.used), name: `${v.name}(${usage.toFixed(2)}%)` })

      // arr.push(usage * 100 + '%')

      // memoryInfo.set(v.name, arr)
    })
    // nonheapChart && nonheapChart.setOption({ series: { data: nonheaparr } } as EChartsOption)

    bufferPoolContext.data.length = 0
    result.memoryInfo.buffer_pool.filter(v => v.name !== "buffer_pool;").forEach(v => {
      bufferPoolContext.data.push({ value: toMb(v.used), name: `${v.name}` })
    })
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
  gcChartContext.xData.length = 0
  gcChartContext.collectionCount.length = 0
  gcChartContext.collectionTime.length = 0
  result.gcInfos.forEach(v => {
    gcChartContext.xData.push(v.name)
    gcChartContext.collectionCount.push(v.collectionCount)
    gcChartContext.collectionTime.push(v.collectionTime)
  })
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

      transformMemory(result)

      runtimeInfo.clear()
      transformRuntimeInfo(result)

      tableResults.length = 0
      transformThread(result, pri.value)

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

onBeforeUnmount(async () => {
  loop.close()
})
</script>

<template>
  <div class="p-2 pointer-events-auto h-full overflow-auto">
    <div class="card bg-base-100 border mb-4 compact h-auto">
      <div class="card-body flex-wrap flex-row">
        <span v-for="(cv, ci) in runtimeInfo" :key="ci" class="badge badge-outline badge-primary">

          {{ cv[0] }}:
          {{ cv[1] }}
        </span>
      </div>
    </div>
    <div class="flex justify-evenly mb-4 flex-1 h-80">
      <div class="card border mr-4 flex-1 bg-base-100">

        <Circle class="card-body" :option="heapoption"></Circle>
      </div>
      <div class="card border mr-4 flex-1 bg-base-100">

        <Circle class="card-body" :option="nonheapoption"></Circle>
      </div>
      <div class="card border flex-1 bg-base-100">
        <Circle class="card-body" :option="bufferPooloption"></Circle>
      </div>
    </div>

    <div class="w-full flex justify-start items-start flex-1">
      <div class="card bg-base-100 border mr-4 h-80 w-1/3">
        <Bar class="card-body" :option="gcoption" />
      </div>
      <div class="card flex-1 h-80 overflow-auto w-0 border bg-base-100">
        <div class="card-body">
          <div class="flex justify-end mb-2">
            <div class="btn-group">
              <button class="btn btn-sm btn-outline" @click="decrease">-</button>
              <button class="btn btn-sm btn-outline border-x-0" @click="setPri">limit:{{ pri }}</button>
              <button class="btn btn-sm btn-outline" @click="increase">+</button>
            </div>
          </div>
          <div class="overflow-x-auto">
            <table class="table table-compact w-full">
              <thead>
                <tr>
                  <th></th>
                  <th v-for="(v, i) in keyList" :key="i" class="normal-case">{{ v }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(map, i) in tableResults" :key="i" class="hover">
                  <th>{{ i + 1 }}</th>
                  <td v-for="(key, j) in keyList" :key="j">
                    {{ map.get(key) }}
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