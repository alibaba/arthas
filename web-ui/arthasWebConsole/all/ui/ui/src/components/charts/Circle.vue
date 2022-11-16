<script setup lang="ts">
// import Chart from "./Chart.vue"
import * as echarts from 'echarts/core';
import { DatasetComponent, DatasetComponentOption, LegendComponent, TitleComponentOption, TooltipComponentOption } from 'echarts/components';
import { PieChart } from 'echarts/charts';
import { LabelLayout } from 'echarts/features';
import { CircleChartOption } from "@/echart";
import Base from './Base.vue';
let useList = ([
  PieChart,
  LabelLayout,
  DatasetComponent
]);
// type CircleChartsOption = echarts.ComposeOption<TooltipComponentOption | TitleComponentOption | DatasetComponentOption | PieSeriesOption>;
const props = defineProps<{ option: CircleChartOption }>()
</script>

<template>
  <Base :use-list="useList" :option="props.option" />
</template>