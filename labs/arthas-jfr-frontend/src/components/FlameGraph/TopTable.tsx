import React, { useState, useMemo } from 'react';
import { Card, Typography, Table, Tag, Space, Input, Button, Tooltip } from 'antd';
import { 
  SearchOutlined, 
  SortAscendingOutlined, 
  SortDescendingOutlined,
  FunctionOutlined,
  EyeOutlined
} from '@ant-design/icons';
import { toReadableValue } from '../../utils/format';
import { colorForFunctionType } from '../../utils/color';

const { Title, Text } = Typography;
const { Search } = Input;

interface TopTableProps {
  flameData: any;
  dimension: string;
  unit: string;
  darkMode: boolean;
  onNodeSelect?: (node: any) => void;
}

interface FunctionStats {
  name: string;
  self: number;
  total: number;
  selfPercentage: number;
  totalPercentage: number;
  callCount: number;
  depth: number;
  children: any[];
}

const TopTable: React.FC<TopTableProps> = ({
  flameData,
  dimension,
  unit,
  darkMode,
  onNodeSelect
}) => {
  const [searchText, setSearchText] = useState('');
  const [sortField, setSortField] = useState<'self' | 'total'>('self');
  const [sortOrder, setSortOrder] = useState<'ascend' | 'descend'>('descend');

  // 计算函数统计信息
  const functionStats = useMemo(() => {
    if (!flameData) return [];

    const stats: FunctionStats[] = [];
    const functionMap = new Map<string, FunctionStats>();

    const traverse = (node: any, depth: number, parentValue: number) => {
      const funcName = node.name;
      const existing = functionMap.get(funcName);
      
      if (existing) {
        existing.self += node.value;
        existing.total += node.value;
        existing.callCount += 1;
        existing.depth = Math.max(existing.depth, depth);
        if (node.children) {
          existing.children.push(...node.children);
        }
      } else {
        const totalValue = calculateTotalValue(node);
        functionMap.set(funcName, {
          name: funcName,
          self: node.value,
          total: totalValue,
          selfPercentage: 0,
          totalPercentage: 0,
          callCount: 1,
          depth,
          children: node.children ? [...node.children] : []
        });
      }

      if (node.children) {
        for (const child of node.children) {
          traverse(child, depth + 1, node.value);
        }
      }
    };

    const calculateTotalValue = (node: any): number => {
      let total = node.value || 0;
      if (node.children) {
        for (const child of node.children) {
          total += calculateTotalValue(child);
        }
      }
      return total;
    };

    traverse(flameData, 0, 0);

    // 计算百分比
    const totalValue = flameData.value || 0;
    for (const stat of functionMap.values()) {
      stat.selfPercentage = (stat.self / totalValue) * 100;
      stat.totalPercentage = (stat.total / totalValue) * 100;
    }

    return Array.from(functionMap.values());
  }, [flameData]);

  // 过滤和排序数据
  const filteredAndSortedData = useMemo(() => {
    let data = functionStats.filter(stat => 
      stat.name.toLowerCase().includes(searchText.toLowerCase())
    );

    // 排序
    data.sort((a, b) => {
      const aValue = a[sortField];
      const bValue = b[sortField];
      
      if (sortOrder === 'ascend') {
        return aValue - bValue;
      } else {
        return bValue - aValue;
      }
    });

    return data;
  }, [functionStats, searchText, sortField, sortOrder]);

  // 表格列定义
  const columns = [
    {
      title: 'Symbol',
      dataIndex: 'name',
      key: 'name',
      width: '40%',
      render: (text: string, record: FunctionStats) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <SearchOutlined style={{ color: '#1890ff', fontSize: 12 }} />
          <Tooltip title={text}>
            <Text 
              style={{ 
                color: darkMode ? '#fff' : '#000',
                cursor: 'pointer',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap'
              }}
              onClick={() => onNodeSelect?.(record)}
            >
              {text}
            </Text>
          </Tooltip>
        </div>
      ),
    },
    {
      title: 'Self',
      dataIndex: 'self',
      key: 'self',
      width: '30%',
      sorter: true,
      sortOrder: sortField === 'self' ? sortOrder : null,
      render: (value: number, record: FunctionStats) => (
        <div>
          <div style={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center',
            marginBottom: 4
          }}>
            <Text style={{ color: darkMode ? '#fff' : '#000' }}>
              {toReadableValue(unit, value)}
            </Text>
            <Tag size="small" color="blue">
              {record.selfPercentage.toFixed(1)}%
            </Tag>
          </div>
          <div style={{ 
            width: '100%', 
            height: 4, 
            background: darkMode ? '#333' : '#f0f0f0',
            borderRadius: 2,
            overflow: 'hidden'
          }}>
            <div 
              style={{ 
                width: `${record.selfPercentage}%`, 
                height: '100%', 
                background: colorForFunctionType(record.name),
                borderRadius: 2
              }} 
            />
          </div>
        </div>
      ),
    },
    {
      title: 'Total',
      dataIndex: 'total',
      key: 'total',
      width: '30%',
      sorter: true,
      sortOrder: sortField === 'total' ? sortOrder : null,
      render: (value: number, record: FunctionStats) => (
        <div>
          <div style={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center',
            marginBottom: 4
          }}>
            <Text style={{ color: darkMode ? '#fff' : '#000' }}>
              {toReadableValue(unit, value)}
            </Text>
            <Tag size="small" color="green">
              {record.totalPercentage.toFixed(1)}%
            </Tag>
          </div>
          <div style={{ 
            width: '100%', 
            height: 4, 
            background: darkMode ? '#333' : '#f0f0f0',
            borderRadius: 2,
            overflow: 'hidden'
          }}>
            <div 
              style={{ 
                width: `${record.totalPercentage}%`, 
                height: '100%', 
                background: colorForFunctionType(record.name),
                borderRadius: 2
              }} 
            />
          </div>
        </div>
      ),
    }
  ];

  // 处理排序变化
  const handleTableChange = (pagination: any, filters: any, sorter: any) => {
    if (sorter.field) {
      setSortField(sorter.field as 'self' | 'total');
      setSortOrder(sorter.order || 'descend');
    }
  };

  // 获取总统计信息
  const totalStats = useMemo(() => {
    if (!flameData) return { totalTime: 0, totalSamples: 0 };
    
    const totalTime = flameData.value || 0;
    const totalSamples = functionStats.reduce((sum, stat) => sum + stat.callCount, 0);
    
    return { totalTime, totalSamples };
  }, [flameData, functionStats]);

  if (!flameData) {
    return (
      <Card 
        title="Top Table" 
        size="small"
        style={{ 
          background: darkMode ? '#262626' : '#fff',
          borderColor: darkMode ? '#434343' : '#f0f0f0'
        }}
      >
        <div style={{ 
          textAlign: 'center', 
          padding: '20px',
          color: darkMode ? '#999' : '#666'
        }}>
          <FunctionOutlined style={{ fontSize: 24, marginBottom: 8 }} />
          <div>暂无火焰图数据</div>
        </div>
      </Card>
    );
  }

  return (
    <Card 
      title={
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Space>
            <FunctionOutlined />
            <span>Top Table</span>
          </Space>
          <div style={{ fontSize: 12, color: darkMode ? '#ccc' : '#666' }}>
            {toReadableValue(unit, totalStats.totalTime)} | {totalStats.totalSamples} samples
          </div>
        </div>
      } 
      size="small"
      style={{ 
        background: darkMode ? '#262626' : '#fff',
        borderColor: darkMode ? '#434343' : '#f0f0f0'
      }}
      bodyStyle={{ padding: 0 }}
    >
      {/* 搜索栏 */}
      <div style={{ 
        padding: '12px 16px', 
        borderBottom: `1px solid ${darkMode ? '#434343' : '#f0f0f0'}`,
        background: darkMode ? '#1f1f1f' : '#fafafa'
      }}>
        <Search
          placeholder="Search functions..."
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          style={{ width: '100%' }}
          allowClear
        />
      </div>

      {/* 表格 */}
      <div style={{ maxHeight: 400, overflow: 'auto' }} className={darkMode ? 'top-table-container' : ''}>
        <Table
          columns={columns}
          dataSource={filteredAndSortedData}
          pagination={false}
          size="small"
          onChange={handleTableChange}
          rowKey="name"
          scroll={{ y: 350 }}
          style={{
            background: darkMode ? '#262626' : '#fff',
          }}
          rowClassName={(record, index) => 
            index === 0 ? 'top-function-row' : ''
          }
          onRow={(record) => ({
            onClick: () => onNodeSelect?.(record),
            style: { cursor: 'pointer' }
          })}
        />
      </div>

      {/* 底部统计 */}
      <div style={{ 
        padding: '8px 16px', 
        borderTop: `1px solid ${darkMode ? '#434343' : '#f0f0f0'}`,
        background: darkMode ? '#1f1f1f' : '#fafafa',
        fontSize: 12,
        color: darkMode ? '#ccc' : '#666',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
      }}>
        <span>显示 {filteredAndSortedData.length} 个函数</span>
        <span>
          排序: {sortField === 'self' ? 'Self' : 'Total'} 
          {sortOrder === 'ascend' ? <SortAscendingOutlined /> : <SortDescendingOutlined />}
        </span>
      </div>
    </Card>
  );
};

export default TopTable;
