import React from 'react';
import { Card, Typography, Statistic, Row, Col, Progress, Tag, Space, Divider } from 'antd';
import { 
  ClockCircleOutlined, 
  FunctionOutlined, 
  BarChartOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  InfoCircleOutlined
} from '@ant-design/icons';
import { toReadableValue } from '../../utils/format';

const { Title, Text } = Typography;

interface FlameStatsProps {
  flameData: any;
  dimension: string;
  unit: string;
  darkMode: boolean;
  selectedNode?: any;
}

interface NodeStats {
  totalNodes: number;
  maxDepth: number;
  avgValue: number;
  maxValue: number;
  minValue: number;
  topFunctions: Array<{
    name: string;
    value: number;
    percentage: number;
  }>;
}

const FlameStats: React.FC<FlameStatsProps> = ({
  flameData,
  dimension,
  unit,
  darkMode,
  selectedNode
}) => {
  // 判断是否为时间维度
  const isTimeDim = unit === 'ns' || unit === 'μs' || unit === 'ms' || unit === 's';
  // 判断是否为内存维度
  const isMemoryDim = unit === 'B' || unit === 'KB' || unit === 'MB' || unit === 'GB' || unit === 'TB';
  
  // 根据维度类型获取显示标签
  const getDimensionLabel = () => {
    if (isTimeDim) return '总耗时';
    if (isMemoryDim) return '总内存';
    return '总数值';
  };
  
  const getAverageLabel = () => {
    if (isTimeDim) return '平均耗时';
    if (isMemoryDim) return '平均内存';
    return '平均值';
  };
  
  const getMaxLabel = () => {
    if (isTimeDim) return '最高耗时';
    if (isMemoryDim) return '最大内存';
    return '最大值';
  };
  
  const getMinLabel = () => {
    if (isTimeDim) return '最低耗时';
    if (isMemoryDim) return '最小内存';
    return '最小值';
  };

  if (!flameData) {
    return (
      <Card 
        title="统计信息" 
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
          <BarChartOutlined style={{ fontSize: 24, marginBottom: 8 }} />
          <div>暂无火焰图数据</div>
        </div>
      </Card>
    );
  }

  // 计算统计信息
  const calculateStats = (node: any, depth = 0): NodeStats => {
    let totalNodes = 1;
    let maxDepth = depth;
    let totalValue = node.value || 0;
    let maxValue = node.value || 0;
    let minValue = node.value || 0;
    const functionMap = new Map<string, number>();

    const traverse = (currentNode: any, currentDepth: number) => {
      totalNodes++;
      maxDepth = Math.max(maxDepth, currentDepth);
      
      const currentValue = currentNode.value || 0;
      totalValue += currentValue;
      maxValue = Math.max(maxValue, currentValue);
      minValue = Math.min(minValue, currentValue);

      // 统计函数调用次数
      const funcName = currentNode.name;
      functionMap.set(funcName, (functionMap.get(funcName) || 0) + 1);

      if (currentNode.children) {
        for (const child of currentNode.children) {
          traverse(child, currentDepth + 1);
        }
      }
    };

    if (node.children) {
      for (const child of node.children) {
        traverse(child, depth + 1);
      }
    }

    // 获取前5个最频繁的函数
    const topFunctions = Array.from(functionMap.entries())
      .map(([name, count]) => ({
        name,
        value: count,
        percentage: (count / totalNodes) * 100
      }))
      .sort((a, b) => b.value - a.value)
      .slice(0, 5);

    return {
      totalNodes,
      maxDepth,
      avgValue: totalValue / totalNodes,
      maxValue,
      minValue,
      topFunctions
    };
  };

  const stats = calculateStats(flameData);
  const totalValue = flameData.value || 0;

  // 计算选中节点的统计信息
  const getSelectedNodeStats = () => {
    if (!selectedNode) return null;

    const nodeValue = selectedNode.value || 0;
    const nodePercentage = ((nodeValue / totalValue) * 100).toFixed(2);
    const childrenCount = selectedNode.children?.length || 0;

    return {
      value: nodeValue,
      percentage: parseFloat(nodePercentage),
      childrenCount,
      depth: getNodeDepth(selectedNode)
    };
  };

  const getNodeDepth = (node: any, depth = 0): number => {
    if (!node.children || node.children.length === 0) return depth;
    return Math.max(...node.children.map((c: any) => getNodeDepth(c, depth + 1)));
  };

  const selectedNodeStats = getSelectedNodeStats();

  return (
    <Card 
      title={
        <Space>
          <BarChartOutlined />
          统计信息
        </Space>
      } 
      size="small"
      style={{ 
        background: darkMode ? '#262626' : '#fff',
        borderColor: darkMode ? '#434343' : '#f0f0f0'
      }}
    >
      {/* 总体统计 */}
      <div style={{ marginBottom: 16 }}>
        <Title level={5} style={{ color: darkMode ? '#fff' : '#000', marginBottom: 12 }}>
          总体统计
        </Title>
        <Row gutter={[16, 8]}>
          <Col span={12}>
            <Statistic
              title="总函数数"
              value={stats.totalNodes}
              prefix={<FunctionOutlined />}
              valueStyle={{ fontSize: '16px', color: darkMode ? '#fff' : '#000' }}
            />
          </Col>
          <Col span={12}>
            <Statistic
              title="最大深度"
              value={stats.maxDepth}
              prefix={<ArrowDownOutlined />}
              valueStyle={{ fontSize: '16px', color: darkMode ? '#fff' : '#000' }}
            />
          </Col>
        </Row>
        <Row gutter={[16, 8]} style={{ marginTop: 8 }}>
          <Col span={12}>
            <Statistic
              title={getDimensionLabel()}
              value={toReadableValue(unit, totalValue)}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ fontSize: '16px', color: darkMode ? '#fff' : '#000' }}
            />
          </Col>
          <Col span={12}>
            <Statistic
              title={getAverageLabel()}
              value={toReadableValue(unit, stats.avgValue)}
              valueStyle={{ fontSize: '16px', color: darkMode ? '#fff' : '#000' }}
            />
          </Col>
        </Row>
      </div>

      <Divider style={{ margin: '12px 0' }} />

      {/* 性能分布 */}
      <div style={{ marginBottom: 16 }}>
        <Title level={5} style={{ color: darkMode ? '#fff' : '#000', marginBottom: 8 }}>
          性能分布
        </Title>
        <div style={{ marginBottom: 8 }}>
          <div style={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center',
            marginBottom: 4
          }}>
            <Text style={{ fontSize: 12, color: darkMode ? '#ccc' : '#666' }}>
              {getMaxLabel()}: {toReadableValue(unit, stats.maxValue)}
            </Text>
            <Text style={{ fontSize: 12, color: darkMode ? '#ccc' : '#666' }}>
              {getMinLabel()}: {toReadableValue(unit, stats.minValue)}
            </Text>
          </div>
          <Progress
            percent={((stats.maxValue - stats.minValue) / stats.maxValue) * 100}
            strokeColor="#1890ff"
            showInfo={false}
            size="small"
          />
        </div>
      </div>

      <Divider style={{ margin: '12px 0' }} />

      {/* 最频繁函数 */}
      <div style={{ marginBottom: 16 }}>
        <Title level={5} style={{ color: darkMode ? '#fff' : '#000', marginBottom: 8 }}>
          最频繁函数
        </Title>
        {stats.topFunctions.map((func, index) => (
          <div key={index} style={{ marginBottom: 8 }}>
            <div style={{ 
              display: 'flex', 
              justifyContent: 'space-between', 
              alignItems: 'center',
              marginBottom: 4
            }}>
              <Text 
                style={{ 
                  fontSize: 12,
                  color: darkMode ? '#d9d9d9' : '#333',
                  flex: 1,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap'
                }}
                title={func.name}
              >
                {index + 1}. {func.name}
              </Text>
              <Tag size="small" color="blue">
                {func.value}次
              </Tag>
            </div>
            <Progress
              percent={func.percentage}
              strokeColor="#52c41a"
              showInfo={false}
              size="small"
            />
          </div>
        ))}
      </div>
    </Card>
  );
};

export default FlameStats;
