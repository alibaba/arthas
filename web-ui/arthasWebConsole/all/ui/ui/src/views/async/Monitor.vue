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
import { onBeforeMount, onBeforeUnmount, reactive, ref } from 'vue';
import Enhancer from '@/components/show/Enhancer.vue';
import { publicStore } from '@/stores/public';
import LineVue from "@/components/charts/Line.vue"
import Bar from '@/components/charts/Bar.vue';
import { BarChartOption, LineChartOption } from '@/echart';
const pollingM = useMachine(machine)
const fetchS = fetchStore()
const { pullResultsLoop, getCommonResEffect } = fetchS
const fetchM = useInterpret(permachine)
const loop = pullResultsLoop(pollingM)
const enhancer = ref(undefined as undefined | EnchanceResult)
const cycleV = ref(120)
const publicS = publicStore()
const modelist: { name: string, value: string }[] = [
  { name: "before", value: "-b" },
  { name: "finish", value: "" }
]
const mode = ref(modelist[1])

const averageRT = ref({
  totalCost: 0,
  totalCount: 0,
})

const chartContext: {
  categories: string[],
  data: number[],
  successData: number[],
  failureData: number[],
  dataZoom: Record<string, unknown>
} = reactive({
  categories: [],
  data: [],
  successData: [],
  failureData: [],
  dataZoom: {
    type: "inside",
    minValueSpan: 10,
    maxValueSpan: 10,
    start: 50,
    end: 100,
    throttle: 0,
    zoomLock: true
  }
})

const chartOption = reactive<BarChartOption>({
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
  dataZoom: chartContext.dataZoom,
  xAxis: [
    {
      type: 'category',
      data: chartContext.categories,
      axisLabel: {
        formatter(value: string) {
          return value.split(" ")[1]
        }
      }
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
      data: chartContext.successData,
    },
    {
      name: 'failure',
      type: 'bar',
      stack: "count",
      data: chartContext.failureData,
      itemStyle: {
        color: "#ff0000",
      }
    },
  ]
});
const costOption = reactive<LineChartOption>({
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
  dataZoom: chartContext.dataZoom,
  // toolbox: {
  //   show: true,
  //   feature: {
  //     dataView: { readOnly: false },
  //   }
  // },
  xAxis: {
    type: 'category',
    data: chartContext.categories,
    axisLabel: {
      formatter(value: string) {
        return value.split(" ")[1]
      }
    }
  },
  yAxis: {
    type: 'value',
    scale: true,
    name: 'rt(ms)',
    min: 0,
  },
  series: {
    name: 'rt',
    type: 'line',
    data: chartContext.data
  }
})
const updateChart = (data: MonitorData) => {
  chartContext.data.push(data.cost / data.total)
  chartContext.failureData.push(data.failed)
  chartContext.successData.push(data.success)
  chartContext.categories.push(data.timestamp)

}
const resetChart = () => {
  chartContext.data.length = 0
  chartContext.failureData.length = 0
  chartContext.successData.length = 0
  enhancer.value = undefined
  averageRT.value.totalCost = 0
  averageRT.value.totalCount = 0
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
onBeforeUnmount(() => {
  loop.close()
})
const submit = async (data: { classItem: Item, methodItem: Item, conditon: string }) => {
  resetChart()
  let condition = data.conditon.trim() == "" ? "" : `'${data.conditon.trim()}'`
  let cycle = `-c ${cycleV.value}`
  fetchS.baseSubmit(fetchM, {
    action: "async_exec",
    command: `monitor ${cycle} ${data.classItem.value as string} ${data.methodItem.value} ${condition}`,
    sessionId: undefined
  }).then(
    _res => loop.open()
  )
}
</script>

<template>
  <MethodInput :submit-f="submit" class="mb-4" ncondition>
    <template #others>
      <Listbox v-model="mode">
        <div class=" relative mx-2 ">
          <ListboxButton class="btn btn-sm btn-outline w-40">{{ mode.name }}</ListboxButton>
          <ListboxOptions
            class="absolute w-40 mt-2 border overflow-hidden rounded-md hover:shadow-xl transition bg-white z-10">
            <ListboxOption v-for="(am, i) in modelist" :key="i" :value="am" v-slot="{ active, selected }">
              <div class=" p-2 transition" :class="{
                'bg-neutral text-neutral-content': active,
                'bg-neutral-focus text-neutral-content': selected,
              }">
                {{ am.name }}
              </div>
            </ListboxOption>
          </ListboxOptions>
        </div>
      </Listbox>
      <button class="btn btn-sm btn-outline" @click="changeCycle">cycle time:{{ cycleV }}</button>
    </template>
  </MethodInput>
  <Enhancer :result="enhancer" v-if="enhancer" class="mb-4">

  </Enhancer>
  <!-- <div id="monitorchart" class="input-btn-style h-60 w-full pointer-events-auto transition mb-2"></div> -->
  <Bar class="h-60 pointer-events-auto transition" :option="chartOption" />
  <!-- <div id="monitorchartcost" class="input-btn-style h-60 w-full pointer-events-auto transition"></div> -->
  <LineVue class="h-60 pointer-events-auto transition" :option="costOption" />
</template>
