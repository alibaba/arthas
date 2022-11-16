<script setup lang="ts">
import Chart from "./Base.vue"
import * as echarts from 'echarts/core';
import { DatasetComponentOption, DataZoomComponent, DataZoomComponentOption, GridComponent, GridComponentOption, LegendComponent, LegendComponentOption, TitleComponentOption, ToolboxComponentOption, TooltipComponentOption } from 'echarts/components';
import { BarChart, BarSeriesOption, } from 'echarts/charts';
import { LabelLayout } from 'echarts/features';
import { BarChartOption } from "@/echart";
let useList = ([
  // LegendComponent,
  BarChart,
  LabelLayout,
  GridComponent,
  DataZoomComponent
]);
const props = defineProps<{ option: BarChartOption }>()
</script>

<template>
  <Chart :use-list="useList" :option="props.option"></Chart>
</template>