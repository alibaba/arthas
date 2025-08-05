import api from './api';

export interface FlameGraph {
  data: number[][];
  symbolTable: Record<string, string>;
  threadSplit: Record<string, number>;
}

export interface Metadata {
  perfDimensions: string[];
}

export interface AnalysisRequest {
  filePath: string;
  dimension: string;
  include?: boolean;
  taskSet?: string[];
  options?: Record<string, string>;
}

export interface AnalysisResponse {
  code: number;
  msg: string;
  data: FlameGraph;
}

export interface MetadataResponse {
  code: number;
  msg: string;
  data: Metadata;
}

// 分析JFR文件并生成火焰图
export async function analyzeJFRFile(request: AnalysisRequest): Promise<AnalysisResponse> {
  const response = await api.post('/api/jfr/analyze', null, { params: request });
  return response.data;
}

// 通过文件ID分析JFR文件并生成火焰图
export async function analyzeJFRFileById(
  fileId: string | number,
  dimension: string,
  include: boolean = true,
  taskSet?: string[]
): Promise<AnalysisResponse> {
  const params: any = { dimension, include };
  if (taskSet && taskSet.length > 0) {
    params.taskSet = taskSet;
  }
  const response = await api.post(`/api/jfr/analyze/${fileId}`, null, { params });
  return response.data;
}

// 获取分析元数据
export async function getMetadata(): Promise<MetadataResponse> {
  const response = await api.get('/api/jfr/metadata');
  return response.data;
}

// 验证JFR文件是否有效
export async function validateJFRFile(filePath: string): Promise<{ code: number; msg: string; data: boolean }> {
  const response = await api.get('/api/jfr/validate', { params: { filePath } });
  return response.data;
}

// 获取支持的分析维度列表
export async function getSupportedDimensions(): Promise<{ code: number; msg: string; data: string[] }> {
  const response = await api.get('/api/jfr/dimensions');
  return response.data;
} 