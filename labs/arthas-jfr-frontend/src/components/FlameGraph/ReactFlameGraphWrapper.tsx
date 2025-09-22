import React, { useState, useRef, useCallback, useLayoutEffect, useEffect, useMemo, forwardRef, useImperativeHandle } from 'react';
import { toReadableValue, formatByteValue } from '../../utils/format';

interface ReactFlameGraphWrapperProps {
  data: any;
  symbolTable?: Record<string, string>;
  onNodeSelect?: (node: any) => void;
  search?: string;
  dimension?: string;
  unit?: string;
  darkMode?: boolean;
}

// 扩展HTMLElement接口以支持flame-graph元素
interface FlameGraphElement extends HTMLElement {
  configuration?: any;
  dataSource?: any;
  dispatchEvent(event: CustomEvent): boolean;
}

const ReactFlameGraphWrapper = forwardRef<any, ReactFlameGraphWrapperProps>(({
  data,
  symbolTable = {},
  onNodeSelect,
  search = '',
  dimension = 'CPU',
  unit = 'ns',
  darkMode = false
}, ref) => {
  const [graphSize, setGraphSize] = useState<{ width: number; height: number }>({ width: 800, height: 300 });
  const [flameGraphElement, setFlameGraphElement] = useState<HTMLElement | null>(null);
  const [isFixed, setIsFixed] = useState<boolean>(false);
  const [fixedNode, setFixedNode] = useState<any>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const flameGraphRef = useRef<any>(null);

  // 暴露火焰图元素给父组件
  useImperativeHandle(ref, () => flameGraphRef.current, [flameGraphRef.current]);


  // 格式化值的函数
  const formatValue = (val: number) => {
    if (!val || isNaN(val)) return '0';
    
    if (unit && (unit.toLowerCase() === 'b' || unit.toLowerCase() === 'bytes' || unit.toLowerCase() === 'byte')) {
      return formatByteValue(val, false);
    }
    return toReadableValue(unit || 'ns', val, false);
  };

  // 确保数据是Web Component需要的数组格式
  const ensureArrayFormat = useCallback((data: any): any[] => {
    if (!data) return [];
    
    // 如果已经是数组格式，直接返回
    if (Array.isArray(data)) {
      return data;
    }
    
    // 如果是树形结构，需要转换为数组格式
    const result: any[] = [];
    
    const processNode = (node: any, stack: number[] = []) => {
      if (!node) return;
      
      // 创建栈跟踪数组
      const currentStack = [...stack];
      if (node.name) {
        // 查找symbolTable中对应的frame ID
        const frameId = Object.keys(symbolTable).find(key => symbolTable[key] === node.name);
        if (frameId) {
          currentStack.push(parseInt(frameId));
        } else {
          // 如果没有找到，使用名称的哈希值
          const hashId = node.name.split('').reduce((a, b) => {
            a = ((a << 5) - a) + b.charCodeAt(0);
            return a & a;
          }, 0);
          currentStack.push(hashId);
        }
      }
      
      // 如果是叶子节点，添加到结果中
      if (!node.children || node.children.length === 0) {
        if (currentStack.length > 0) {
          result.push([currentStack, node.value || 0]);
        }
      } else {
        // 递归处理子节点
        node.children.forEach((child: any) => {
          processNode(child, currentStack);
        });
      }
    };
    
    processNode(data);
    return result;
  }, [symbolTable]);

  // 定义火焰图的文本生成器函数
  const rootTextGenerator = useCallback((ds: any, information: any) => {
    return `Total ${dimension}: ${formatValue(information.totalWeight)}`;
  }, [dimension, unit]);

  const textGenerator = useCallback((ds: any, frame: number) => {
    return symbolTable[frame] || `Frame ${frame}`;
  }, [symbolTable]);

  const titleGenerator = useCallback((ds: any, frame: number, information: any) => {
    const text = information.text;
    let i1 = text.lastIndexOf('.');
    if (i1 > 0) {
      let i2 = text.lastIndexOf('.', i1 - 1);
      if (i2 > 0) {
        if (!isNaN(Number(text.substring(i2 + 1, i1)))) {
          // java lambda ?
          let i3 = text.lastIndexOf('.', i2 - 1);
          if (i3 > 0) {
            return text.substring(i3 + 1);
          }
        } else {
          return text.substring(i2 + 1);
        }
      }
    }
    return text;
  }, []);

  const detailsGenerator = useCallback((ds: any, frame: number, information: any) => {
    const text = information.text;
    let i1 = text.lastIndexOf('.');
    if (i1 > 0) {
      let i2 = text.lastIndexOf('.', i1 - 1);
      if (i2 > 0) {
        let p;
        if (!isNaN(Number(text.substring(i2 + 1, i1)))) {
          // java lambda ?
          let i3 = text.lastIndexOf('.', i2 - 1);
          if (i3 > 0) {
            p = text.substring(0, i3);
          }
        } else {
          p = text.substring(0, i2);
        }
        if (p) {
          return { package: p };
        }
      }
    }
    return null;
  }, []);

  const footTextGenerator = useCallback((dataSource: any, frame: number, information: any) => {
    let sw = information.selfWeight;
    let w = information.weight;
    let tw = information.totalWeight;
    let value = Math.round((w / tw) * 100 * 100) / 100;
    if (w === sw || sw === 0) {
      return value + '% - ' + formatValue(w);
    }
    return value + '% - ' + formatValue(w) + '(' + formatValue(sw) + ')';
  }, [unit]);

  const hashCodeGenerator = useCallback((ds: any, frame: number, information: any) => {
    let text = information.text;
    if (text.startsWith('java') || text.startsWith('jdk') || text.startsWith('JVM')) {
      return 0;
    }
    let i1 = text.lastIndexOf('.');
    if (i1 !== -1) {
      let i2 = text.lastIndexOf('.', i1 - 1);
      if (i2 === -1) {
        text = text.substring(0, i1);
      }
      text = text.substring(0, i2);
    }

    let hash = 0;
    for (let i = 0; i < text.length; i++) {
      hash = 31 * hash + (text.charCodeAt(i) & 0xff);
      hash &= 0xffffffff;
    }
    return hash;
  }, []);

  // 定义火焰图的配置 - 使用useMemo避免每次渲染都创建新对象
  const configuration = useMemo(() => ({
    rootTextGenerator: rootTextGenerator,
    textGenerator: textGenerator,
    titleGenerator: titleGenerator,
    detailsGenerator: detailsGenerator,
    footTextGenerator: footTextGenerator,
    hashCodeGenerator: hashCodeGenerator,
    stackTraceFilter: null,
    showHelpButton: false
  }), [dimension, unit, symbolTable]);

  // 初始化火焰图Web Component
  useEffect(() => {
    if (!containerRef.current || !data) return;

    // 创建火焰图元素
    const flameGraphEl = document.createElement('flame-graph') as FlameGraphElement;
    flameGraphEl.setAttribute('id', 'flame-graph');
    flameGraphEl.setAttribute('downward', '');
    flameGraphEl.style.width = '100%';
    flameGraphEl.style.height = 'auto';
    flameGraphEl.style.minHeight = '400px';
    flameGraphEl.style.display = 'block';

    // 清空容器并添加火焰图
    if (containerRef.current) {
      containerRef.current.innerHTML = '';
      containerRef.current.appendChild(flameGraphEl);
    }

    // 设置配置和数据
    flameGraphEl.configuration = configuration;
    const arrayData = ensureArrayFormat(data);
    
    // 确保数据不为空
    if (arrayData.length === 0) {
    }
    
    flameGraphEl.dataSource = {
      format: 'line',
      data: arrayData
    };

    // 添加resize监听器
    const handleResize = () => {
      if (flameGraphEl && flameGraphEl.dispatchEvent) {
        flameGraphEl.dispatchEvent(new CustomEvent('re-render'));
      }
    };

    window.addEventListener('resize', handleResize);

    // 添加固定帧功能
    const handleFrameClick = (event: CustomEvent) => {
      const frameData = event.detail;
      if (frameData) {
        setFixedNode(frameData);
        setIsFixed(true);
        
        // 触发固定帧事件
        flameGraphEl.dispatchEvent(new CustomEvent('fix-frame', {
          detail: frameData
        }));
        
        if (onNodeSelect) {
          onNodeSelect(frameData);
        }
      }
    };

    const handleUnfixFrame = () => {
      setIsFixed(false);
      setFixedNode(null);
      
      // 触发取消固定帧事件
      flameGraphEl.dispatchEvent(new CustomEvent('unfix-frame'));
    };

    // 添加事件监听器
    flameGraphEl.addEventListener('frame-click', handleFrameClick as EventListener);
    flameGraphEl.addEventListener('unfix-frame', handleUnfixFrame);

    setFlameGraphElement(flameGraphEl);
    flameGraphRef.current = flameGraphEl;

    // 清理函数
    return () => {
      if (flameGraphEl) {
        flameGraphEl.removeEventListener('frame-click', handleFrameClick as EventListener);
        flameGraphEl.removeEventListener('unfix-frame', handleUnfixFrame);
      }
      window.removeEventListener('resize', handleResize);
    };
  }, [data, darkMode, onNodeSelect, unit, configuration]);


  // 当搜索条件改变时重新渲染
  useEffect(() => {
    if (flameGraphElement && data) {
      const arrayData = ensureArrayFormat(data);
      (flameGraphElement as FlameGraphElement).dataSource = {
        format: 'line',
        data: arrayData
      };
      flameGraphElement.dispatchEvent(new CustomEvent('re-render'));
    }
  }, [search, data, flameGraphElement, symbolTable]);

  // 键盘快捷键支持
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // ESC 退出固定模式
      if (e.key === 'Escape' && isFixed) {
        setIsFixed(false);
        setFixedNode(null);
        if (flameGraphElement) {
          flameGraphElement.dispatchEvent(new CustomEvent('unfix-frame'));
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isFixed, flameGraphElement]);

  // 容器尺寸调整
  useLayoutEffect(() => {
    const update = () => {
      if (!containerRef.current) return;
      const rect = containerRef.current.getBoundingClientRect();
      const width = Math.max(320, Math.floor(rect.width));
      const height = Math.max(220, Math.floor(rect.height));
      setGraphSize({ width, height });
    };
    update();
    const ro = new ResizeObserver(update);
    if (containerRef.current) ro.observe(containerRef.current);
    window.addEventListener('resize', update);
    return () => {
      ro.disconnect();
      window.removeEventListener('resize', update);
    };
  }, []);

  if (!data) {
    return (
      <div style={{
        height: '100%', minHeight: 300,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: darkMode ? '#999' : '#666',
        flexDirection: 'column',
        gap: 8
      }}>
        <div>暂无火焰图数据</div>
        <div style={{ fontSize: 12, color: darkMode ? '#666' : '#999' }}>
          请先选择文件并进行分析
        </div>
      </div>
    );
  }

  // 验证数据结构的完整性
  if (!data) {
    return (
      <div style={{
        height: '100%', minHeight: 300,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: darkMode ? '#999' : '#666',
        flexDirection: 'column',
        gap: 8
      }}>
        <div>暂无火焰图数据</div>
        <div style={{ fontSize: 12, color: darkMode ? '#666' : '#999' }}>
          请先选择文件并进行分析
        </div>
      </div>
    );
  }

  // 检查数据是否为空数组
  if (Array.isArray(data) && data.length === 0) {
    return (
      <div style={{
        height: '100%', minHeight: 300,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: darkMode ? '#999' : '#666',
        flexDirection: 'column',
        gap: 8
      }}>
        <div>暂无火焰图数据</div>
        <div style={{ fontSize: 12, color: darkMode ? '#666' : '#999' }}>
          当前筛选条件下没有数据
        </div>
      </div>
    );
  }

  return (
    <div 
      ref={containerRef} 
      style={{ 
        position: 'relative',
        width: '100%',
        height: '100%',
        background: darkMode ? '#1a1a1a' : '#fafafa',
        borderRadius: 6,
        overflow: 'auto',
        minHeight: '400px'
      }} 
    >
      {/* 固定帧状态指示器 */}
      {isFixed && fixedNode && (
            <div style={{ 
          position: 'absolute',
          top: 8,
          right: 8,
          background: darkMode ? 'rgba(0, 0, 0, 0.8)' : 'rgba(255, 255, 255, 0.9)',
          color: darkMode ? '#fff' : '#000',
              padding: '4px 8px',
          borderRadius: 4,
                  fontSize: 12,
          zIndex: 10,
                display: 'flex', 
                alignItems: 'center',
          gap: 8
        }}>
          <span>固定模式: {fixedNode.text || 'Unknown'}</span>
          <button
            onClick={() => {
              setIsFixed(false);
              setFixedNode(null);
              if (flameGraphElement) {
                flameGraphElement.dispatchEvent(new CustomEvent('unfix-frame'));
              }
            }}
            style={{
              background: 'none',
              border: 'none',
              color: 'inherit',
              cursor: 'pointer',
              fontSize: 14,
              padding: 0,
              width: 16,
              height: 16,
              display: 'flex', 
              alignItems: 'center',
              justifyContent: 'center'
            }}
            title="退出固定模式 (ESC)"
          >
            ×
          </button>
        </div>
      )}
    </div>
  );
});

ReactFlameGraphWrapper.displayName = 'ReactFlameGraphWrapper';

export default ReactFlameGraphWrapper;