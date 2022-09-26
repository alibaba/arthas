<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { useInterpret, useMachine } from '@xstate/vue';
import {
  ListboxOption,
  Listbox,
  ListboxButton,
  ListboxOptions
} from "@headlessui/vue"
import MethodInput from '@/components/input/MethodInput.vue';
import machine from '@/machines/consoleMachine';
import { fetchStore } from '@/stores/fetch';
import { onBeforeMount, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
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
const { pullResultsLoop, getCommonResEffect } = fetchS
const fetchM = useInterpret(permachine)
const loop = pullResultsLoop(pollingM)
const enhancer = ref(undefined as undefined | EnchanceResult)
const cycleV = ref(120)
const publicS = publicStore()
type KMD = | keyof MonitorData
// const keyList: string[] = [
//   "className",
//   "methodName",
//   "cost",
//   "success",
//   "failed",
//   "fail-rate",
//   "total",
// ]
const modelist: { name: string, value: string }[] = [
  { name: "调用开始之前", value: "-b" },
  { name: "调用结束之后", value: "" }
]
const mode = ref(modelist[1])

// const tableResults = reactive([] as Map<string, string[] | string>[])

const chartContext: {
  count: number,
  myChart?: ECharts,
  costChart?: ECharts,
  categories: number[],
  data: number[],
  cur: number,
  max: number,
  successData: number[],
  failureData: number[]
} = {
  max: 0,
  cur: 0,
  count: 40,
  myChart: undefined,
  costChart: undefined,
  categories: [],
  data: [],
  successData: [],
  failureData: [],
}
for (let i = 0; i < chartContext.count; i++) { chartContext.categories[i] = i + 1 }

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
  yAxis: [{
    type: 'value',
    name: 'count'
  }
  ],
  series: [
    {
      name: 'success',
      type: 'bar',
      stack: 'count',
      data: [],
      // itemStyle: {
      //   color: "#9836cd"
      // }
    },
    {
      name: 'failure',
      type: 'bar',
      stack: "count",
      data: [],
      itemStyle: {
        color: "#ff0000",
      }
    },
  ]
};
const costOption = {
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
      min: 0,
      boundaryGap: [0.2, 0.2]
    }
  ],
  series: [
    {
      name: 'cost',
      type: 'bar',
      data: chartContext.data
    }
  ]
}
const updateChart = (data: MonitorData) => {
  while (chartContext.cur > chartContext.count) {
    chartContext.data.shift()
    chartContext.successData.shift()
    chartContext.failureData.shift()
    chartContext.cur--
  }
  chartContext.data.push(data.cost)
  chartContext.failureData.push(data.failed)
  chartContext.successData.push(data.success)
  chartContext.cur++

  chartContext.myChart!.setOption<EChartsOption>({
    xAxis: [
      {
        data: chartContext.categories
      }
    ],
    series: [{
      data: chartContext.successData
    }, {
      data: chartContext.failureData
    }
    ]
  })
  chartContext.costChart!.setOption<EChartsOption>({
    xAxis: [
      {
        data: chartContext.categories
      }
    ],
    series: [
      {
        data: chartContext.data
      }
    ]
  })
}
const resetChart = () => {
  chartContext.data.length = 0
  chartContext.failureData.length = 0
  chartContext.successData.length = 0
  chartContext.myChart!.setOption<EChartsOption>({
    xAxis: [
      {
        data: chartContext.categories
      }
    ],
    series: [{
      data: chartContext.successData
    }, {
      data: chartContext.failureData
    }
    ]
  })
  chartContext.costChart!.setOption<EChartsOption>({
    xAxis: [
      {
        data: chartContext.categories
      }
    ],
    series: [
      {
        data: chartContext.data
      }
    ]
  })
}
const transform = (result: ArthasResResult) => {
  if (result.type === "monitor") {
    result.monitorDataList.forEach(data => {
      updateChart(data)
    })
  }
  if (result.type === "enhancer") {
    enhancer.value = result
  }
}
getCommonResEffect(pollingM, body => {
  if (body.results.length > 0) {
    body.results.forEach(result => {
      transform(result)
    })
  }
})

const changeCycle = publicS.inputDialogFactory(
  cycleV,
  (raw) => {
    let valRaw = parseInt(raw)
    return Number.isNaN(valRaw) ? 120 : valRaw
  },
  (input) => input.value.toString()
)
onBeforeMount(() => {
  fetchS.asyncInit()
  pollingM.send("INIT")
})
onMounted(() => {
  const chartDom = document.getElementById('monitorchart')!;
  chartContext.myChart = echarts.init(chartDom);
  chartOption && chartContext.myChart.setOption(chartOption)
  chartContext.costChart = echarts.init(document.getElementById('monitorchartcost')!);
  chartOption && chartContext.costChart.setOption(costOption)
})
onBeforeUnmount(() => {
  loop.close()
})
const submit = async (data: { classItem: Item, methodItem: Item, conditon: string }) => {
  enhancer.value = undefined
  // tableResults.length = 0

  let condition = data.conditon.trim() == "" ? "" : `'${data.conditon.trim()}'`
  let cycle = `-c ${cycleV.value}`
  fetchS.baseSubmit(fetchM, {
    action: "async_exec",
    command: `monitor -c 5 ${data.classItem.value} ${data.methodItem.value} ${condition}`,
    sessionId: undefined
  }).then(
    res => loop.open()
  )
}
</script>

<template>
  <MethodInput :submit-f="submit" class="mb-4" ncondition>
    <template #others>
      <Listbox v-model="mode">
        <div class=" relative mx-2 ">
          <ListboxButton class="input-btn-style w-40">{{ mode.name }}</ListboxButton>
          <ListboxOptions
            class="absolute w-40 mt-2 border overflow-hidden rounded-md hover:shadow-xl transition bg-white">
            <ListboxOption v-for="(am,i) in modelist" :key="i" :value="am" v-slot="{active, selected}">
              <div class=" p-2 transition " :class="{
              'bg-blue-300 text-white': active,
              'bg-blue-500 text-white': selected,
              'text-gray-900': !active && !selected
              }">
                {{ am.name }}
              </div>
            </ListboxOption>
          </ListboxOptions>
        </div>
      </Listbox>
      <button class="input-btn-style ml-2" @click="changeCycle">cycle time:{{cycleV}}</button>
    </template>
  </MethodInput>
  <Enhancer :result="enhancer" v-if="enhancer" class="input-btn-style mb-4"></Enhancer>
  <div id="monitorchart" class="input-btn-style h-60 w-full pointer-events-auto transition mb-2"></div>
  <div id="monitorchartcost" class="input-btn-style h-60 w-full pointer-events-auto transition"></div>

</template>

<style>
.bg {
  background: #9836cd;
}
</style>