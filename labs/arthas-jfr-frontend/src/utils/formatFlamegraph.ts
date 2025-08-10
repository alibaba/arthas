// @ts-nocheck
// eslint-disable-next-line
/**
 * 将JFR后端返回的line格式火焰图数据转换为树结构
 * @param {Array} data - 形如[[0,1,2], 100]的数组，表示栈帧路径和权重
 * @param {Object} symbolTable - {0: 'main', 1: 'foo', ...}
 * @returns {Object} 递归树结构 { name, value, children }
 */
export function formatFlamegraph(data, symbolTable) {
  const root = { name: 'root', value: 0, children: [] };
  for (const [stack, value] of data) {
    let node = root;
    node.value += value;
    for (let i = 0; i < stack.length; i++) {
      const frameId = stack[i];
      const frameName = symbolTable[frameId] || String(frameId);
      let child = node.children.find(c => c.name === frameName);
      if (!child) {
        child = { name: frameName, value: 0, children: [] };
        node.children.push(child);
      }
      child.value += value;
      node = child;
    }
  }
  return root;
} 