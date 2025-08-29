import React from 'react';
import { Card, Typography, Progress, Tag, Space, Divider } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined, FunctionOutlined } from '@ant-design/icons';
import { toReadableValue } from '../../utils/format';

const { Title, Text } = Typography;

interface SandwichViewProps {
  selectedNode: any;
  flameData: any;
  dimension: string;
  unit: string;
  darkMode: boolean;
}

interface CallerInfo {
  name: string;
  value: number;
  percentage: number;
  depth: number;
}

interface CalleeInfo {
  name: string;
  value: number;
  percentage: number;
  depth: number;
}

const SandwichView: React.FC<SandwichViewProps> = ({
  selectedNode,
  flameData,
  dimension,
  unit,
  darkMode
}) => {
  if (!selectedNode || !flameData) {
    return (
      <Card 
        title="三明治视图" 
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
          <div>请选择一个函数节点查看调用关系</div>
        </div>
      </Card>
    );
  }

  // 计算选中节点的总权重
  const getTotalWeight = (node: any): number => {
    let total = node.value || 0;
    if (node.children) {
      for (const child of node.children) {
        total += getTotalWeight(child);
      }
    }
    return total;
  };

  const totalValue = flameData.value || 0;
  const nodeValue = selectedNode.value || 0;
  const nodePercentage = ((nodeValue / totalValue) * 100).toFixed(2);

  // 查找调用者（父节点）
  const findCallers = (node: any, data: any, path: string[] = []): CallerInfo[] => {
    const callers: CallerInfo[] = [];
    
    const searchInNode = (currentNode: any, currentPath: string[]) => {
      if (currentNode.children) {
        for (const child of currentNode.children) {
          if (child === node || child.name === node.name) {
            // 找到目标节点，记录路径
            const callerPath = [...currentPath];
            if (callerPath.length > 0) {
              const callerName = callerPath[callerPath.length - 1];
              const callerValue = getTotalWeight(currentNode);
              const callerPercentage = ((callerValue / totalValue) * 100).toFixed(2);
              
              callers.push({
                name: callerName,
                value: callerValue,
                percentage: parseFloat(callerPercentage),
                depth: callerPath.length
              });
            }
          } else {
            searchInNode(child, [...currentPath, currentNode.name]);
          }
        }
      }
    };
    
    searchInNode(data, []);
    return callers;
  };

  // 获取被调用者（子节点）
  const getCallees = (node: any): CalleeInfo[] => {
    if (!node.children || node.children.length === 0) {
      return [];
    }
    
    return node.children.map((child: any) => {
      const childValue = getTotalWeight(child);
      const childPercentage = ((childValue / totalValue) * 100).toFixed(2);
      
      return {
        name: child.name,
        value: childValue,
        percentage: parseFloat(childPercentage),
        depth: 1
      };
    }).sort((a: CalleeInfo, b: CalleeInfo) => b.percentage - a.percentage);
  };

  const callers = findCallers(selectedNode, flameData);
  const callees = getCallees(selectedNode);

  return (
    <Card 
      title={
        <Space>
          <FunctionOutlined />
          三明治视图
        </Space>
      } 
      size="small"
      style={{ 
        background: darkMode ? '#262626' : '#fff',
        borderColor: darkMode ? '#434343' : '#f0f0f0'
      }}
    >
      {/* 当前选中节点信息 */}
      <div style={{ marginBottom: 16 }}>
        <Title level={5} style={{ color: darkMode ? '#fff' : '#000', marginBottom: 8 }}>
          当前函数: {selectedNode.name}
        </Title>
        <div style={{ marginBottom: 8 }}>
          <Text style={{ color: darkMode ? '#ccc' : '#666' }}>
            耗时: {toReadableValue(unit, nodeValue)} ({nodePercentage}%)
          </Text>
        </div>
        <Progress
          percent={parseFloat(nodePercentage)}
          strokeColor="#1890ff"
          showInfo={false}
          size="small"
        />
      </div>

      <Divider style={{ margin: '12px 0' }} />

      {/* 调用者信息 */}
      <div style={{ marginBottom: 16 }}>
        <div style={{ 
          display: 'flex', 
          alignItems: 'center', 
          marginBottom: 8,
          color: darkMode ? '#fff' : '#000'
        }}>
          <ArrowUpOutlined style={{ marginRight: 4, color: '#52c41a' }} />
          <Text strong>调用者 ({callers.length})</Text>
        </div>
        
        {callers.length > 0 ? (
          <div>
            {callers.slice(0, 5).map((caller, index) => (
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
                    title={caller.name}
                  >
                    {caller.name}
                  </Text>
                  <Tag size="small" color="green">
                    {caller.percentage.toFixed(1)}%
                  </Tag>
                </div>
                <Progress
                  percent={caller.percentage}
                  strokeColor="#52c41a"
                  showInfo={false}
                  size="small"
                />
                <Text style={{ fontSize: 11, color: darkMode ? '#999' : '#999' }}>
                  {toReadableValue(unit, caller.value)}
                </Text>
              </div>
            ))}
            {callers.length > 5 && (
              <Text type="secondary" style={{ fontSize: 12 }}>
                ... 还有 {callers.length - 5} 个调用者
              </Text>
            )}
          </div>
        ) : (
          <Text style={{ color: darkMode ? '#999' : '#999', fontSize: 12 }}>
            无直接调用者信息
          </Text>
        )}
      </div>

      <Divider style={{ margin: '12px 0' }} />

      {/* 被调用者信息 */}
      <div>
        <div style={{ 
          display: 'flex', 
          alignItems: 'center', 
          marginBottom: 8,
          color: darkMode ? '#fff' : '#000'
        }}>
          <ArrowDownOutlined style={{ marginRight: 4, color: '#fa8c16' }} />
          <Text strong>被调用者 ({callees.length})</Text>
        </div>
        
        {callees.length > 0 ? (
          <div>
            {callees.slice(0, 5).map((callee, index) => (
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
                    title={callee.name}
                  >
                    {callee.name}
                  </Text>
                  <Tag size="small" color="orange">
                    {callee.percentage.toFixed(1)}%
                  </Tag>
                </div>
                <Progress
                  percent={callee.percentage}
                  strokeColor="#fa8c16"
                  showInfo={false}
                  size="small"
                />
                <Text style={{ fontSize: 11, color: darkMode ? '#999' : '#999' }}>
                  {toReadableValue(unit, callee.value)}
                </Text>
              </div>
            ))}
            {callees.length > 5 && (
              <Text type="secondary" style={{ fontSize: 12 }}>
                ... 还有 {callees.length - 5} 个被调用者
              </Text>
            )}
          </div>
        ) : (
          <Text style={{ color: darkMode ? '#999' : '#999', fontSize: 12 }}>
            无子函数调用
          </Text>
        )}
      </div>

      {/* 统计摘要 */}
      <Divider style={{ margin: '12px 0' }} />
      <div style={{ 
        background: darkMode ? '#1a1a1a' : '#f8f9fa',
        padding: 8,
        borderRadius: 4,
        fontSize: 12
      }}>
        <div style={{ 
          display: 'flex', 
          justifyContent: 'space-between',
          color: darkMode ? '#ccc' : '#666'
        }}>
          <span>总调用深度: {Math.max(...callers.map(c => c.depth), 0) + 1}</span>
          <span>子函数数: {callees.length}</span>
        </div>
        <div style={{ 
          marginTop: 4,
          color: darkMode ? '#999' : '#999'
        }}>
          此函数在调用链中处于第 {callers.length > 0 ? Math.max(...callers.map(c => c.depth)) + 1 : 1} 层
        </div>
      </div>
    </Card>
  );
};

export default SandwichView;
