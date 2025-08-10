// @ts-nocheck
// eslint-disable-next-line

// mock 文件列表
let fileList = Array.from({ length: 12 }).map((_, i) => ({
  id: `${1000 + i}`,
  name: `sample_${i + 1}.jfr`,
  type: 'jfr',
  size: Math.round(Math.random() * 1024 * 1024 * 100) / (1024 * 1024), // MB
  createTime: new Date(Date.now() - i * 3600 * 1000).toISOString(),
  status: ['处理中', '分析完成', '失败'][i % 3],
}));

export function getFiles({ page = 1, pageSize = 10, search = '', sorter = {} }) {
  let data = fileList.filter(f => f.name.includes(search));
  if (sorter.field && sorter.order) {
    data = [...data].sort((a, b) => {
      if (sorter.order === 'ascend') return a[sorter.field] > b[sorter.field] ? 1 : -1;
      return a[sorter.field] < b[sorter.field] ? 1 : -1;
    });
  }
  const total = data.length;
  data = data.slice((page - 1) * pageSize, page * pageSize);
  return Promise.resolve({ 
    code: 1, 
    msg: 'success', 
    data: {
      total,
      page,
      pageSize,
      items: data
    }
  });
}

export function uploadFile(file) {
  // 简单模拟上传
  return new Promise((resolve) => {
    setTimeout(() => {
      const newFileId = Date.now();
      fileList.unshift({
        id: `${newFileId}`,
        name: file.name,
        type: 'jfr',
        size: file.size / (1024 * 1024),
        createTime: new Date().toISOString(),
        status: '处理中',
      });
      resolve({ 
        code: 1, 
        msg: 'success', 
        data: newFileId 
      });
    }, 1000);
  });
}

export function deleteFile(id) {
  fileList = fileList.filter(f => f.id !== id);
  return Promise.resolve({ 
    code: 1, 
    msg: 'success' 
  });
}

export function getDimensions({ fileId }) {
  // 模拟接口文档格式
  return Promise.resolve({
    code: 1,
    msg: 'success',
    data: {
      perfDimensions: [
        {
          key: 'CPU',
          unit: 'ns',
          filters: [
            { key: 'Thread' },
            { key: 'Class' },
            { key: 'Method' }
          ]
        },
        {
          key: 'Allocation',
          unit: 'bytes',
          filters: [
            { key: 'Thread' },
            { key: 'Class' }
          ]
        }
      ]
    }
  });
}

export function getTasks() {
  return Promise.resolve(Array.from({ length: 20 }).map((_, i) => `Thread-${i + 1}`));
}

export function getFlameGraph({ fileId, dimension, include, taskSet }) {
  // 模拟接口文档格式
  let data, symbolTable, threadSplit;
  if (dimension === 'CPU') {
    data = [
      [[0, 1, 2], 1000],
      [[0, 1, 3], 500],
      [[0, 4], 300]
    ];
    symbolTable = {
      0: 'java.lang.Thread.run',
      1: 'com.example.Main.main',
      2: 'com.example.Service.process',
      3: 'com.example.Service.validate',
      4: 'com.example.Util.calculate'
    };
    threadSplit = { main: 1500, 'worker-1': 300 };
  } else if (dimension === 'Allocation') {
    data = [
      [[0, 5, 6], 800],
      [[0, 5, 7], 400],
      [[0, 8], 200]
    ];
    symbolTable = {
      0: 'java.lang.Thread.run',
      5: 'com.example.Allocator.alloc',
      6: 'com.example.Buffer.create',
      7: 'com.example.Buffer.expand',
      8: 'com.example.Util.allocHelper'
    };
    threadSplit = { main: 1200, 'worker-2': 200 };
  } else {
    data = [];
    symbolTable = {};
    threadSplit = {};
  }
  return Promise.resolve({
    code: 1,
    msg: 'success',
    data: {
      data,
      symbolTable,
      threadSplit
    }
  });
} 