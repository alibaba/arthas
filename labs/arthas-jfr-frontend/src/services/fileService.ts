import api from './api';

export interface FileView {
  id: number;
  uniqueName: string;
  originalName: string;
  size: number;
  type: string;
  createdTime: string;
  status?: string;
}

export interface PageView<T> {
  total: number;
  page: number;
  pageSize: number;
  items: T[];
}

export interface FileUploadResponse {
  code: number;
  msg: string;
  data: number;
}

export interface FileListResponse {
  code: number;
  msg: string;
  data: PageView<FileView>;
}

// 获取文件列表
export async function getFiles(params: {
  page?: number;
  pageSize?: number;
  type?: string;
  search?: string;
}): Promise<FileListResponse> {
  const response = await api.get('/files', { params });
  const raw = response.data;
  // 兼容后端返回结构
  let items = raw.data?.items || raw.data?.data || [];
  let total = raw.data?.total || raw.data?.totalSize || 0;
  let page = raw.data?.page || 1;
  let pageSize = raw.data?.pageSize || 10;
  return {
    code: raw.code,
    msg: raw.msg,
    data: {
      items,
      total,
      page,
      pageSize,
    },
  };
}

// 上传文件
export async function uploadFile(file: File, type: string = 'JFR'): Promise<FileUploadResponse> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('type', type);
  
  const response = await api.post('/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
}

// 删除文件
export async function deleteFile(fileId: number): Promise<{ code: number; msg: string }> {
  const response = await api.delete(`/files/${fileId}`);
  return response.data;
} 