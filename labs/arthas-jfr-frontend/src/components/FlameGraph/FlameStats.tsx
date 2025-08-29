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
              title="总耗时"
              value={toReadableValue(unit, totalValue)}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ fontSize: '16px', color: darkMode ? '#fff' : '#000' }}
            />
          </Col>
          <Col span={12}>
            <Statistic
              title="平均耗时"
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
              最高耗时: {toReadableValue(unit, stats.maxValue)}
            </Text>
            <Text style={{ fontSize: 12, color: darkMode ? '#ccc' : '#666' }}>
              最低耗时: {toReadableValue(unit, stats.minValue)}
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

      {/* 选中节点统计 */}
      {selectedNodeStats && (
        <>
          <Divider style={{ margin: '12px 0' }} />
          <div>
            <Title level={5} style={{ color: darkMode ? '#fff' : '#000', marginBottom: 8 }}>
              选中节点统计
            </Title>
            <Row gutter={[16, 8]}>
              <Col span={12}>
                <Statistic
                  title="耗时占比"
                  value={selectedNodeStats.percentage}
                  suffix="%"
                  valueStyle={{ fontSize: '14px', color: '#1890ff' }}
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title="子函数数"
                  value={selectedNodeStats.childrenCount}
                  valueStyle={{ fontSize: '14px', color: darkMode ? '#fff' : '#000' }}
                />
              </Col>
            </Row>
            <div style={{ marginTop: 8 }}>
              <Text style={{ fontSize: 12, color: darkMode ? '#ccc' : '#666' }}>
                调用深度: {selectedNodeStats.depth}
              </Text>
            </div>
          </div>
        </>
      )}

      {/* 分析建议 */}
      <Divider style={{ margin: '12px 0' }} />
      <div style={{ 
        background: darkMode ? '#1a1a1a' : '#f8f9fa',
        padding: 8,
        borderRadius: 4,
        fontSize: 12
      }}>
        <div style={{ 
          display: 'flex', 
          alignItems: 'center',
          marginBottom: 4,
          color: darkMode ? '#ccc' : '#666'
        }}>
          <InfoCircleOutlined style={{ marginRight: 4 }} />
          <Text strong>分析建议</Text>
        </div>
        <div style={{ color: darkMode ? '#999' : '#999' }}>
          {stats.maxDepth > 20 && '调用链较深，建议优化函数嵌套'}
          {stats.maxDepth <= 20 && stats.maxDepth > 10 && '调用链适中，关注性能热点'}
          {stats.maxDepth <= 10 && '调用链较浅，整体结构良好'}
        </div>
      </div>
    </Card>
  );
};

export default FlameStats;
