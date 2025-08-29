import React, { useState, useEffect, useRef, useCallback } from 'react';
import { 
  Card, 
  Row, 
  Col, 
  Select, 
  Input, 
  Checkbox, 
  Typography, 
  Spin, 
  message, 
  Button, 
  Space, 
  Divider,
  Tooltip,
  Switch,
  Badge,
  Tag,
  Drawer,
  Descriptions,
  Progress,
  Statistic,
  Collapse
} from 'antd';
import { 
  SearchOutlined, 
  DownloadOutlined, 
  FullscreenOutlined, 
  ReloadOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  InfoCircleOutlined,
  SettingOutlined,
  BarChartOutlined,
  ClockCircleOutlined,
  FunctionOutlined,
  DatabaseOutlined,
  EyeOutlined,
  EyeInvisibleOutlined
} from '@ant-design/icons';
import { useParams } from 'react-router-dom';
import FlameGraph from '../../components/FlameGraph';
import { getSupportedDimensions, analyzeJFRFileById } from '../../services/jfrService';
import { formatFlamegraph } from '../../utils/formatFlamegraph';
import { useFileContext } from '../../stores/FileContext';
import { toReadableValue } from '../../utils/format';
import SandwichView from '../../components/FlameGraph/SandwichView';
import FlameStats from '../../components/FlameGraph/FlameStats';
import TopTable from '../../components/FlameGraph/TopTable';
import ViewToggle, { ViewMode } from '../../components/FlameGraph/ViewToggle';

const { Title, Text } = Typography;
const { Panel } = Collapse;

// 指标类型配置
const METRIC_TYPES = [
  { key: 'CPU', label: 'CPU 时间', icon: <ClockCircleOutlined />, color: '#1890ff' },
  { key: 'Memory', label: '内存分配', icon: <DatabaseOutlined />, color: '#52c41a' },
  { key: 'Lock', label: '锁竞争', icon: <BarChartOutlined />, color: '#fa8c16' },
  { key: 'IO', label: 'I/O 操作', icon: <FunctionOutlined />, color: '#eb2f96' },
  { key: 'GC', label: '垃圾回收', icon: <DatabaseOutlined />, color: '#722ed1' }
];

const Analysis: React.FC = () => {
  const { fileId } = useParams();
  const { files, hasFiles } = useFileContext();
  const [selectedFileId, setSelectedFileId] = useState<string | undefined>(fileId);
  const [dimension, setDimension] = useState<string>('');
  const [dimensionOptions, setDimensionOptions] = useState<Array<{label: string; value: string; unit: string}>>([]);
  const [unit, setUnit] = useState<string>('ns');
  const [taskSearch, setTaskSearch] = useState<string>('');
  const [allTasks, setAllTasks] = useState<string[]>([]);
  const [selectedTasks, setSelectedTasks] = useState<string[]>([]);
  const [include, setInclude] = useState<boolean>(true);
  const [flameData, setFlameData] = useState<any>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [search, setSearch] = useState<string>('');
  const [isZoomed, setIsZoomed] = useState<boolean>(false);
  const [darkMode, setDarkMode] = useState<boolean>(false);
  const [selectedNode, setSelectedNode] = useState<any>(null);
  const [detailDrawerVisible, setDetailDrawerVisible] = useState<boolean>(false);
  const [sandwichView, setSandwichView] = useState<boolean>(false);
  const [flameGraphScale, setFlameGraphScale] = useState<number>(1);
  const [flameGraphOffset, setFlameGraphOffset] = useState<{x: number; y: number}>({ x: 0, y: 0 });
  const [currentView, setCurrentView] = useState<ViewMode>('both');
  const flameGraphRef = useRef<any>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // 文件选择逻辑
  useEffect(() => {
    if (files.length > 0) {
      if (fileId) {
        const file = files.find(f => f.id.toString() === fileId);
        if (file) {
          setSelectedFileId(fileId);
        } else {
          setSelectedFileId(files[0].id.toString());
        }
      } else {
        setSelectedFileId(files[0].id.toString());
      }
    } else {
      setSelectedFileId(undefined);
    }
  }, [files, fileId]);

  // 加载维度元数据
  useEffect(() => {
    if (!selectedFileId) return;
    
    const file = files.find(f => f.id.toString() === selectedFileId);
    if (!file) {
      message.error('文件不存在');
      return;
    }

    getSupportedDimensions().then(dimRes => {
      if (dimRes.code === 1 && dimRes.data) {
        const dims = dimRes.data.map(d => ({ 
          label: d, 
          value: d, 
          unit: d === 'CPU' ? 'ns' : 'bytes' 
        }));
        setDimensionOptions(dims);
        if (dims.length > 0) {
          setDimension(dims[0].value);
          setUnit(dims[0].unit);
        }
      }
    }).catch(e => message.error('获取分析维度失败: ' + e.message));
  }, [selectedFileId, files]);

  // 加载火焰图数据
  useEffect(() => {
    if (!selectedFileId || !dimension) return;
    setLoading(true);
    
    analyzeJFRFileById(
      selectedFileId,
      dimension,
      include,
      selectedTasks.length > 0 ? selectedTasks : undefined
    ).then(analysisRes => {
      if (analysisRes.code === 1 && analysisRes.data) {
        const { data, symbolTable, threadSplit } = analysisRes.data;
        if (data && symbolTable) {
          setFlameData(formatFlamegraph(data, symbolTable));
        } else {
          setFlameData(null);
        }
        if (threadSplit) {
          setAllTasks(Object.keys(threadSplit));
        }
      } else {
        setFlameData(null);
      }
      setLoading(false);
    }).catch(e => { 
      setLoading(false); 
      message.error('分析失败: ' + e.message); 
    });
  }, [selectedFileId, dimension, include, selectedTasks]);

  // 任务筛选
  const filteredTasks = allTasks.filter(t => t.toLowerCase().includes(taskSearch.toLowerCase()));

  const fileOptions = files.map(file => ({
    label: file.originalName,
    value: file.id.toString()
  }));

  // 获取当前指标类型配置
  const currentMetric = METRIC_TYPES.find(m => m.key === dimension) || METRIC_TYPES[0];

  // 处理节点选择
  const handleNodeSelect = useCallback((node: any) => {
    setSelectedNode(node);
    setDetailDrawerVisible(true);
  }, []);

  // 下载SVG
  const handleDownloadSVG = () => {
    if (containerRef.current) {
      const svg = containerRef.current.querySelector('svg');
      if (svg) {
        const svgData = new XMLSerializer().serializeToString(svg);
        const svgBlob = new Blob([svgData], { type: 'image/svg+xml;charset=utf-8' });
        const svgUrl = URL.createObjectURL(svgBlob);
        const downloadLink = document.createElement('a');
        downloadLink.href = svgUrl;
        downloadLink.download = `flamegraph-${dimension}-${Date.now()}.svg`;
        document.body.appendChild(downloadLink);
        downloadLink.click();
        document.body.removeChild(downloadLink);
        URL.revokeObjectURL(svgUrl);
        message.success('SVG下载成功');
      }
    }
  };

  // 全屏展示
  const handleFullscreen = () => {
    if (containerRef.current) {
      if (document.fullscreenElement) {
        document.exitFullscreen();
      } else {
        containerRef.current.requestFullscreen();
      }
    }
  };

  // 重置视图
  const handleResetView = () => {
    setFlameGraphScale(1);
    setFlameGraphOffset({ x: 0, y: 0 });
    flameGraphRef.current?.resetZoom();
    setSelectedNode(null);
    setDetailDrawerVisible(false);
  };

  // 缩放控制
  const handleZoomIn = () => {
    setFlameGraphScale(prev => Math.min(prev * 1.2, 3));
  };

  const handleZoomOut = () => {
    setFlameGraphScale(prev => Math.max(prev / 1.2, 0.5));
  };

  // 计算选中节点的统计信息
  const getNodeStats = (node: any) => {
    if (!node || !flameData) return null;
    
    const totalValue = flameData.value || 0;
    const percentage = ((node.value / totalValue) * 100).toFixed(2);
    
    return {
      percentage,
      value: node.value,
      totalValue,
      childrenCount: node.children?.length || 0,
      depth: getNodeDepth(node)
    };
  };

  // 获取节点深度
  const getNodeDepth = (node: any, depth = 0): number => {
    if (!node.children || node.children.length === 0) return depth;
    return Math.max(...node.children.map((c: any) => getNodeDepth(c, depth + 1)));
  };

  const nodeStats = getNodeStats(selectedNode);

  return (
    <div 
      ref={containerRef}
      style={{ 
        height: '100vh',
        background: darkMode ? '#141414' : '#f5f5f5',
        color: darkMode ? '#fff' : '#000',
        overflow: 'hidden'
      }}
    >
      {/* 顶部操作栏 */}
      <div style={{
        height: 64,
        background: darkMode ? '#1f1f1f' : '#fff',
        borderBottom: `1px solid ${darkMode ? '#303030' : '#f0f0f0'}`,
        padding: '0 24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <Title level={4} style={{ margin: 0, color: darkMode ? '#fff' : '#000' }}>
            JFR 性能分析
          </Title>
          <Badge 
            count={currentMetric.label} 
            style={{ backgroundColor: currentMetric.color }}
          />
        </div>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <ViewToggle
            currentView={currentView}
            onViewChange={setCurrentView}
            darkMode={darkMode}
          />
        </div>
        
        <Space>
          <Switch
            checkedChildren={<EyeOutlined />}
            unCheckedChildren={<EyeInvisibleOutlined />}
            checked={darkMode}
            onChange={setDarkMode}
          />
          <Button 
            icon={<ReloadOutlined />} 
            onClick={handleResetView}
            type="text"
          >
            重置
          </Button>
          <Button 
            icon={<ZoomOutOutlined />} 
            onClick={handleZoomOut}
            type="text"
            disabled={flameGraphScale <= 0.5}
          />
          <Button 
            icon={<ZoomInOutlined />} 
            onClick={handleZoomIn}
            type="text"
            disabled={flameGraphScale >= 3}
          />
          <Button 
            icon={<DownloadOutlined />} 
            onClick={handleDownloadSVG}
            type="text"
          >
            下载SVG
          </Button>
          <Button 
            icon={<FullscreenOutlined />} 
            onClick={handleFullscreen}
            type="text"
          >
            全屏
          </Button>
        </Space>
      </div>

      <div style={{ display: 'flex', height: 'calc(100vh - 64px)' }}>
        {/* 左侧边栏 */}
        <div style={{
          width: 320,
          background: darkMode ? '#1f1f1f' : '#fff',
          borderRight: `1px solid ${darkMode ? '#303030' : '#f0f0f0'}`,
          padding: 16,
          overflowY: 'auto'
        }}>
          {/* 文件选择 */}
          <Card 
            title="文件选择" 
            size="small"
            style={{ 
              marginBottom: 16,
              background: darkMode ? '#262626' : '#fff',
              borderColor: darkMode ? '#434343' : '#f0f0f0'
            }}
          >
            <Select
              style={{ width: '100%' }}
              options={fileOptions}
              value={selectedFileId}
              onChange={setSelectedFileId}
              placeholder="请选择要分析的文件"
              disabled={!hasFiles}
            />
            {!hasFiles && (
              <div style={{ marginTop: 8, color: '#999', fontSize: 12 }}>
                暂无文件，请先上传JFR文件
              </div>
            )}
          </Card>

          {/* 分析维度 */}
          <Card 
            title="分析维度" 
            size="small"
            style={{ 
              marginBottom: 16,
              background: darkMode ? '#262626' : '#fff',
              borderColor: darkMode ? '#434343' : '#f0f0f0'
            }}
          >
            <Select
              style={{ width: '100%' }}
              options={dimensionOptions}
              value={dimension}
              onChange={v => {
                setDimension(v);
                const found = dimensionOptions.find(d => d.value === v);
                setUnit(found?.unit || 'ns');
              }}
              disabled={!hasFiles || !selectedFileId}
            />
          </Card>

          {/* 任务筛选 */}
          <Card 
            title="任务筛选" 
            size="small"
            style={{ 
              marginBottom: 16,
              background: darkMode ? '#262626' : '#fff',
              borderColor: darkMode ? '#434343' : '#f0f0f0'
            }}
          >
            <Input.Search
              placeholder="搜索任务名"
              allowClear
              value={taskSearch}
              onChange={e => setTaskSearch(e.target.value)}
              style={{ marginBottom: 8 }}
              disabled={!hasFiles || !selectedFileId}
            />
            <Checkbox
              checked={include}
              onChange={e => setInclude(e.target.checked)}
              style={{ marginBottom: 8 }}
              disabled={!hasFiles || !selectedFileId}
            >
              包含（Include）
            </Checkbox>
            <div style={{ maxHeight: 200, overflow: 'auto' }}>
              <Checkbox.Group
                options={filteredTasks}
                value={selectedTasks}
                onChange={setSelectedTasks}
                style={{ display: 'block' }}
                disabled={!hasFiles || !selectedFileId}
              />
            </div>
          </Card>

          {/* 火焰图搜索 */}
          <Card 
            title="方法搜索" 
            size="small"
            style={{ 
              marginBottom: 16,
              background: darkMode ? '#262626' : '#fff',
              borderColor: darkMode ? '#434343' : '#f0f0f0'
            }}
          >
            <Input.Search
              placeholder="方法/类名高亮"
              allowClear
              value={search}
              onChange={e => setSearch(e.target.value)}
              disabled={!hasFiles || !selectedFileId}
              prefix={<SearchOutlined />}
            />
          </Card>

          {/* 视图控制 */}
          <Card 
            title="视图控制" 
            size="small"
            style={{ 
              background: darkMode ? '#262626' : '#fff',
              borderColor: darkMode ? '#434343' : '#f0f0f0'
            }}
          >
            <Space direction="vertical" style={{ width: '100%' }}>
              <div>
                <Text style={{ color: darkMode ? '#d9d9d9' : '#666' }}>缩放: {Math.round(flameGraphScale * 100)}%</Text>
              </div>
              <div>
                <Text style={{ color: darkMode ? '#d9d9d9' : '#666' }}>三明治视图</Text>
                <Switch
                  checked={sandwichView}
                  onChange={setSandwichView}
                  size="small"
                  style={{ marginLeft: 8 }}
                />
              </div>
            </Space>
          </Card>

          {/* 三明治视图 */}
          {sandwichView && selectedNode && (
            <div style={{ marginTop: 16 }}>
              <SandwichView
                selectedNode={selectedNode}
                flameData={flameData}
                dimension={dimension}
                unit={unit}
                darkMode={darkMode}
              />
            </div>
          )}

          {/* 统计信息 */}
          <div style={{ marginTop: 16 }}>
            <FlameStats
              flameData={flameData}
              dimension={dimension}
              unit={unit}
              darkMode={darkMode}
              selectedNode={selectedNode}
            />
          </div>
        </div>

        {/* 主内容区 */}
        <div style={{ flex: 1, padding: 16, overflow: 'hidden' }}>
          {currentView === 'table' && (
            <Card 
              title="函数性能排行"
              style={{ 
                height: '100%',
                background: darkMode ? '#262626' : '#fff',
                borderColor: darkMode ? '#434343' : '#f0f0f0'
              }}
              bodyStyle={{ 
                height: 'calc(100% - 57px)', 
                padding: 0,
                overflow: 'hidden'
              }}
            >
              <TopTable
                flameData={flameData}
                dimension={dimension}
                unit={unit}
                darkMode={darkMode}
                onNodeSelect={handleNodeSelect}
              />
            </Card>
          )}
          
          {currentView === 'flame' && (
            <Card 
              title={
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span>火焰图分析</span>
                  {isZoomed && (
                    <Tag color="blue">已缩放</Tag>
                  )}
                </div>
              }
              style={{ 
                height: '100%',
                background: darkMode ? '#262626' : '#fff',
                borderColor: darkMode ? '#434343' : '#f0f0f0'
              }}
              bodyStyle={{ 
                height: 'calc(100% - 57px)', 
                padding: 0,
                overflow: 'hidden'
              }}
            >
              <Spin spinning={loading} tip="火焰图加载中...">
                <div style={{ 
                  height: '100%',
                  transform: `scale(${flameGraphScale}) translate(${flameGraphOffset.x}px, ${flameGraphOffset.y}px)`,
                  transformOrigin: 'top left',
                  transition: 'transform 0.2s ease'
                }}>
                  <FlameGraph 
                    ref={flameGraphRef}
                    data={flameData} 
                    search={search} 
                    dimension={dimension}
                    onZoomChange={(zoomNode) => setIsZoomed(!!zoomNode)}
                    onNodeSelect={handleNodeSelect}
                    darkMode={darkMode}
                    sandwichView={sandwichView}
                  />
                </div>
              </Spin>
            </Card>
          )}
          
          {currentView === 'both' && (
            <div style={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
              {/* Top Table 区域 */}
              <div style={{ height: '40%' }}>
                <TopTable
                  flameData={flameData}
                  dimension={dimension}
                  unit={unit}
                  darkMode={darkMode}
                  onNodeSelect={handleNodeSelect}
                />
              </div>
              
              {/* 火焰图区域 */}
              <div style={{ height: '60%' }}>
                <Card 
                  title={
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <span>火焰图分析</span>
                      {isZoomed && (
                        <Tag color="blue">已缩放</Tag>
                      )}
                    </div>
                  }
                  style={{ 
                    height: '100%',
                    background: darkMode ? '#262626' : '#fff',
                    borderColor: darkMode ? '#434343' : '#f0f0f0'
                  }}
                  bodyStyle={{ 
                    height: 'calc(100% - 57px)', 
                    padding: 0,
                    overflow: 'hidden'
                  }}
                >
                  <Spin spinning={loading} tip="火焰图加载中...">
                    <div style={{ 
                      height: '100%',
                      transform: `scale(${flameGraphScale}) translate(${flameGraphOffset.x}px, ${flameGraphOffset.y}px)`,
                      transformOrigin: 'top left',
                      transition: 'transform 0.2s ease'
                    }}>
                      <FlameGraph 
                        ref={flameGraphRef}
                        data={flameData} 
                        search={search} 
                        dimension={dimension}
                        onZoomChange={(zoomNode) => setIsZoomed(!!zoomNode)}
                        onNodeSelect={handleNodeSelect}
                        darkMode={darkMode}
                        sandwichView={sandwichView}
                      />
                    </div>
                  </Spin>
                </Card>
              </div>
            </div>
          )}
        </div>

        {/* 右侧详情面板 */}
        {selectedNode && (
          <div style={{
            width: 360,
            background: darkMode ? '#1f1f1f' : '#fff',
            borderLeft: `1px solid ${darkMode ? '#303030' : '#f0f0f0'}`,
            padding: 16,
            overflowY: 'auto'
          }}>
            <div style={{ marginBottom: 16 }}>
              <Button 
                icon={<InfoCircleOutlined />}
                type="text"
                onClick={() => setDetailDrawerVisible(true)}
                style={{ width: '100%' }}
              >
                查看完整详情
              </Button>
            </div>

            {/* 节点概览 */}
            <Card 
              title="节点概览" 
              size="small"
              style={{ 
                marginBottom: 16,
                background: darkMode ? '#262626' : '#fff',
                borderColor: darkMode ? '#434343' : '#f0f0f0'
              }}
            >
              <div style={{ marginBottom: 16 }}>
                <Text strong style={{ color: darkMode ? '#fff' : '#000' }}>
                  {selectedNode.name}
                </Text>
              </div>
              
              {nodeStats && (
                <>
                  <Statistic
                    title="耗时占比"
                    value={nodeStats.percentage}
                    suffix="%"
                    valueStyle={{ color: currentMetric.color }}
                  />
                  <Progress
                    percent={parseFloat(nodeStats.percentage)}
                    strokeColor={currentMetric.color}
                    showInfo={false}
                    style={{ marginBottom: 16 }}
                  />
                  
                  <Descriptions size="small" column={1}>
                    <Descriptions.Item label="耗时值">
                      {toReadableValue(unit, nodeStats.value)}
                    </Descriptions.Item>
                    <Descriptions.Item label="子节点数">
                      {nodeStats.childrenCount}
                    </Descriptions.Item>
                    <Descriptions.Item label="调用深度">
                      {nodeStats.depth}
                    </Descriptions.Item>
                  </Descriptions>
                </>
              )}
            </Card>

            {/* 快速操作 */}
            <Card 
              title="快速操作" 
              size="small"
              style={{ 
                background: darkMode ? '#262626' : '#fff',
                borderColor: darkMode ? '#434343' : '#f0f0f0'
              }}
            >
              <Space direction="vertical" style={{ width: '100%' }}>
                <Button 
                  size="small" 
                  block
                  onClick={() => {
                    if (flameGraphRef.current) {
                      flameGraphRef.current.zoomToNode(selectedNode);
                    }
                  }}
                >
                  聚焦此节点
                </Button>
                <Button 
                  size="small" 
                  block
                  onClick={() => {
                    setSearch(selectedNode.name);
                  }}
                >
                  搜索相似方法
                </Button>
              </Space>
            </Card>
          </div>
        )}
      </div>

      {/* 详情抽屉 */}
      <Drawer
        title="函数详情"
        placement="right"
        width={600}
        open={detailDrawerVisible}
        onClose={() => setDetailDrawerVisible(false)}
        styles={{
          body: { background: darkMode ? '#141414' : '#fff' },
          header: { background: darkMode ? '#1f1f1f' : '#fff' }
        }}
      >
        {selectedNode && (
          <div>
            <Collapse defaultActiveKey={['1', '2', '3']}>
              <Panel header="基本信息" key="1">
                <Descriptions column={1}>
                  <Descriptions.Item label="函数名">
                    <Text code>{selectedNode.name}</Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="耗时值">
                    <Text strong>{toReadableValue(unit, selectedNode.value)}</Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="耗时占比">
                    <Text strong style={{ color: currentMetric.color }}>
                      {nodeStats?.percentage}%
                    </Text>
                  </Descriptions.Item>
                </Descriptions>
              </Panel>
              
              <Panel header="调用链信息" key="2">
                <div>
                  <Text>调用深度: {nodeStats?.depth}</Text>
                  <br />
                  <Text>子节点数: {nodeStats?.childrenCount}</Text>
                  {selectedNode.children && selectedNode.children.length > 0 && (
                    <div style={{ marginTop: 8 }}>
                      <Text strong>子调用:</Text>
                      <div style={{ marginTop: 4 }}>
                        {selectedNode.children.slice(0, 5).map((child: any, index: number) => (
                          <Tag key={index} style={{ marginBottom: 4 }}>
                            {child.name} ({toReadableValue(unit, child.value)})
                          </Tag>
                        ))}
                        {selectedNode.children.length > 5 && (
                          <Text type="secondary">... 还有 {selectedNode.children.length - 5} 个</Text>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              </Panel>
              
              <Panel header="性能分析" key="3">
                <div>
                  <Text>此函数在分析周期内被调用，消耗了 {nodeStats?.percentage}% 的 {dimension} 资源。</Text>
                  <br />
                  <Text>建议关注:</Text>
                  <ul>
                    <li>是否存在性能瓶颈</li>
                    <li>调用频率是否合理</li>
                    <li>是否有优化空间</li>
                  </ul>
                </div>
              </Panel>
            </Collapse>
          </div>
        )}
      </Drawer>
    </div>
  );
};

export default Analysis;
