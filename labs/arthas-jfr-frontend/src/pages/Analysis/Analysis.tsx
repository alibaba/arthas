import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { 
  Card, 
  Select, 
  Input, 
  Typography, 
  Spin, 
  message, 
  Button, 
  Space, 
  Tooltip,
  Switch,
  Badge,
  Tag,
  Collapse,
  Layout,
  Menu,
  Table,
  Checkbox,
  Progress
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
  EyeInvisibleOutlined,
  AimOutlined,
  CompressOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  FileTextOutlined,
  BarChartOutlined as BarChartIcon,
  FilterOutlined,
  UserOutlined,
  CodeOutlined,
  FunctionOutlined as MethodOutlined
} from '@ant-design/icons';
import { useParams } from 'react-router-dom';
import ReactFlameGraphWrapper from '../../components/FlameGraph/ReactFlameGraphWrapper';
import { getMetadata, analyzeJFRFileById } from '../../services/jfrService';
import { formatFlamegraph } from '../../utils/formatFlamegraph';
import { useFileContext } from '../../stores/FileContext';
import { toReadableValue, formatByteValue } from '../../utils/format';
import FlameStats from '../../components/FlameGraph/FlameStats';

// 简单的防抖函数实现
const debounce = (func: Function, wait: number) => {
  let timeout: number;
  return function executedFunction(...args: any[]) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = window.setTimeout(later, wait);
  };
};

const { Title, Text } = Typography;
const { Panel } = Collapse;
const { Sider: AntdSider, Content } = Layout;

const Analysis: React.FC = () => {
  const { fileId } = useParams();
  const { files, hasFiles } = useFileContext();
  const [selectedFileId, setSelectedFileId] = useState<string | undefined>(fileId);
  const [dimension, setDimension] = useState<string>('');
  const [dimensionOptions, setDimensionOptions] = useState<Array<{label: string; value: string; unit: string}>>([]);
  const [unit, setUnit] = useState<string>('ns');
  const [flameData, setFlameData] = useState<any>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [search, setSearch] = useState<string>('');
  const [isZoomed, setIsZoomed] = useState<boolean>(false);
  const [darkMode, setDarkMode] = useState<boolean>(false);
  const [flameGraphScale, setFlameGraphScale] = useState<number>(1);
  const [flameGraphOffset, setFlameGraphOffset] = useState<{x: number; y: number}>({ x: 0, y: 0 });
  const [isFocused, setIsFocused] = useState<boolean>(false);
  const [focusedNode, setFocusedNode] = useState<any>(null);
  const [sidebarCollapsed, setSidebarCollapsed] = useState<boolean>(false);
  
  // 筛选相关状态
  const [selectedFilterIndex, setSelectedFilterIndex] = useState<number>(0);
  const [filterValues, setFilterValues] = useState<Array<{key: string; weight: number; checked: boolean}>>([]);
  const [filterValuesMap, setFilterValuesMap] = useState<Record<string, {key: string; weight: number; checked: boolean}>>({});
  const [totalWeight, setTotalWeight] = useState<number>(0);
  const [toggleFilterValuesChecked, setToggleFilterValuesChecked] = useState<boolean>(true);
  const [threadSplit, setThreadSplit] = useState<Record<string, number> | null>(null);
  const [symbolTable, setSymbolTable] = useState<Record<string, string>>({});
  const [flameGraphDataSource, setFlameGraphDataSource] = useState<any>(null);
  const [hasData, setHasData] = useState<boolean>(false);
  
  // 保存完整的threadSplit，用于始终显示所有线程
  const [fullThreadSplit, setFullThreadSplit] = useState<Record<string, number> | null>(null);
  
  // 筛选相关新增状态
  const [filterSearchText, setFilterSearchText] = useState<string>('');
  const [filterSortBy, setFilterSortBy] = useState<'weight' | 'name'>('weight');
  const [filterSortOrder, setFilterSortOrder] = useState<'asc' | 'desc'>('desc');
  const [isFiltering, setIsFiltering] = useState<boolean>(false);
  
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
      return;
    }

    getMetadata().then(metadataRes => {
      if (metadataRes.code === 1 && metadataRes.data && metadataRes.data.perfDimensions) {
        const dims = metadataRes.data.perfDimensions.map(d => ({
          label: d.desc?.key || d.key,
          value: d.key,
          unit: d.unit || 'ns'
        }));
        setDimensionOptions(dims);
        if (dims.length > 0) {
          setDimension(dims[0].value);
          setUnit(dims[0].unit);
        }
      }
    }).catch(e => message.error('获取分析元数据失败: ' + e.message));
  }, [selectedFileId, files]);

  // 加载火焰图数据
  useEffect(() => {
    if (!selectedFileId || !dimension) return;
    setLoading(true);
    
    analyzeJFRFileById(selectedFileId, dimension).then(analysisRes => {
      if (analysisRes.code === 1 && analysisRes.data) {
        const { data, symbolTable, threadSplit } = analysisRes.data;
        if (data && symbolTable) {
          setFlameData(formatFlamegraph(data, symbolTable));
          setSymbolTable(symbolTable);
          setThreadSplit(threadSplit || {});
          setFlameGraphDataSource(data);
          setHasData(data.length > 0);
          
          const filterTypes = ['Thread', 'Class', 'Method'];
          const filterName = filterTypes[selectedFilterIndex] || 'Thread';
          
          if (filterName === 'Thread') {
            buildFilterValueByThreads(threadSplit || {});
          } else if (filterName === 'Class') {
            buildFilterValueByClass();
          } else if (filterName === 'Method') {
            buildFilterValueByMethod();
          }
        } else {
          setFlameData(null);
          setHasData(false);
        }
      } else {
        setFlameData(null);
        setHasData(false);
      }
      setLoading(false);
    }).catch(e => { 
      setLoading(false); 
      message.error('分析失败: ' + e.message); 
    });
  }, [selectedFileId, dimension, selectedFilterIndex]);

  // 当筛选索引改变时重建过滤值
  useEffect(() => {
    if (selectedFilterIndex !== null && dimensionOptions.length > 0) {
      setToggleFilterValuesChecked(true);
      const filterTypes = ['Thread', 'Class', 'Method'];
      const filterName = filterTypes[selectedFilterIndex] || 'Thread';
      
      if (filterName === 'Thread') {
        buildFilterValueByThreads(fullThreadSplit || threadSplit || {});
      } else if (filterName === 'Class') {
        buildFilterValueByClass();
      } else if (filterName === 'Method') {
        buildFilterValueByMethod();
      }
    }
  }, [selectedFilterIndex, threadSplit, symbolTable, flameGraphDataSource]);

  const fileOptions = files.map(file => ({
    label: file.originalName,
    value: file.id.toString()
  }));


  // 处理火焰图缩放变化
  const handleZoomChange = useCallback((zoomNode: any) => {
    setIsZoomed(!!zoomNode);
    setFocusedNode(zoomNode);
    setIsFocused(!!zoomNode);
  }, []);

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

  // 判断是否为时间维度
  const isTimeDim = unit === 'ns' || unit === 'μs' || unit === 'ms' || unit === 's';

  // 根据线程构建过滤值 - 保留选中状态
  const buildFilterValueByThreads = (
    threadSplitData: Record<string, number>,
    prevFilterValues?: Array<{key: string; weight: number; checked: boolean}>
  ) => {
    const fv: Array<{key: string; weight: number; checked: boolean}> = [];
    const map: Record<string, {key: string; weight: number; checked: boolean}> = {};
    let index = 0;
    let total = 0;
    
    for (const key in threadSplitData) {
      const threadValue = threadSplitData[key];
      total += threadValue;
      let v = map[key];
      
      if (v) {
        v.weight += threadValue;
      } else {
        // 优先使用之前的选中状态，否则默认选中
        const prevChecked = prevFilterValues 
          ? prevFilterValues.find(p => p.key === key)?.checked ?? true 
          : true;
        
        v = {
          key,
          weight: threadValue,
          checked: prevChecked
        };
        map[key] = v;
        fv[index++] = v;
      }
    }

    fv.sort((i, j) => j.weight - i.weight);
    
    setFullThreadSplit(threadSplitData);
    setFilterValuesMap(map);
    setFilterValues(fv);
    setTotalWeight(total);
  };

  // 根据类构建过滤值
  const buildFilterValueByClass = () => {
    buildFilterValue((d: any) => {
      const v = symbolTable[d[0][d[0].length - 1]];
      if (v) {
        const index = v.lastIndexOf('.');
        if (index >= 0) {
          return v.substring(0, index);
        }
        return v;
      } else {
        return 'undefined';
      }
    });
  };

  // 根据方法构建过滤值
  const buildFilterValueByMethod = () => {
    buildFilterValue((d: any) => symbolTable[d[0][d[0].length - 1]]);
  };

  // 通用构建过滤值函数
  const buildFilterValue = (keyExtractor: (d: any) => string) => {
    const fv: Array<{key: string; weight: number; checked: boolean}> = [];
    const map: Record<string, {key: string; weight: number; checked: boolean}> = {};
    let index = 0;
    let total = 0;
    const dataSource = flameGraphDataSource;

    if (!dataSource) {
      return;
    }

    for (let i = 0; i < dataSource.length; i++) {
      total += dataSource[i][1];
      const key = keyExtractor(dataSource[i]);
      let v = map[key];
      if (v) {
        v.weight += dataSource[i][1];
      } else {
        v = {
          key,
          weight: dataSource[i][1],
          checked: true
        };
        map[key] = v;
        fv[index++] = v;
      }
    }

    fv.sort((i, j) => j.weight - i.weight);
    setFilterValuesMap(map);
    setFilterValues(fv);
    setTotalWeight(total);

    const filter = (d: any, s: any) => {
      const key = keyExtractor(s);
      return map[key] ? map[key].checked : false;
    };

    if (flameGraphRef.current) {
      flameGraphRef.current.configuration = {
        ...flameGraphRef.current.configuration,
        stackTraceFilter: filter
      };
      flameGraphRef.current.dispatchEvent(new CustomEvent('re-render'));
    }
  };


  // 处理过滤值的选中状态
  const handleFilterValuesChecked = async (checked: boolean, index: number, updatedFilterValues?: Array<{key: string; weight: number; checked: boolean}>) => {
    setIsFiltering(true);
    const filterTypes = ['Thread', 'Class', 'Method'];
    const filterName = filterTypes[selectedFilterIndex] || 'Thread';
    
    try {
      if (filterName === 'Thread') {
        const currentFilterValues = updatedFilterValues || filterValues;
        // 收集所有"未选中"和"选中"的线程
        const unCheckedThreads = currentFilterValues.filter(v => !v.checked).map(v => v.key);
        const checkedThreads = currentFilterValues.filter(v => v.checked).map(v => v.key);
        
        // 基于实际选中数量判断模式
        const isAllChecked = currentFilterValues.every(v => v.checked);
        const include = !isAllChecked; // 全选→排除模式，非全选→包含模式
        const taskSet = isAllChecked ? unCheckedThreads : checkedThreads;
        
        clearFlameGraph();
        
        // 非全选模式下无选中线程则清空
        if (!isAllChecked && checkedThreads.length === 0) {
          setHasData(false);
          setIsFiltering(false);
          return;
        }
        
        if (selectedFileId && dimension) {
          setLoading(true);
          try {
            const analysisRes = await analyzeJFRFileById(selectedFileId, dimension, include, taskSet);
            if (analysisRes.code === 1 && analysisRes.data) {
              const { data, symbolTable, threadSplit } = analysisRes.data;
              if (data && symbolTable) {
                setFlameData(formatFlamegraph(data, symbolTable));
                setSymbolTable(symbolTable);
                setFlameGraphDataSource(data);
                setHasData(data.length > 0);
                
                // 保留原选中状态
                buildFilterValueByThreads(threadSplit || {}, currentFilterValues);
                
                if (flameGraphRef.current) {
                  flameGraphRef.current.dataSource = {
                    format: 'line',
                    data: data
                  };
                }
              }
            }
          } catch (e) {
            message.error('筛选失败: ' + (e as Error).message);
            // 异常时恢复原筛选状态
            setFilterValues(currentFilterValues);
          } finally {
            setLoading(false);
          }
        }
      } else {
        const anyChecked = filterValues.some(v => v.checked);
        setHasData(anyChecked);
        
        if (anyChecked) {
          restoreFlameGraph();
          refreshFlameGraph();
        } else {
          clearFlameGraph();
        }
        
        if (filterName === 'Class') {
          updateStackTraceFilterForClass();
        } else if (filterName === 'Method') {
          updateStackTraceFilterForMethod();
        }
      }
    } finally {
      setTimeout(() => {
        setIsFiltering(false);
      }, 500);
    }
  };

  // 处理全选/全不选
  const handleToggleFilterValuesChecked = async (checked: boolean) => {
    setToggleFilterValuesChecked(checked);
    
    const filterTypes = ['Thread', 'Class', 'Method'];
    const filterName = filterTypes[selectedFilterIndex] || 'Thread';
    
    // 更新所有过滤值的检查状态
    const updatedFilterValues = filterValues.map(v => ({ ...v, checked }));
    setFilterValues(updatedFilterValues);
    
    if (filterName === 'Thread') {
      // 基于实际选中状态构建任务集合
      const isAllChecked = checked;
      const include = !isAllChecked;
      const taskSet = isAllChecked 
        ? updatedFilterValues.filter(v => !v.checked).map(v => v.key) 
        : updatedFilterValues.filter(v => v.checked).map(v => v.key);
      
      clearFlameGraph();
      
      if (!isAllChecked && taskSet.length === 0) {
        setHasData(false);
        setIsFiltering(false);
        return;
      }
      
      if (selectedFileId && dimension) {
        setLoading(true);
        try {
          const analysisRes = await analyzeJFRFileById(selectedFileId, dimension, include, taskSet);
          if (analysisRes.code === 1 && analysisRes.data) {
            const { data, symbolTable, threadSplit } = analysisRes.data;
            if (data && symbolTable) {
              setFlameData(formatFlamegraph(data, symbolTable));
              setSymbolTable(symbolTable);
              setFlameGraphDataSource(data);
              setHasData(data.length > 0);
              
              // 保留全选/取消全选的状态
              buildFilterValueByThreads(threadSplit || {}, updatedFilterValues);
              
              if (flameGraphRef.current) {
                flameGraphRef.current.dataSource = {
                  format: 'line',
                  data: data
                };
              }
            }
          }
        } catch (e) {
          message.error('筛选失败: ' + (e as Error).message);
          setFilterValues(updatedFilterValues);
        } finally {
          setLoading(false);
        }
      }
    } else {
      if (checked) {
        restoreFlameGraph();
        setHasData(true);
        refreshFlameGraph();
      } else {
        clearFlameGraph();
        setHasData(false);
      }
      
      if (filterName === 'Class') {
        updateStackTraceFilterForClass();
      } else if (filterName === 'Method') {
        updateStackTraceFilterForMethod();
      }
    }
  };

  // 计算选中的过滤值数量
  const checkedCount = useMemo(() => {
    return filterValues.filter(v => v.checked).length;
  }, [filterValues]);

  // 筛选和排序过滤值
  const filteredAndSortedValues = useMemo(() => {
    let filtered = filterValues;
    
    if (filterSearchText.trim()) {
      const searchLower = filterSearchText.toLowerCase();
      filtered = filtered.filter(item => 
        item.key.toLowerCase().includes(searchLower)
      );
    }
    
    filtered = [...filtered].sort((a, b) => {
      let comparison = 0;
      if (filterSortBy === 'weight') {
        comparison = a.weight - b.weight;
      } else {
        comparison = a.key.localeCompare(b.key);
      }
      return filterSortOrder === 'desc' ? -comparison : comparison;
    });
    
    return filtered;
  }, [filterValues, filterSearchText, filterSortBy, filterSortOrder]);

  // 重置筛选状态
  const resetFilterState = useCallback(() => {
    setFilterSearchText('');
    setFilterSortBy('weight');
    setFilterSortOrder('desc');
    setIsFiltering(false);
  }, []);

  // 处理筛选搜索
  const handleFilterSearch = useCallback((value: string) => {
    setFilterSearchText(value);
  }, []);

  // 防抖搜索
  const debouncedSearch = useCallback(
    debounce((value: string) => {
      setFilterSearchText(value);
    }, 300),
    []
  );

  // 处理排序变化
  const handleSortChange = useCallback((sortBy: 'weight' | 'name') => {
    if (filterSortBy === sortBy) {
      setFilterSortOrder(prev => prev === 'asc' ? 'desc' : 'asc');
    } else {
      setFilterSortBy(sortBy);
      setFilterSortOrder('desc');
    }
  }, [filterSortBy]);

  // 清空火焰图
  const clearFlameGraph = useCallback(() => {
    if (flameGraphRef.current) {
      flameGraphRef.current.dataSource = { format: 'line', data: [] };
    }
  }, []);

  // 恢复火焰图数据源
  const restoreFlameGraph = useCallback(() => {
    if (flameGraphRef.current && flameGraphDataSource) {
      flameGraphRef.current.dataSource = {
        format: 'line',
        data: flameGraphDataSource
      };
    }
  }, [flameGraphDataSource]);

  // 刷新火焰图
  const refreshFlameGraph = useCallback(() => {
    if (flameGraphRef.current) {
      flameGraphRef.current.dispatchEvent(new CustomEvent('re-render'));
    }
  }, []);

  // 更新Class筛选的stackTraceFilter
  const updateStackTraceFilterForClass = useCallback(() => {
    if (!flameGraphRef.current || !flameGraphDataSource) return;
    
    const filter = (d: any, s: any) => {
      const v = symbolTable[s[0][s[0].length - 1]];
      let key = 'undefined';
      if (v) {
        const index = v.lastIndexOf('.');
        if (index >= 0) {
          key = v.substring(0, index);
        } else {
          key = v;
        }
      }
      return filterValuesMap[key] ? filterValuesMap[key].checked : false;
    };

    flameGraphRef.current.configuration = {
      ...flameGraphRef.current.configuration,
      stackTraceFilter: filter
    };
    flameGraphRef.current.dispatchEvent(new CustomEvent('re-render'));
  }, [symbolTable, filterValuesMap]);

  // 更新Method筛选的stackTraceFilter
  const updateStackTraceFilterForMethod = useCallback(() => {
    if (!flameGraphRef.current || !flameGraphDataSource) return;
    
    const filter = (d: any, s: any) => {
      const key = symbolTable[s[0][s[0].length - 1]] || 'undefined';
      return filterValuesMap[key] ? filterValuesMap[key].checked : false;
    };

    flameGraphRef.current.configuration = {
      ...flameGraphRef.current.configuration,
      stackTraceFilter: filter
    };
    flameGraphRef.current.dispatchEvent(new CustomEvent('re-render'));
  }, [symbolTable, filterValuesMap]);

  return (
    <Layout 
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
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        zIndex: 1000
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <Title level={4} style={{ margin: 0, color: darkMode ? '#fff' : '#000' }}>
            JFR 性能分析
          </Title>
        </div>
        
        
        <Space>
          <Switch
            checkedChildren={<EyeOutlined />}
            unCheckedChildren={<EyeInvisibleOutlined />}
            checked={darkMode}
            onChange={setDarkMode}
          />
          <Button 
            icon={<FullscreenOutlined />} 
            onClick={handleFullscreen}
            type="text"
          >
            全屏
          </Button>
        </Space>
      </div>

      <Layout style={{ height: 'calc(100vh - 64px)', display: 'flex' }}>
        {/* 左侧控制面板 */}
        <AntdSider
          width={280}
          collapsed={sidebarCollapsed}
          collapsedWidth={80}
          theme={darkMode ? 'dark' : 'light'}
          style={{
            background: darkMode ? '#1f1f1f' : '#fff',
            borderRight: `1px solid ${darkMode ? '#303030' : '#f0f0f0'}`,
            overflow: 'hidden',
            flexShrink: 0
          }}
          trigger={null}
        >
          {/* 折叠控制按钮 */}
          <div style={{
            height: 48,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderBottom: `1px solid ${darkMode ? '#303030' : '#f0f0f0'}`,
            background: darkMode ? '#262626' : '#fafafa'
          }}>
            <Button
              type="text"
              icon={sidebarCollapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
              style={{
                color: darkMode ? '#d9d9d9' : '#666',
                border: 'none'
              }}
            />
          </div>

          {/* 控制面板内容 */}
          <div style={{
            padding: sidebarCollapsed ? 8 : 16,
            overflowY: 'auto',
            height: 'calc(100% - 48px)'
          }}>
            {!sidebarCollapsed ? (
              <>
                {/* 文件选择 */}
                <Card 
                  title={
                    <span>
                      <FileTextOutlined style={{ marginRight: 8 }} />
                      文件选择
                    </span>
                  }
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
                  title={
                    <span>
                      <BarChartIcon style={{ marginRight: 8 }} />
                      分析维度
                    </span>
                  }
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
                      if (found) {
                        setUnit(found.unit);
                      }
                    }}
                    disabled={!hasFiles || !selectedFileId}
                  />
                </Card>

                {/* 统计信息 */}
                <div>
                  <FlameStats
                    flameData={flameData}
                    dimension={dimension}
                    unit={unit}
                    darkMode={darkMode}
                  />
                </div>
              </>
            ) : (
              // 折叠状态下的图标菜单
              <div style={{ padding: '8px 0' }}>
                <Menu
                  mode="inline"
                  theme={darkMode ? 'dark' : 'light'}
                  selectedKeys={[]}
                  style={{
                    background: 'transparent',
                    border: 'none'
                  }}
                  items={[
                    {
                      key: 'file',
                      icon: <FileTextOutlined />,
                      title: '文件选择'
                    },
                    {
                      key: 'dimension',
                      icon: <BarChartIcon />,
                      title: '分析维度'
                    },
                    {
                      key: 'settings',
                      icon: <SettingOutlined />,
                      title: '视图控制'
                    }
                  ]}
                />
              </div>
            )}
          </div>
        </AntdSider>

        {/* 主内容区域 - 火焰图 */}
        <Content style={{ 
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          background: darkMode ? '#141414' : '#f5f5f5',
          minHeight: 0
        }}>
              {/* 火焰图信息栏 */}
            <div style={{ 
              height: 40,
              display: 'flex',
              alignItems: 'center',
              marginBottom: 12,
              padding: '0 16px',
              background: darkMode ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.02)',
              borderRadius: 8,
              border: `1px solid ${darkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)'}`,
              backdropFilter: 'blur(10px)',
              boxShadow: darkMode 
                ? '0 2px 8px rgba(0, 0, 0, 0.3)' 
                : '0 2px 8px rgba(0, 0, 0, 0.1)'
            }}>
              <div style={{ flex: 1, display: 'flex', alignItems: 'center', overflow: 'hidden', gap: 20 }}>
                {/* 维度显示 */}
                <div style={{ 
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8
                }}>
                  <div style={{
                    width: 4,
                    height: 16,
                    background: 'linear-gradient(135deg, #1890ff, #722ed1)',
                    borderRadius: 2
                  }} />
                  <div style={{ 
                    fontSize: 15, 
                    fontWeight: 600,
                    color: darkMode ? '#fff' : '#1f1f1f',
                    letterSpacing: '0.3px'
                  }}>
                    {dimensionOptions.find(d => d.value === dimension)?.label || 'CPU Time'}
                  </div>
                </div>
                
                {/* 总权重显示 */}
                {totalWeight > 0 && (
                  <div style={{ 
                    display: 'flex',
                    alignItems: 'center',
                    gap: 6,
                    padding: '4px 12px',
                    background: darkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.05)',
                    borderRadius: 16,
                    border: `1px solid ${darkMode ? 'rgba(255, 255, 255, 0.15)' : 'rgba(0, 0, 0, 0.1)'}`
                  }}>
                    <div style={{
                      width: 6,
                      height: 6,
                      borderRadius: '50%',
                      background: '#52c41a'
                    }} />
                    <div style={{ 
                      fontSize: 13, 
                      fontWeight: 500,
                      color: darkMode ? '#e6f7ff' : '#1890ff'
                    }}>
                      {toReadableValue(unit, totalWeight)}
                    </div>
                  </div>
                )}
                
                {/* 筛选状态指示 */}
                {isFiltering && (
                  <div style={{ 
                    display: 'flex',
                    alignItems: 'center', 
                    gap: 6,
                    padding: '4px 12px',
                    background: 'linear-gradient(135deg, #52c41a, #73d13d)',
                    borderRadius: 16,
                    fontSize: 12,
                    fontWeight: 500,
                    color: '#fff',
                    boxShadow: '0 2px 4px rgba(82, 196, 26, 0.3)',
                    animation: 'pulse 2s infinite'
                  }}>
                    <Spin size="small" style={{ color: '#fff' }} />
                    筛选中...
                  </div>
                )}
                
                {/* 复制提示 */}
                <div style={{ 
                  marginLeft: 'auto',
                  fontSize: 12, 
                  color: darkMode ? '#8c8c8c' : '#8c8c8c',
                  fontStyle: 'italic',
                  opacity: 0.8
                }}>
                  复制方法名: <kbd style={{
                    padding: '2px 6px',
                    background: darkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)',
                    borderRadius: 4,
                    fontSize: 11,
                    fontFamily: 'monospace',
                    border: `1px solid ${darkMode ? 'rgba(255, 255, 255, 0.2)' : 'rgba(0, 0, 0, 0.2)'}`
                  }}>Ctrl+C</kbd>
                </div>
              </div>
            </div>

          {/* 火焰图视图 */}
          <div style={{ 
            flex: 1,
              display: 'flex',
              flexDirection: 'column',
            minHeight: 0,
            overflow: 'hidden'
            }}>
              <Card 
                title={
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <span>火焰图分析</span>
                  </div>
                }
                style={{ 
                  height: '100%',
                  background: darkMode ? '#262626' : '#fff',
                  borderColor: darkMode ? '#434343' : '#f0f0f0',
                  display: 'flex',
                  flexDirection: 'column'
                }}
                styles={{ 
                  body: { 
                    flex: 1,
                    padding: 0,
                    overflow: 'auto',
                    display: 'flex',
                    flexDirection: 'column',
                    minHeight: 0
                  }
                }}
              >
                <Spin spinning={loading} tip="火焰图加载中...">
                    <ReactFlameGraphWrapper
                    ref={flameGraphRef}
                    data={flameGraphDataSource}
                    symbolTable={symbolTable}
                      search={search}
                      dimension={dimension}
                    unit={unit}
                      darkMode={darkMode}
                    />
                </Spin>
              </Card>
            </div>
        </Content>

        {/* 右侧筛选面板 */}
        <AntdSider
          width={360}
          theme={darkMode ? 'dark' : 'light'}
          style={{
            background: darkMode ? '#1f1f1f' : '#fff',
            borderLeft: `1px solid ${darkMode ? '#303030' : '#f0f0f0'}`,
            overflow: 'hidden',
            flexShrink: 0
          }}
        >
            <div style={{ 
            padding: 16,
            overflowY: 'auto',
            height: '100%'
          }}>
            {/* 筛选器控制面板 */}
            <Card 
              title={
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span>
                    <FilterOutlined style={{ marginRight: 8 }} />
                    数据筛选
                  </span>
                  {isFiltering && (
                    <Badge 
                      count="筛选中" 
                      style={{ 
                        backgroundColor: '#52c41a',
                        fontSize: '10px',
                        height: '16px',
                        lineHeight: '16px'
                      }} 
                    />
                  )}
                </div>
              }
              size="small"
              style={{ 
                marginBottom: 16,
                background: darkMode ? '#262626' : '#fff',
                borderColor: darkMode ? '#434343' : '#f0f0f0'
              }}
            >
              {/* 筛选类型选择 */}
              <div style={{ marginBottom: 12 }}>
              <div style={{ 
                  fontSize: 12, 
                  color: darkMode ? '#ccc' : '#666', 
                  marginBottom: 4,
                  fontWeight: 500
                }}>
                  筛选类型
                </div>
                <Select
                  style={{ width: '100%' }}
                  value={selectedFilterIndex}
                  onChange={setSelectedFilterIndex}
                  placeholder="选择筛选类型"
                  size="small"
                >
                  <Select.Option value={0} key="thread">
                    <UserOutlined style={{ marginRight: 8 }} />
                    线程 (Thread)
                  </Select.Option>
                  <Select.Option value={1} key="class">
                    <CodeOutlined style={{ marginRight: 8 }} />
                    类 (Class)
                  </Select.Option>
                  <Select.Option value={2} key="method">
                    <MethodOutlined style={{ marginRight: 8 }} />
                    方法 (Method)
                  </Select.Option>
                </Select>
              </div>
              {/* 统计信息和控制 */}
              <div style={{ 
                display: 'flex',
                alignItems: 'center', 
                justifyContent: 'space-between',
                marginBottom: 12,
                padding: '8px 12px',
                background: darkMode ? '#1a1a1a' : '#f8f9fa',
                borderRadius: 6,
                border: `1px solid ${darkMode ? '#434343' : '#e8e8e8'}`
              }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  <div style={{ fontSize: 12, color: darkMode ? '#ccc' : '#666' }}>
                    已选择: <span style={{ color: darkMode ? '#fff' : '#000', fontWeight: 500 }}>{checkedCount}</span>/{filterValues.length}
                  </div>
                  {totalWeight > 0 && (
                    <div style={{ fontSize: 11, color: darkMode ? '#999' : '#999' }}>
                      总权重: {toReadableValue(unit, totalWeight)}
                    </div>
                  )}
                </div>
                <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                  <Button
                    size="small"
                    type="text"
                    icon={<ReloadOutlined />}
                    onClick={resetFilterState}
                    title="重置筛选"
                  />
                  <Checkbox
                    checked={toggleFilterValuesChecked}
                    onChange={(e) => handleToggleFilterValuesChecked(e.target.checked)}
                  >
                    全选
                  </Checkbox>
                </div>
              </div>
            </Card>

            {/* 筛选值列表 */}
                <Card 
                  title={
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span>筛选值列表</span>
                  <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <Button.Group size="small">
                      <Button
                        type={filterSortBy === 'weight' ? 'primary' : 'default'}
                        onClick={() => handleSortChange('weight')}
                        icon={filterSortBy === 'weight' && filterSortOrder === 'desc' ? <BarChartOutlined /> : <BarChartOutlined />}
                      >
                        权重
                      </Button>
                      <Button
                        type={filterSortBy === 'name' ? 'primary' : 'default'}
                        onClick={() => handleSortChange('name')}
                        icon={filterSortBy === 'name' && filterSortOrder === 'desc' ? <BarChartOutlined /> : <BarChartOutlined />}
                      >
                        名称
                      </Button>
                    </Button.Group>
                    {filterSortOrder === 'asc' ? <BarChartOutlined style={{ fontSize: 12 }} /> : <BarChartOutlined style={{ fontSize: 12, transform: 'rotate(180deg)' }} />}
                  </div>
                    </div>
                  }
              size="small"
                  style={{ 
                height: 'calc(100% - 200px)',
                    background: darkMode ? '#262626' : '#fff',
                    borderColor: darkMode ? '#434343' : '#f0f0f0',
                    display: 'flex',
                    flexDirection: 'column'
                  }}
                  styles={{ 
                    body: { 
                      flex: 1,
                      padding: 0,
                      overflow: 'hidden',
                      display: 'flex',
                      flexDirection: 'column',
                      minHeight: 0
                    }
                  }}
                >
                    <div style={{ 
                      flex: 1,
                overflowY: 'auto',
                overflowX: 'hidden',
                padding: 8,
                      minHeight: 0
                    }}>
                {filteredAndSortedValues.map((item, index) => {
                  const originalIndex = filterValues.findIndex(v => v.key === item.key);
                  return (
                    <div 
                      key={item.key} 
                      style={{ 
                      display: 'flex',
                        alignItems: 'center', 
                        justifyContent: 'space-between',
                        padding: '8px 12px',
                        marginBottom: 2,
                        background: item.checked 
                          ? (darkMode ? '#1a3a1a' : '#f6ffed') 
                          : (darkMode ? '#1a1a1a' : '#fafafa'),
                        borderRadius: 4,
                        border: `1px solid ${item.checked 
                          ? (darkMode ? '#52c41a' : '#b7eb8f') 
                          : (darkMode ? '#434343' : '#e8e8e8')}`,
                        transition: 'all 0.2s ease',
                        cursor: 'pointer'
                      }}
                      onClick={() => {
                        const filterTypes = ['Thread', 'Class', 'Method'];
                        const filterName = filterTypes[selectedFilterIndex] || 'Thread';
                        
                        // 更新filterValues
                        const newFilterValues = [...filterValues];
                        newFilterValues[originalIndex].checked = !item.checked;
                        setFilterValues(newFilterValues);
                        
                        // 调用筛选处理函数，传递更新后的状态
                        handleFilterValuesChecked(!item.checked, originalIndex, newFilterValues);
                      }}
                    >
                      <div style={{ flex: 1, marginRight: 8, minWidth: 0 }}>
                        {/* 方法名称 */}
                        <div style={{ 
                          fontSize: 12, 
                          color: darkMode ? '#fff' : '#000',
                          marginBottom: 4,
                          fontWeight: item.checked ? 500 : 400,
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                          position: 'relative'
                        }}>
                          <Tooltip title={item.key} placement="topLeft">
                            <span style={{ 
                              display: 'inline-block',
                              maxWidth: '100%',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap'
                            }}>
                              {item.key}
                            </span>
                          </Tooltip>
                    </div>
                        
                        {/* 权重和百分比信息 */}
          <div style={{
                          display: 'flex', 
                          alignItems: 'center', 
                          justifyContent: 'space-between',
                          marginBottom: 3
                        }}>
                          <div style={{ 
                            fontSize: 10, 
                            color: darkMode ? '#999' : '#666',
                            fontWeight: 500,
                            flex: 1,
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap'
                          }}>
                            {toReadableValue(unit, item.weight)}
                          </div>
                          <div style={{ 
                            fontSize: 9, 
                            color: darkMode ? '#999' : '#999',
                            marginLeft: 8,
                            flexShrink: 0
                          }}>
                            {totalWeight > 0 ? Math.round((item.weight / totalWeight) * 100) : 0}%
                          </div>
              </div>
              
                        {/* 进度条 */}
                    <Progress
                          percent={totalWeight > 0 ? Math.round((item.weight / totalWeight) * 100) : 0}
                          size="small"
                          strokeColor={item.checked ? '#52c41a' : '#d9d9d9'}
                      showInfo={false}
                          style={{ height: 3 }}
                    />
                  </div>
                  
                      {/* 复选框 */}
                      <Checkbox
                        checked={item.checked}
                        onChange={(e) => {
                          e.stopPropagation();
                          const filterTypes = ['Thread', 'Class', 'Method'];
                          const filterName = filterTypes[selectedFilterIndex] || 'Thread';
                          // 更新filterValues
                          const newFilterValues = [...filterValues];
                          newFilterValues[originalIndex].checked = e.target.checked;
                          setFilterValues(newFilterValues);
                          
                          // 调用筛选处理函数，传递更新后的状态
                          handleFilterValuesChecked(e.target.checked, originalIndex, newFilterValues);
                        }}
                        style={{ marginLeft: 4, flexShrink: 0 }}
                    />
                  </div>
                  );
                })}
                {filteredAndSortedValues.length === 0 && (
                  <div style={{ 
                    textAlign: 'center', 
                    color: darkMode ? '#999' : '#666',
                    padding: '40px 20px',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: 8
                  }}>
                    <SearchOutlined style={{ fontSize: 24, color: darkMode ? '#666' : '#ccc' }} />
                    <div>暂无筛选数据</div>
                    {filterSearchText && (
                      <div style={{ fontSize: 12, color: darkMode ? '#666' : '#999' }}>
                        尝试调整搜索关键词
                      </div>
                    )}
          </div>
        )}
              </div>
            </Card>
          </div>
        </AntdSider>

      </Layout>
    </Layout>
  );
};

export default Analysis;
