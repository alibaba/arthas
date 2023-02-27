import * as echarts from "echarts";
import {
  DatasetComponentOption,
  DataZoomComponentOption,
  GridComponentOption,
  LegendComponentOption,
  TitleComponentOption,
  ToolboxComponentOption,
  TooltipComponentOption,
} from "echarts/components";
import {
  BarSeriesOption,
  LineSeriesOption,
  PieSeriesOption,
} from "echarts/charts";
type LineChartOption = echarts.ComposeOption<
  | TooltipComponentOption
  | TitleComponentOption
  | DatasetComponentOption
  | LineSeriesOption
  | TitleComponentOption
  | ToolboxComponentOption
  | TooltipComponentOption
  | GridComponentOption
  | LegendComponentOption
  | DataZoomComponentOption
  | LineSeriesOption
>;

type BarChartOption = echarts.ComposeOption<
  | TooltipComponentOption
  | TitleComponentOption
  | DatasetComponentOption
  | BarSeriesOption
  | DataZoomComponentOption
  | LegendComponentOption
  | GridComponentOption
  | ToolboxComponentOption
>;

