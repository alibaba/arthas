<script setup lang="ts">
import Chart from "./Base.vue"
import { DataZoomComponent,  GridComponent } from 'echarts/components';
import { LineChart } from 'echarts/charts';
import { LabelLayout } from 'echarts/features';
import { LineChartOption } from "@/echart";
let useList = ([
  LineChart,
  LabelLayout,
  GridComponent, 
  DataZoomComponent
]);
const props = defineProps<{ option: LineChartOption }>()
</script>

<template>
  <Chart :use-list="useList" :option="props.option"></Chart>
</template>