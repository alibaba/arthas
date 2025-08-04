// @ts-nocheck
// eslint-disable-next-line
import React, { useState, useRef } from 'react';
import { Upload, message, Button, Progress } from 'antd';
import { InboxOutlined, StopOutlined } from '@ant-design/icons';
import { uploadFile } from '../../services/fileService';
import { useFileContext } from '../../stores/FileContext';

const { Dragger } = Upload;

const MAX_SIZE = 2 * 1024 * 1024 * 1024; // 2GB

const FileUpload: React.FC<{ onUploadSuccess?: () => void }> = ({ onUploadSuccess }) => {
  const { refreshFiles } = useFileContext();
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [fileList, setFileList] = useState([]);
  const xhrRef = useRef<any>(null);

  // 文件类型和大小校验
  const beforeUpload = (file: File) => {
    if (!file.name.endsWith('.jfr')) {
      message.error('只允许上传 .jfr 文件');
      return Upload.LIST_IGNORE;
    }
    if (file.size > MAX_SIZE) {
      message.error('文件大小不能超过2GB');
      return Upload.LIST_IGNORE;
    }
    return true;
  };

  // 使用真实API上传
  const customRequest = async (options: any) => {
    const { file, onSuccess, onError } = options;
    setUploading(true);
    setProgress(30);
    try {
      const res = await uploadFile(file, 'JFR');
      if (res.code === 1) {
        setProgress(100);
        // 立即刷新文件列表
        await refreshFiles();
        message.success('上传成功');
        onSuccess && onSuccess();
        onUploadSuccess && onUploadSuccess();
        
        // 延迟重置上传状态
        setTimeout(() => {
          setUploading(false);
          setProgress(0);
          setFileList([]);
        }, 500);
      } else {
        throw new Error(res.msg || '上传失败');
      }
    } catch (e) {
      setUploading(false);
      setProgress(0);
      message.error(e.message || '上传失败');
      onError && onError(e);
    }
  };

  // 取消上传（mock下直接重置）
  const handleCancel = () => {
    setUploading(false);
    setProgress(0);
    message.info('已取消上传');
  };

  return (
    <div>
      <Dragger
        name="file"
        multiple={false}
        beforeUpload={beforeUpload}
        customRequest={customRequest}
        fileList={fileList}
        onChange={({ fileList }) => setFileList(fileList)}
        showUploadList={{ showRemoveIcon: !uploading }}
        disabled={uploading}
        accept=".jfr"
      >
        <p className="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p className="ant-upload-text">点击或拖拽上传 .jfr 文件（最大2GB）</p>
        <p className="ant-upload-hint">仅支持 .jfr 文件，上传过程中可取消</p>
      </Dragger>
      {uploading && (
        <div style={{ marginTop: 16 }}>
          <Progress percent={progress} status={progress < 100 ? 'active' : 'success'} />
          <Button icon={<StopOutlined />} onClick={handleCancel} style={{ marginTop: 8 }} danger>
            取消上传
          </Button>
        </div>
      )}
    </div>
  );
};

export default FileUpload; 