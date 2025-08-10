// @ts-nocheck
// eslint-disable-next-line
import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Select, Input, Checkbox, Typography, Spin, message } from 'antd';
import { useParams } from 'react-router-dom';
import FlameGraph from '../../components/FlameGraph';
import { getSupportedDimensions, analyzeJFRFileById } from '../../services/jfrService';
import { formatFlamegraph } from '../../utils/formatFlamegraph';
import { useFileContext } from '../../stores/FileContext';

const { Title } = Typography;

const Analysis: React.FC = () => {
  const { fileId } = useParams();
  const { files, hasFiles } = useFileContext();
  const [selectedFileId, setSelectedFileId] = useState(fileId);
  const [dimension, setDimension] = useState('');
  const [dimensionOptions, setDimensionOptions] = useState([]);
  const [unit, setUnit] = useState('ns');
  const [taskSearch, setTaskSearch] = useState('');
  const [allTasks, setAllTasks] = useState([]);
  const [selectedTasks, setSelectedTasks] = useState([]);
  const [include, setInclude] = useState(true);
  const [flameData, setFlameData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [isZoomed, setIsZoomed] = useState(false);
  const flameGraphRef = React.useRef<any>(null);

  // 文件选择逻辑：优先使用 URL 参数，否则选择第一个文件
  useEffect(() => {
    if (files.length > 0) {
      // 如果 URL 中有文件ID，检查该文件是否存在
      if (fileId) {
        const file = files.find(f => f.id.toString() === fileId);
        if (file) {
          setSelectedFileId(fileId);
        } else {
          // 如果 URL 中的文件不存在，选择第一个文件
          setSelectedFileId(files[0].id.toString());
        }
      } else {
        // 如果 URL 中没有文件ID，选择第一个文件
        setSelectedFileId(files[0].id.toString());
      }
    } else {
      // 如果没有文件，清空选择
      setSelectedFileId(null);
    }
  }, [files, fileId]);

  // 加载维度元数据
  useEffect(() => {
    if (!selectedFileId) return;
    
    // 检查选中的文件是否存在
    const file = files.find(f => f.id.toString() === selectedFileId);
    if (!file) {
      message.error('文件不存在');
      return;
    }

    // 获取支持的分析维度
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
    
    // 直接通过文件ID分析JFR文件
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

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>JFR 分析详情</Title>
      <Row gutter={24}>
        <Col span={6}>
          <Card title="选择文件" variant="borderless" style={{ marginBottom: 16 }}>
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
          <Card title="分析维度" variant="borderless" style={{ marginBottom: 16 }}>
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
          <Card title="任务筛选" variant="borderless">
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
            <Checkbox.Group
              options={filteredTasks}
              value={selectedTasks}
              onChange={setSelectedTasks}
              style={{ display: 'block', maxHeight: 200, overflow: 'auto' }}
              disabled={!hasFiles || !selectedFileId}
            />
          </Card>
          <Card title="火焰图搜索" variant="borderless" style={{ marginTop: 16 }}>
            <Input.Search
              placeholder="方法/类名高亮"
              allowClear
              value={search}
              onChange={e => setSearch(e.target.value)}
              disabled={!hasFiles || !selectedFileId}
            />
          </Card>
        </Col>
        <Col span={18}>
          <Card 
            title={
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span>火焰图</span>
                {isZoomed && (
                  <button
                    onClick={() => {
                      flameGraphRef.current?.resetZoom();
                    }}
                    style={{
                      padding: '4px 8px',
                      background: '#1890ff',
                      color: 'white',
                      border: 'none',
                      borderRadius: 4,
                      cursor: 'pointer',
                      fontSize: 12
                    }}
                  >
                    返回
                  </button>
                )}
              </div>
            } 
            variant="borderless"
          >
            <Spin spinning={loading} tip="火焰图加载中...">
              <FlameGraph 
                ref={flameGraphRef}
                data={flameData} 
                search={search} 
                dimension={dimension}
                onZoomChange={(zoomNode) => setIsZoomed(!!zoomNode)}
              />
            </Spin>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Analysis; 