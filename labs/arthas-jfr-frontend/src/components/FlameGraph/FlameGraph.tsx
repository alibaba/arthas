// @ts-nocheck
// eslint-disable-next-line
import React, { useState, useImperativeHandle, forwardRef } from 'react';
import { colorForName } from '../../utils/color';
import { toReadableValue } from '../../utils/format';

// FlameGraph数据结构示例
type FlameNode = {
  name: string;
  value: number;
  children?: FlameNode[];
};

interface FlameGraphProps {
  data: FlameNode | null;
  width?: number;
  height?: number;
  search?: string;
  total?: number;
  onZoomChange?: (zoomNode: FlameNode | null) => void;
  dimension?: string; // 新增分析维度参数
}

const COLORS = [
  '#FFB74D', '#4FC3F7', '#81C784', '#BA68C8', '#E57373', '#FFD54F', '#A1887F', '#90A4AE', '#64B5F6', '#F06292',
  '#537e8b', '#c12561', '#fec91b', '#3f7350', '#408118', '#3ea9da', '#9fb036', '#b671c1', '#faa938'
];

const BAR_HEIGHT = 32;
const BAR_GAP = 4;
const TEXT_PADDING = 8;
const MIN_TEXT_WIDTH = 60; // 最小文本显示宽度

// 截断文本函数
function truncateText(text: string, maxWidth: number, fontSize: number = 12): string {
  if (maxWidth <= MIN_TEXT_WIDTH) {
    return '';
  }
  
  // 更精确的字符宽度估算
  const getCharWidth = (char: string) => {
    // 中文字符、全角字符
    if (/[\u4e00-\u9fa5\u3000-\u303f\uff00-\uffef]/.test(char)) {
      return 14;
    }
    // 数字和英文字符
    if (/[0-9a-zA-Z]/.test(char)) {
      return 8;
    }
    // 其他字符
    return 6;
  };
  
  // 计算文本总宽度
  const textWidth = text.split('').reduce((width, char) => width + getCharWidth(char), 0);
  const availableWidth = maxWidth - TEXT_PADDING * 2;
  
  if (textWidth <= availableWidth) {
    return text;
  }
  
  // 智能截断：优先保留方法名和类名
  const parts = text.split('.');
  if (parts.length >= 2) {
    const className = parts[parts.length - 2];
    const methodName = parts[parts.length - 1];
    
    // 如果只有类名和方法名，尝试显示完整的方法名
    if (parts.length === 2) {
      const methodWidth = methodName.split('').reduce((width, char) => width + getCharWidth(char), 0);
      if (methodWidth + 3 <= availableWidth) { // 3是省略号长度
        return `...${methodName}`;
      }
    }
    
    // 显示类名.方法名的形式
    const shortName = `${className}.${methodName}`;
    const shortWidth = shortName.split('').reduce((width, char) => width + getCharWidth(char), 0);
    if (shortWidth <= availableWidth) {
      return shortName;
    }
  }
  
  // 通用截断逻辑
  let currentWidth = 0;
  let truncatedText = '';
  const suffix = '...';
  const suffixWidth = 3 * 6; // 省略号宽度
  
  for (let i = 0; i < text.length; i++) {
    const charWidth = getCharWidth(text[i]);
    if (currentWidth + charWidth + suffixWidth <= availableWidth) {
      truncatedText += text[i];
      currentWidth += charWidth;
    } else {
      break;
    }
  }
  
  return truncatedText + suffix;
}

// 获取方法名的简短版本
function getShortMethodName(name: string): string {
  // 如果是完整的类名+方法名，提取方法名
  if (name.includes('.')) {
    const parts = name.split('.');
    if (parts.length >= 2) {
      const methodName = parts[parts.length - 1];
      const className = parts[parts.length - 2];
      return `${className}.${methodName}`;
    }
  }
  return name;
}

// 格式化方法名用于工具提示显示
function formatMethodName(name: string): string {
  if (!name.includes('.')) {
    return name;
  }
  
  const parts = name.split('.');
  if (parts.length === 2) {
    // 简单的类名.方法名格式
    return `${parts[0]}.${parts[1]}`;
  } else if (parts.length > 2) {
    // 复杂的包名.类名.方法名格式
    const className = parts[parts.length - 2];
    const methodName = parts[parts.length - 1];
    const packageName = parts.slice(0, -2).join('.');
    
    // 如果包名太长，只显示最后一部分
    if (packageName.length > 30) {
      const shortPackage = '...' + packageName.substring(packageName.length - 25);
      return `${shortPackage}.${className}.${methodName}`;
    }
    
    return `${packageName}.${className}.${methodName}`;
  }
  
  return name;
}

// 计算节点的总权重（包括所有子节点）
function calculateTotalWeight(node: FlameNode): number {
  let total = node.value || 0;
  if (node.children) {
    for (const child of node.children) {
      total += calculateTotalWeight(child);
    }
  }
  return total;
}

// 生成root节点的显示文本
function generateRootText(node: FlameNode, totalValue: number, dimension: string = 'CPU Time'): string {
  const totalWeight = calculateTotalWeight(node);
  // return `Total ${dimension}: ${toReadableValue('ns', totalWeight)}`;
  return `Total ${dimension}: ${toReadableValue('ns', node.value)}`;
}

// 复制文本到剪贴板
function copyToClipboard(text: string): void {
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).catch(err => {
      console.error('Failed to copy text: ', err);
    });
  } else {
    // 降级方案
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    try {
      document.execCommand('copy');
    } catch (err) {
      console.error('Failed to copy text: ', err);
    }
    document.body.removeChild(textArea);
  }
}

function FlameRect({ node, x, y, width, height, depth, onHover, isActive, onClick, isZoomed, isHighlighted, isRoot, totalValue, dimension }: any) {
  const displayText = width > MIN_TEXT_WIDTH ? truncateText(node.name, width) : '';
  const showText = displayText.length > 0;
  
  // 根据宽度调整字体大小
  const fontSize = width < 100 ? 10 : width < 150 ? 11 : 12;
  
  // 如果是root节点，显示总耗时
  const textToShow = isRoot ? generateRootText(node, totalValue, dimension) : displayText;
  const showRootText = isRoot && width > MIN_TEXT_WIDTH;
  
  // 根据深度调整透明度，防止覆盖
  const opacity = isActive ? 1 : Math.max(0.7, 1 - depth * 0.05);
  
  // 处理键盘事件
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'c') {
      e.preventDefault();
      copyToClipboard(node.name);
      // 可以添加一个临时的提示
      const originalText = e.currentTarget.textContent;
      e.currentTarget.textContent = '已复制!';
      setTimeout(() => {
        if (e.currentTarget) {
          e.currentTarget.textContent = originalText;
        }
      }, 1000);
    }
  };
  
  return (
    <g style={{ pointerEvents: 'all' }}>
      <rect
        x={x}
        y={y}
        width={width}
        height={height}
        fill={colorForName(node.name, COLORS)}
        opacity={opacity}
        stroke={isHighlighted ? '#ff8200' : '#fff'}
        strokeWidth={isHighlighted ? 3 : 1}
        rx={4}
        onMouseEnter={e => { 
          e.preventDefault();
          e.stopPropagation(); 
          onHover(node, e, true); 
        }}
        onMouseLeave={e => { 
          e.preventDefault();
          e.stopPropagation(); 
          onHover(node, e, false); 
        }}
        onClick={e => { e.stopPropagation(); onClick(node); }}
        onKeyDown={handleKeyDown}
        tabIndex={0}
        style={{ cursor: 'pointer' }}
      />
      {(showText || showRootText) && (
        <text
          x={x + TEXT_PADDING}
          y={y + height / 2 + 4}
          fontSize={fontSize}
          fill="#222"
          pointerEvents="none"
          style={{ userSelect: 'none' }}
          textAnchor="start"
          dominantBaseline="middle"
        >
          {textToShow}
        </text>
      )}
    </g>
  );
}

function renderFlame(node: FlameNode, x: number, y: number, width: number, depth: number, total: number, onHover: any, activeNode: any, onClick: any, zoomNode: any, search: string, dimension: string, isRoot: boolean = false): any[] {
  if (!node) return [];
  const isHighlighted = search && node.name.toLowerCase().includes(search.toLowerCase());
  
  // 为每个节点创建唯一的key
  const nodeKey = `${node.name}-${x}-${y}-${depth}`;
  
  const rects = [
    <FlameRect
      key={nodeKey}
      node={node}
      x={x}
      y={y}
      width={width}
      height={BAR_HEIGHT}
      depth={depth}
      onHover={onHover}
      isActive={activeNode === node}
      onClick={onClick}
      isZoomed={zoomNode === node}
      isHighlighted={isHighlighted}
      isRoot={isRoot}
      totalValue={total}
      dimension={dimension}
    />
  ];
  
  if (node.children && node.children.length > 0) {
    // 按照权重排序子节点，确保重要节点优先显示
    const sortedChildren = [...node.children].sort((a, b) => b.value - a.value);
    
    // 计算子节点的总权重
    const totalChildWeight = sortedChildren.reduce((sum, child) => sum + child.value, 0);
    
    if (totalChildWeight > 0) {
      let childX = x;
      const availableWidth = width;
      
      // 计算间隙 - 根据子节点数量动态调整
      const gapCount = sortedChildren.length - 1;
      const minGap = 1;
      const maxGap = Math.min(3, availableWidth * 0.02); // 最大间隙为可用宽度的2%
      const gapWidth = gapCount > 0 ? Math.min(maxGap, Math.max(minGap, availableWidth / (sortedChildren.length * 10))) : 0;
      
      // 计算实际可用宽度（减去间隙）
      const totalGap = gapCount * gapWidth;
      const actualAvailableWidth = Math.max(availableWidth - totalGap, availableWidth * 0.8); // 确保至少80%的宽度可用
      
      // 分配宽度给每个子节点
      let remainingWidth = actualAvailableWidth;
      const childWidths = [];
      
      for (let i = 0; i < sortedChildren.length; i++) {
        const child = sortedChildren[i];
        let childWidth = (child.value / totalChildWeight) * actualAvailableWidth;
        
        // 确保最小宽度
        const minWidth = Math.max(2, availableWidth / (sortedChildren.length * 2));
        childWidth = Math.max(childWidth, minWidth);
        
        // 确保不超过剩余空间
        childWidth = Math.min(childWidth, remainingWidth);
        
        childWidths.push(childWidth);
        remainingWidth -= childWidth;
      }
      
      // 渲染子节点
      for (let i = 0; i < sortedChildren.length; i++) {
        const child = sortedChildren[i];
        const childWidth = childWidths[i];
        
        if (childWidth > 0 && childX < x + availableWidth) {
          rects.push(...renderFlame(
            child, 
            childX, 
            y + BAR_HEIGHT + BAR_GAP, 
            childWidth, 
            depth + 1, 
            total, 
            onHover, 
            activeNode, 
            onClick, 
            zoomNode, 
            search, 
            dimension,
            false
          ));
          childX += childWidth + gapWidth;
        }
      }
    }
  }
  return rects;
}

const FlameGraph = forwardRef<{ resetZoom: () => void }, FlameGraphProps>(({ data, width, height = 320, search = '', total, onZoomChange, dimension = 'CPU Time' }, ref) => {
  const [hovered, setHovered] = useState<any>(null);
  const [tooltip, setTooltip] = useState<{ x: number; y: number; node: FlameNode } | null>(null);
  const [zoomNode, setZoomNode] = useState<any>(null);
  const containerRef = React.useRef<HTMLDivElement>(null);
  const [svgWidth, setSvgWidth] = useState(width || 900);

  React.useEffect(() => {
    function updateWidth() {
      if (containerRef.current) {
        const containerWidth = containerRef.current.offsetWidth;
        setSvgWidth(Math.max(containerWidth, 300)); // 最小宽度300px
      }
    }
    updateWidth();
    window.addEventListener('resize', updateWidth);
    return () => window.removeEventListener('resize', updateWidth);
  }, []);

  // 当缩放状态改变时通知父组件
  React.useEffect(() => {
    onZoomChange && onZoomChange(zoomNode);
  }, [zoomNode, onZoomChange]);

  // 暴露重置方法给父组件
  React.useImperativeHandle(ref, () => ({
    resetZoom: () => setZoomNode(null)
  }));

  // 添加全局键盘事件监听
  React.useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'c' && hovered) {
        e.preventDefault();
        copyToClipboard(hovered.name);
        // 显示复制成功提示
        const message = document.createElement('div');
        message.textContent = '已复制方法名到剪贴板';
        message.style.cssText = `
          position: fixed;
          top: 20px;
          right: 20px;
          background: #52c41a;
          color: white;
          padding: 8px 16px;
          border-radius: 4px;
          z-index: 10000;
          font-size: 14px;
          box-shadow: 0 2px 8px rgba(0,0,0,0.2);
        `;
        document.body.appendChild(message);
        setTimeout(() => {
          document.body.removeChild(message);
        }, 2000);
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [hovered]);

  if (!data) {
    return (
      <div style={{ 
        height: 400, 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center', 
        color: '#aaa', 
        fontSize: 20,
        background: '#fafafa',
        borderRadius: 8,
        border: '1px dashed #ddd'
      }}>
        暂无火焰图数据
      </div>
    );
  }

  // 计算最大深度
  function getDepth(node: FlameNode, d = 1): number {
    if (!node.children || node.children.length === 0) return d;
    return Math.max(...node.children.map(c => getDepth(c, d + 1)));
  }
  const maxDepth = getDepth(zoomNode || data);
  const svgHeight = Math.max(maxDepth * (BAR_HEIGHT + BAR_GAP) + 40, 200); // 最小高度200px

  // 悬停事件
  const handleHover = (node: FlameNode, e: any, active: boolean) => {
    console.log('Hover event:', { 
      nodeName: node.name, 
      active, 
      value: node.value,
      x: e.clientX,
      y: e.clientY,
      target: e.target
    });
    setHovered(active ? node : null);
    if (active) {
      setTooltip({
        x: e.clientX,
        y: e.clientY,
        node,
      });
    } else {
      setTooltip(null);
    }
  };

  // 点击缩放
  const handleClick = (node: FlameNode) => {
    setZoomNode(node);
  };

  // 双击重置
  const handleDoubleClick = (e: any) => {
    setZoomNode(null);
  };

  // 计算总权重
  function getTotal(node: FlameNode): number {
    return calculateTotalWeight(node);
  }
  const totalValue = total || getTotal(zoomNode || data);

  return (
    <div style={{ 
      position: 'relative', 
      width: '100%',
      minHeight: 200,
      background: '#fafafa',
      borderRadius: 8,
      overflow: 'hidden'
    }} ref={containerRef}>
      <svg 
        width={svgWidth} 
        height={svgHeight} 
        style={{ 
          display: 'block', 
          background: '#fafafa', 
          borderRadius: 8, 
          width: '100%', 
          minWidth: 300,
          maxWidth: '100%'
        }} 
        onDoubleClick={handleDoubleClick}
        viewBox={`0 0 ${svgWidth} ${svgHeight}`}
        preserveAspectRatio="xMidYMid meet"
      >
        {renderFlame(zoomNode || data, 0, 20, svgWidth, 0, totalValue, handleHover, hovered, handleClick, zoomNode, search, dimension, true)}
      </svg>
      {tooltip && (
        <div
          style={{
            position: 'fixed',
            left: Math.min(tooltip.x + 12, window.innerWidth - 250),
            top: Math.min(tooltip.y + 12, window.innerHeight - 150),
            background: '#fff',
            border: '1px solid #ddd',
            borderRadius: 6,
            boxShadow: '0 2px 8px #0001',
            padding: '8px 16px',
            zIndex: 1000,
            pointerEvents: 'none',
            color: '#222',
            fontSize: 14,
            minWidth: 200,
            maxWidth: 500,
            wordBreak: 'break-all',
            whiteSpace: 'pre-wrap',
          }}
        >
          <div style={{ 
            fontWeight: 'bold', 
            marginBottom: 4, 
            wordBreak: 'break-all',
            lineHeight: '1.4',
            fontSize: 13
          }}>
            {tooltip.node.name === 'root' ? 'Total' : formatMethodName(tooltip.node.name)}
          </div>
          <div style={{ fontSize: 12, color: '#666', marginBottom: 2 }}>
            耗时/值: {toReadableValue('ns', tooltip.node.value)}
          </div>
          <div style={{ fontSize: 12, color: '#666', marginBottom: 2 }}>
            百分比: {((tooltip.node.value / totalValue) * 100).toFixed(2)}%
          </div>
          {/* <div style={{ fontSize: 10, color: '#999', marginBottom: 2 }}>
            调试: {tooltip.node.name} (value: {tooltip.node.value})
          </div> */}
          {tooltip.node.children && (
            <div style={{ fontSize: 12, color: '#666' }}>
              子节点: {tooltip.node.children.length}
            </div>
          )}
          <div style={{ 
            fontSize: 11, 
            color: '#1890ff', 
            marginTop: 4, 
            borderTop: '1px solid #eee', 
            paddingTop: 4,
            fontWeight: 'bold',
            backgroundColor: '#f0f8ff',
            padding: '4px 8px',
            borderRadius: '3px',
           
          }}>
            💡 按 Ctrl+C 复制方法名
          </div>
        </div>
      )}
    </div>
  );
});

export default FlameGraph; 