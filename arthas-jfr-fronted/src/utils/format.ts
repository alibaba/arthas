// @ts-nocheck
// eslint-disable-next-line
export function toReadableValue(unit: string, value: number) {
  if (unit === 'ns') {
    let result = '';
    const ns = value % 1000000;
    value = Math.round(value / 1000000);
    const ms = value % 1000;
    if (ms > 0) result = ms + 'ms';
    value = Math.floor(value / 1000);
    const s = value % 60;
    if (s > 0) result = s + 's ' + result;
    value = Math.floor(value / 60);
    const m = value;
    if (m > 0) result = m.toLocaleString() + 'm ' + result;
    if (result.length === 0) {
      if (ns > 0) return ns + 'ns';
      else return '0ms';
    }
    return result;
  } else if (unit === 'byte') {
    let result = '';
    const bytes = value % 1024;
    value = Math.round(value / 1024);
    const kb = value % 1024;
    if (kb > 0) result = kb + 'KB';
    value = Math.floor(value / 1024);
    const mb = value % 1024;
    if (mb > 0) result = mb + 'MB ' + result;
    value = Math.floor(value / 1024);
    const gb = value % 1024;
    if (gb > 0) result = gb + 'GB ' + result;
    value = Math.floor(value / 1024);
    const tb = value;
    if (tb > 0) result = tb + 'TB ' + result;
    if (result.length === 0) {
      if (bytes == 0) return '0B';
      else return bytes + 'bytes';
    }
    return result;
  } else {
    return value.toLocaleString();
  }
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