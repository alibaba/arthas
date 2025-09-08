
// 耗时格式化工具函数

// 单位转换配置
const TIME_UNITS = {
  ns: { label: '纳秒', factor: 1, short: 'ns' },
  μs: { label: '微秒', factor: 1000, short: 'μs' },
  ms: { label: '毫秒', factor: 1000000, short: 'ms' },
  s: { label: '秒', factor: 1000000000, short: 's' }
};

const MEMORY_UNITS = {
  B: { label: '字节', factor: 1, short: 'B' },
  KB: { label: '千字节', factor: 1024, short: 'KB' },
  MB: { label: '兆字节', factor: 1048576, short: 'MB' },
  GB: { label: '吉字节', factor: 1073741824, short: 'GB' },
  TB: { label: '太字节', factor: 1099511627776, short: 'TB' }
};

// 针对 Byte 单位的特殊处理
export function formatByteValue(bytes: number, showPercentage: boolean = false, totalBytes?: number): string {
  if (bytes === 0) return '0 B';
  
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const k = 1024;
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  const value = bytes / Math.pow(k, i);
  const unit = units[i];
  
  let result = `${value.toLocaleString('en-US', { 
    minimumFractionDigits: 0, 
    maximumFractionDigits: 2 
  })} ${unit}`;
  
  if (showPercentage && totalBytes && totalBytes > 0) {
    const percentage = ((bytes / totalBytes) * 100).toFixed(2);
    result += ` (${percentage}%)`;
  }
  
  return result;
}

// 主要格式化函数
export function toReadableValue(unit: string, value: number, showPercentage: boolean = false, totalValue?: number): string {
  if (value === 0) return '0';
  
  let result = '';
  
  if (unit === 'ns') {
    // to ns
    const ns = value % 1000000;

    // to ms
    let tempValue = Math.round(value / 1000000);
    const ms = tempValue % 1000;
    if (ms > 0) {
      result = ms + 'ms';
    }

    // to second
    tempValue = Math.floor(tempValue / 1000);
    const s = tempValue % 60;
    if (s > 0) {
      if (result.length > 0) {
        result = s + 's ' + result;
      } else {
        result = s + 's';
      }
    }

    // to minute
    tempValue = Math.floor(tempValue / 60);
    const m = tempValue;
    if (m > 0) {
      if (result.length > 0) {
        result = m.toLocaleString() + 'm ' + result;
      } else {
        result = m.toLocaleString() + 'm';
      }
    }

    if (result.length === 0) {
      if (ns > 0) {
        result = ns + 'ns';
      } else {
        result = '0ms';
      }
    }
  } else if (unit === 'byte' || unit === 'B' || unit === 'bytes') {
    const bytes = value % 1024;

    // to Kilobytes
    let tempValue = Math.round(value / 1024);
    const kb = tempValue % 1024;
    if (kb > 0) {
      result = kb + 'KB';
    }

    // to Megabytes
    tempValue = Math.floor(tempValue / 1024);
    const mb = tempValue % 1024;
    if (mb > 0) {
      if (result.length > 0) {
        result = mb + 'MB ' + result;
      } else {
        result = mb + 'MB';
      }
    }

    // to Gigabyte
    tempValue = Math.floor(tempValue / 1024);
    const gb = tempValue % 1024;
    if (gb > 0) {
      if (result.length > 0) {
        result = gb + 'GB ' + result;
      } else {
        result = gb + 'GB';
      }
    }

    // to Terabyte
    tempValue = Math.floor(tempValue / 1024);
    const tb = tempValue;
    if (tb > 0) {
      if (result.length > 0) {
        result = tb + 'TB ' + result;
      } else {
        result = tb + 'TB';
      }
    }

    if (result.length === 0) {
      if (bytes == 0) {
        result = '0B';
      } else {
        result = bytes + 'B';
      }
    }
  } else {
    result = value.toLocaleString();
  }
  
  // 如果需要显示百分比且提供了总值
  if (showPercentage && totalValue && totalValue > 0) {
    const percentage = ((value / totalValue) * 100).toFixed(2);
    result += ` (${percentage}%)`;
  }
  
  return result;
}

// 自动选择最佳单位
function getBestUnit(value: number, unitType: 'time' | 'memory'): { unit: string; factor: number; short: string } {
  const units = unitType === 'time' ? TIME_UNITS : MEMORY_UNITS;
  const unitEntries = Object.entries(units);
  
  // 找到第一个合适的单位
  for (let i = unitEntries.length - 1; i >= 0; i--) {
    const [unit, config] = unitEntries[i];
    if (value >= config.factor) {
      return { unit, factor: config.factor, short: config.short };
    }
  }
  
  // 返回最小单位
  const [firstUnit, firstConfig] = unitEntries[0];
  return { unit: firstUnit, factor: firstConfig.factor, short: firstConfig.short };
}

// 计算 Self 时间（仅当前函数，不包含子函数）
export function calculateSelfTime(node: any, totalValue: number): { self: number; total: number; selfPercentage: number; totalPercentage: number } {
  if (!node) return { self: 0, total: 0, selfPercentage: 0, totalPercentage: 0 };
  
  const total = node.value || 0;
  const children = node.children || [];
  
  // Self = 当前节点值 - 所有子节点值之和
  const childrenSum = children.reduce((sum: number, child: any) => sum + (child.value || 0), 0);
  const self = Math.max(0, total - childrenSum);
  
  const selfPercentage = totalValue > 0 ? (self / totalValue) * 100 : 0;
  const totalPercentage = totalValue > 0 ? (total / totalValue) * 100 : 0;
  
  return {
    self,
    total,
    selfPercentage: parseFloat(selfPercentage.toFixed(2)),
    totalPercentage: parseFloat(totalPercentage.toFixed(2))
  };
}

// 获取单位显示配置
export function getUnitConfig(unit: string): { label: string; short: string; factor: number } {
  if (TIME_UNITS[unit as keyof typeof TIME_UNITS]) {
    return TIME_UNITS[unit as keyof typeof TIME_UNITS];
  }
  if (MEMORY_UNITS[unit as keyof typeof MEMORY_UNITS]) {
    return MEMORY_UNITS[unit as keyof typeof MEMORY_UNITS];
  }
  return { label: unit, short: unit, factor: 1 };
}

// 格式化百分比
export function formatPercentage(value: number, total: number): string {
  if (total === 0) return '0.00%';
  return `${((value / total) * 100).toFixed(2)}%`;
}

// 智能单位选择 - 针对大数值优化
export function smartFormatValue(value: number, originalUnit: string): string {
  // 如果是字节单位且数值过大，自动调整
  if ((originalUnit === 'B' || originalUnit === 'bytes' || originalUnit === 'byte') && value > 1000) {
    return formatByteValue(value);
  }
  
  // 其他情况使用原有逻辑
  return toReadableValue(originalUnit, value);
}

// 获取可读的数值范围
export function getReadableRange(min: number, max: number, unit: string): string {
  const minFormatted = toReadableValue(unit, min);
  const maxFormatted = toReadableValue(unit, max);
  return `${minFormatted} - ${maxFormatted}`;
}

// 文件大小格式化函数
export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';
  
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

// 将字节转换为MB
export const bytesToMB = (bytes: number): number => {
  return bytes / (1024 * 1024);
};

// 将字节转换为KB
export const bytesToKB = (bytes: number): number => {
  return bytes / 1024;
};

// 将字节转换为GB
export const bytesToGB = (bytes: number): number => {
  return bytes / (1024 * 1024 * 1024);
}; 