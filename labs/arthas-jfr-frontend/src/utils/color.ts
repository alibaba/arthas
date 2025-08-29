// 火焰图颜色工具函数

// 专业的火焰图颜色方案
const FLAME_COLORS = [
  // 蓝色系 - 系统调用和底层函数
  '#1f77b4', '#aec7e8', '#ff7f0e', '#ffbb78',
  // 绿色系 - 业务逻辑函数
  '#2ca02c', '#98df8a', '#d62728', '#ff9896',
  // 紫色系 - 框架和库函数
  '#9467bd', '#c5b0d5', '#8c564b', '#c49c94',
  // 橙色系 - 工具函数
  '#e377c2', '#f7b6d2', '#7f7f7f', '#c7c7c7',
  // 红色系 - 性能热点
  '#bcbd22', '#dbdb8d', '#17becf', '#9edae5',
  // 深色系 - 特殊函数
  '#393b79', '#637939', '#8c6d31', '#b5cf6b',
  '#d6616b', '#843c39', '#7b4173', '#5254a3'
];

// 基于函数名的颜色生成
export function colorForName(name: string, customColors?: string[]): string {
  const colors = customColors || FLAME_COLORS;
  
  if (!name || name === 'root') {
    return '#666666'; // root节点使用灰色
  }
  
  // 使用字符串哈希生成颜色索引
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    const char = name.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // 转换为32位整数
  }
  
  const index = Math.abs(hash) % colors.length;
  return colors[index];
}

// 基于函数类型的颜色生成
export function colorForFunctionType(name: string): string {
  const lowerName = name.toLowerCase();
  
  // 系统函数
  if (lowerName.includes('system') || lowerName.includes('native') || lowerName.includes('jvm')) {
    return '#1f77b4'; // 深蓝色
  }
  
  // 数据库相关
  if (lowerName.includes('sql') || lowerName.includes('jdbc') || lowerName.includes('hibernate')) {
    return '#2ca02c'; // 绿色
  }
  
  // 网络相关
  if (lowerName.includes('http') || lowerName.includes('socket') || lowerName.includes('netty')) {
    return '#ff7f0e'; // 橙色
  }
  
  // 文件操作
  if (lowerName.includes('file') || lowerName.includes('io') || lowerName.includes('nio')) {
    return '#d62728'; // 红色
  }
  
  // 集合操作
  if (lowerName.includes('list') || lowerName.includes('map') || lowerName.includes('set')) {
    return '#9467bd'; // 紫色
  }
  
  // 线程相关
  if (lowerName.includes('thread') || lowerName.includes('executor') || lowerName.includes('pool')) {
    return '#8c564b'; // 棕色
  }
  
  // 缓存相关
  if (lowerName.includes('cache') || lowerName.includes('redis') || lowerName.includes('memcached')) {
    return '#e377c2'; // 粉色
  }
  
  // 默认使用哈希颜色
  return colorForName(name);
}

// 基于性能热度的颜色生成
export function colorForPerformance(value: number, maxValue: number): string {
  if (maxValue === 0) return '#666666';
  
  const ratio = value / maxValue;
  
  if (ratio > 0.8) {
    return '#d62728'; // 红色 - 高热度
  } else if (ratio > 0.6) {
    return '#ff7f0e'; // 橙色 - 中高热度
  } else if (ratio > 0.4) {
    return '#ffbb78'; // 浅橙色 - 中热度
  } else if (ratio > 0.2) {
    return '#98df8a'; // 浅绿色 - 中低热度
  } else {
    return '#2ca02c'; // 绿色 - 低热度
  }
}

// 基于调用深度的颜色生成
export function colorForDepth(depth: number, maxDepth: number): string {
  if (maxDepth === 0) return '#666666';
  
  const ratio = depth / maxDepth;
  
  if (ratio > 0.8) {
    return '#393b79'; // 深紫色 - 深层调用
  } else if (ratio > 0.6) {
    return '#637939'; // 深绿色 - 中深层调用
  } else if (ratio > 0.4) {
    return '#8c6d31'; // 棕色 - 中层调用
  } else if (ratio > 0.2) {
    return '#b5cf6b'; // 浅绿色 - 中浅层调用
  } else {
    return '#d6616b'; // 浅红色 - 浅层调用
  }
}

// 生成渐变色
export function generateGradientColor(startColor: string, endColor: string, ratio: number): string {
  // 简单的颜色插值
  const start = hexToRgb(startColor);
  const end = hexToRgb(endColor);
  
  if (!start || !end) return startColor;
  
  const r = Math.round(start.r + (end.r - start.r) * ratio);
  const g = Math.round(start.g + (end.g - start.g) * ratio);
  const b = Math.round(start.b + (end.b - start.b) * ratio);
  
  return `rgb(${r}, ${g}, ${b})`;
}

// 辅助函数：十六进制转RGB
function hexToRgb(hex: string): {r: number, g: number, b: number} | null {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : null;
}

// 获取对比度颜色（用于文字）
export function getContrastColor(backgroundColor: string): string {
  const rgb = hexToRgb(backgroundColor);
  if (!rgb) return '#000000';
  
  // 计算亮度
  const brightness = (rgb.r * 299 + rgb.g * 587 + rgb.b * 114) / 1000;
  
  // 根据亮度返回黑色或白色
  return brightness > 128 ? '#000000' : '#ffffff';
}

// 生成调色板
export function generateColorPalette(count: number, baseColor?: string): string[] {
  const colors: string[] = [];
  
  for (let i = 0; i < count; i++) {
    if (baseColor) {
      // 基于基础颜色生成变体
      const hue = Math.random() * 360;
      const saturation = 60 + Math.random() * 40; // 60-100%
      const lightness = 40 + Math.random() * 40; // 40-80%
      colors.push(`hsl(${hue}, ${saturation}%, ${lightness}%)`);
    } else {
      // 使用预定义颜色
      colors.push(FLAME_COLORS[i % FLAME_COLORS.length]);
    }
  }
  
  return colors;
}

// 导出预定义颜色
export { FLAME_COLORS }; 