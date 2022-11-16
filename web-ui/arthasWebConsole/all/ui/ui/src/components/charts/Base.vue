<script setup lang="ts">
import * as echarts from 'echarts/core';
import { SVGRenderer } from 'echarts/renderers';
import { TitleComponent, TitleComponentOption, TooltipComponent, TooltipComponentOption, ToolboxComponent, LegendComponent} from 'echarts/components'
import { onBeforeUnmount, onMounted, Ref, ref, watch } from 'vue';
// option && myChart.setOption(option);

type GetTuple<T extends unknown> = T extends T
  ? T extends [...infer E]
  ? T
  : never
  : never
type UseType = GetTuple<Parameters<typeof echarts.use>[0]>
// echarts.ECharts
type OptionType = Parameters<echarts.ECharts["setOption"]>[0]
const props = defineProps<{
  useList: UseType,
  option: OptionType,
}>()

// type EChartsOption = echarts.ComposeOption<TooltipComponentOption | TitleComponentOption | DatasetComponentOption | PieSeriesOption>;
const container = ref(null)
let myChart: echarts.ECharts
echarts.use([
  ...props.useList,
  SVGRenderer,
  TitleComponent,
  TooltipComponent,
  ToolboxComponent,
  LegendComponent
])


// 抽离出来使得resize事件可以在全局挂载和卸载
const resizeDom = () => {
  myChart && myChart.resize()
}
onMounted(() => {
  // container.value.el.id = chartId.toString()
  let dom = container.value
  myChart = echarts.init(dom as unknown as HTMLElement)
  myChart && myChart.setOption<OptionType>({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    ...props.option
  })
  window.addEventListener("resize", resizeDom)
})
watch(props.option, (newData: any) => {
  myChart && newData && myChart.setOption(newData, true)
  console.log(newData)
},
  {
    immediate: true,
    deep: true
  })
onBeforeUnmount(() => {
  myChart && myChart.dispose()
  window.removeEventListener("resize", resizeDom)
})
</script>

<template>
  <div ref="container" class="w-full h-full"></div>
</template>

<style scoped>

</style>