<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { useInterpret, useMachine } from '@xstate/vue';
import { onBeforeMount, onBeforeUnmount, reactive, ref } from 'vue';
import permachine from '@/machines/perRequestMachine';
import Bar from '@/components/charts/Bar.vue';
import { BarChartOption, LineChartOption } from '@/echart';
import Line from '@/components/charts/Line.vue';
import dayjs from "dayjs";
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
let tz = dayjs.tz.guess();
let dashboardId = -1

const options = reactive<Map<string, LineChartOption>>(new Map())
const colors = ['#5470C6', '#91CC75'];
const gcChartContext = reactive<{ xData: string[], collectionCount: number[], collectionTime: number[] }>({
  xData: [],
  collectionCount: [],
  collectionTime: []
})
const gcoption = reactive<BarChartOption>({
  color: colors,
  title: {
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
const initOption = (title: string, have_max: boolean = true): LineChartOption => {
  let series: {
    type: string;
    name: string;
    areaStyle: any;
    data: number[];
    yAxisIndex: number;
    tooltip: any;
  }[] = [
      {
        type: 'line',
        name: "total",
        areaStyle: {},
        data: [],
        yAxisIndex: 0,
        tooltip: {
        }
      },
      {
        type: 'line',
        name: "used",
        areaStyle: {},
        data: [],
        yAxisIndex: 0,
        tooltip: {
        }
      }
    ]
  if (have_max) series.unshift({
    type: 'line',
    areaStyle: {},
    name: 'max',
    yAxisIndex: 0,
    data: [],
    tooltip: {
      formatter: `{a}:{c}M`
    }
  })

  series.unshift({
    type: 'line',
    areaStyle: undefined,
    name: 'usage',
    yAxisIndex: 1,
    data: [],
    tooltip: {
    }
  })

  const time = reactive<string[]>([])
  return {
    title: {
      text: title
    },
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
      left: "right",
      orient: "vertical"
    },
    xAxis: {
      type: 'category',
      data: time
    },
    yAxis: [
      {
        type: 'value',
        name: 'MB',
        min: 0,
        position: "left"
      },
      {
        type: 'value',
        name: 'usage(%)',
        min: 0,
        position: 'right'
      }],
    series
  } as LineChartOption
}
const transformMemory = (result: ArthasResResult) => {
  if (result.type === "dashboard") {
    let timestamp = result.runtimeInfo.timestamp
    const time = dayjs(timestamp).tz(tz).format('HH:mm:ss')
    const updateMemory = (v: { max: number; total: number; used: number; name: string; }, i: number) => {
      const have_max = v.max > 0
      const max = toMb(have_max ? v.max : v.total)
      const total = toMb(v.total)
      const used = toMb(v.used)

      let chartOption = options.get(v.name)
      if (chartOption === undefined) {
        options.set(v.name, chartOption = initOption(v.name, have_max))
      }
      (chartOption.xAxis as ({ data: string[] })).data.push(time)
      if (have_max) {
        (chartOption.series as { data: number[] }[])[1].data.push(max)
          ; (chartOption.series as { data: number[] }[])[2].data.push(total)
          ; (chartOption.series as { data: number[] }[])[3].data.push(used)
          ; (chartOption.series as { data: number[] }[])[0].data.push(Math.floor((used / max) * 10000) / 100)
      } else {
        ; (chartOption.series as { data: number[] }[])[2].data.push(used)
          ; (chartOption.series as { data: number[] }[])[1].data.push(total)
          ; (chartOption.series as { data: number[] }[])[0].data.push(Math.floor((used / max) * 10000) / 100)
      }

    }
    result.memoryInfo.heap.forEach(updateMemory)

    result.memoryInfo.nonheap.forEach(updateMemory)

    result.memoryInfo.buffer_pool.forEach(updateMemory)
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
    if (!['timestamp', 'uptime'].includes(key)) runtimeInfo.set(key as keyof RuntimeInfo, result.runtimeInfo[key as keyof RuntimeInfo].toString())
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
    <div class="card border bg-base-100">

      <div class="card-body">
        <template v-for="([title, option]) in options.entries()" :key="title">
          <Line class="h-80" :option="option"></Line>
        </template>
      </div>
    </div>
  </div>
</template>