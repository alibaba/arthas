// @ts-nocheck
// eslint-disable-next-line

// 获取JFR文件分析维度元数据
export async function fetchJfrMetadata(fileId) {
  const body = {
    namespace: 'jfr-file',
    api: 'metadata',
    target: fileId,
    parameters: {}
  };
  const res = await fetch('/arthas-api/analysis', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  const json = await res.json();
  if (json.code !== 1) throw new Error(json.msg || '元数据获取失败');
  return json;
}

// 获取火焰图数据
export async function fetchJfrFlameGraph({ fileId, dimension, include, taskSet }) {
  const body = {
    namespace: 'jfr-file',
    api: 'flameGraph',
    target: fileId,
    parameters: { dimension, include, taskSet }
  };
  const res = await fetch('/arthas-api/analysis', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  const json = await res.json();
  if (json.code !== 1) throw new Error(json.msg || '火焰图获取失败');
  return json;
} 