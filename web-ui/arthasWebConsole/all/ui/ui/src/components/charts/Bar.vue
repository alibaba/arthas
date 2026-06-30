<script setup lang="ts">
import Chart from "./Base.vue"
import { DataZoomComponent, GridComponent } from 'echarts/components';
import { BarChart,} from 'echarts/charts';
import { LabelLayout } from 'echarts/features';
import { BarChartOption } from "@/echart";
let useList = ([
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