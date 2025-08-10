// @ts-nocheck
// eslint-disable-next-line
import React, { useState, useMemo } from 'react';
import { Table, Input, Tag, Button, Popconfirm, message } from 'antd';
import { deleteFile } from '../../services/fileService';
import { useFileContext } from '../../stores/FileContext';
import { useWindowSize } from '../../hooks/useWindowSize';
import { formatFileSize } from '../../utils/format';

const statusColor = {
  '处理中': 'processing',
  '分析完成': 'success',
  '失败': 'error',
};

const FileTable: React.FC = () => {
  const { files, refreshFiles } = useFileContext();
  const { width, isMobile } = useWindowSize();
  const [search, setSearch] = useState('');
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10 });
  const [sorter, setSorter] = useState({});
  const [loading, setLoading] = useState(false);

  // 根据搜索条件过滤文件
  const filteredFiles = useMemo(() => {
    if (!search) return files;
    return files.filter(file => 
      file.originalName.toLowerCase().includes(search.toLowerCase())
    );
  }, [files, search]);

  // 根据排序条件排序文件
  const sortedFiles = useMemo(() => {
    if (!sorter || !sorter.field) return filteredFiles;
    
    return [...filteredFiles].sort((a, b) => {
      const aValue = a[sorter.field];
      const bValue = b[sorter.field];
      
      if (typeof aValue === 'string' && typeof bValue === 'string') {
        return sorter.order === 'descend' 
          ? bValue.localeCompare(aValue)
          : aValue.localeCompare(bValue);
      }
      
      if (typeof aValue === 'number' && typeof bValue === 'number') {
        return sorter.order === 'descend' ? bValue - aValue : aValue - bValue;
      }
      
      return 0;
    });
  }, [filteredFiles, sorter]);

  // 分页处理
  const paginatedFiles = useMemo(() => {
    const start = (pagination.current - 1) * pagination.pageSize;
    const end = start + pagination.pageSize;
    return sortedFiles.slice(start, end);
  }, [sortedFiles, pagination.current, pagination.pageSize]);

  const handleDelete = async (id: number) => {
    setLoading(true);
    try {
      const res = await deleteFile(id);
      if (res.code === 1) {
        message.success('删除成功');
        refreshFiles(); // 刷新全局文件列表
      } else {
        message.error(res.msg || '删除失败');
      }
    } catch (e) {
      message.error('删除失败');
    } finally {
      setLoading(false);
    }
  };

  // 动态列配置
  const getColumns = () => {
    const baseColumns = [
      {
        title: '文件名',
        dataIndex: 'originalName',
        key: 'originalName',
        sorter: true,
        ellipsis: true,
        width: isMobile ? '60%' : '30%',
        minWidth: isMobile ? 150 : 200,
        render: (text, record) => (
          <div>
            <div style={{ fontWeight: 500 }}>{text}</div>
            {isMobile && (
              <div style={{ fontSize: 12, color: '#666', marginTop: 4 }}>
                {record.type} • {formatFileSize(record.size)} • {new Date(record.createdTime).toLocaleDateString()}
              </div>
            )}
          </div>
        ),
      },
    ];

    // 桌面端显示完整列
    if (!isMobile) {
      baseColumns.push(
        {
          title: '类型',
          dataIndex: 'type',
          key: 'type',
          width: '10%',
          minWidth: 80,
          responsive: ['lg'],
        },
        {
          title: '大小',
          dataIndex: 'size',
          key: 'size',
          sorter: true,
          render: (size) => formatFileSize(size),
          width: '12%',
          minWidth: 100,
          responsive: ['md'],
        },
        {
          title: '上传时间',
          dataIndex: 'createdTime',
          key: 'createdTime',
          sorter: true,
          render: (time) => new Date(time).toLocaleString(),
          width: '20%',
          minWidth: 150,
          responsive: ['lg'],
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          render: (status) => <Tag color={statusColor[status]}>{status}</Tag>,
          width: '12%',
          minWidth: 100,
          responsive: ['md'],
        }
      );
    }

    // 操作列
    baseColumns.push({
      title: '操作',
      dataIndex: 'action',
      key: 'action',
      width: isMobile ? '40%' : '16%',
      minWidth: isMobile ? 100 : 120,
      fixed: 'right',
      render: (_, record) => (
        <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
          <Popconfirm title="确认删除该文件？" onConfirm={() => handleDelete(record.id)} okText="删除" cancelText="取消">
            <Button type="link" size="small" danger style={{ padding: 0 }}>删除</Button>
          </Popconfirm>
          <Button 
            type="link" 
            size="small" 
            style={{ padding: 0 }}
            onClick={() => window.location.href = `/analysis/${record.id}`}
          >
            分析
          </Button>
        </div>
      ),
    });

    return baseColumns;
  };

  const columns = getColumns();

  return (
    <div style={{ width: '100%' }}>
      <div style={{ 
        marginBottom: 16, 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        flexWrap: 'wrap', 
        gap: 8 
      }}>
        <Input.Search
          placeholder="搜索文件名"
          allowClear
          style={{ 
            width: isMobile ? '100%' : 240, 
            minWidth: isMobile ? 'auto' : 200 
          }}
          onSearch={setSearch}
        />
        {!isMobile && (
          <div style={{ fontSize: 12, color: '#666' }}>
            共 {filteredFiles.length} 个文件
          </div>
        )}
      </div>
      <div style={{ overflow: 'auto' }}>
        <Table
          columns={columns}
          dataSource={paginatedFiles}
          loading={loading}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: filteredFiles.length,
            showSizeChanger: !isMobile,
            pageSizeOptions: ['10', '20', '50'],
            onChange: (page, pageSize) => setPagination({ current: page, pageSize }),
            showQuickJumper: !isMobile,
            showTotal: !isMobile ? (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条` : undefined,
            responsive: true,
            size: isMobile ? 'small' : 'default',
          }}
          onChange={(_, __, sorterObj) => setSorter(sorterObj)}
          rowKey="id"
          size={isMobile ? 'small' : 'middle'}
          scroll={{ 
            x: isMobile ? 600 : 800, 
            y: isMobile ? 300 : 400 
          }}
          style={{ minWidth: isMobile ? 600 : 800 }}
          tableLayout="auto"
        />
      </div>
    </div>
  );
};

export default FileTable; 