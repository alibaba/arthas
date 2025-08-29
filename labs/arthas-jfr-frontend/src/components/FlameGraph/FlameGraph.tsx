// @ts-nocheck
// eslint-disable-next-line
import React, { useState, useImperativeHandle, forwardRef } from 'react';
import { colorForName, colorForFunctionType, colorForPerformance } from '../../utils/color';
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
  onNodeSelect?: (node: FlameNode) => void;
  dimension?: string;
  darkMode?: boolean;
  sandwichView?: boolean;
}

const BAR_HEIGHT = 24; // 减小高度，更紧凑
const BAR_GAP = 2; // 减小间隙
const TEXT_PADDING = 6;
const MIN_TEXT_WIDTH = 80; // 增加最小文本宽度

// 截断文本函数
function truncateText(text: string, maxWidth: number, fontSize: number = 12): string {
  if (maxWidth <= MIN_TEXT_WIDTH) {
    return '';
  }
  
  const getCharWidth = (char: string) => {
    if (/[\u4e00-\u9fa5\u3000-\u303f\uff00-\uffef]/.test(char)) {
      return 14;
    }
    if (/[0-9a-zA-Z]/.test(char)) {
      return 8;
    }
    return 6;
  };
  
  const textWidth = text.split('').reduce((width, char) => width + getCharWidth(char), 0);
  const availableWidth = maxWidth - TEXT_PADDING * 2;
  
  if (textWidth <= availableWidth) {
    return text;
  }
  
  const parts = text.split('.');
  if (parts.length >= 2) {
    const className = parts[parts.length - 2];
    const methodName = parts[parts.length - 1];
    
    if (parts.length === 2) {
      const methodWidth = methodName.split('').reduce((width, char) => width + getCharWidth(char), 0);
      if (methodWidth + 3 <= availableWidth) {
        return `...${methodName}`;
      }
    }
    
    const shortName = `${className}.${methodName}`;
    const shortWidth = shortName.split('').reduce((width, char) => width + getCharWidth(char), 0);
    if (shortWidth <= availableWidth) {
      return shortName;
    }
  }
  
  let currentWidth = 0;
  let truncatedText = '';
  const suffix = '...';
  const suffixWidth = 3 * 6;
  
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

// 格式化方法名用于工具提示显示
function formatMethodName(name: string): string {
  if (!name.includes('.')) {
    return name;
  }
  
  const parts = name.split('.');
  if (parts.length === 2) {
    return `${parts[0]}.${parts[1]}`;
  } else if (parts.length > 2) {
    const className = parts[parts.length - 2];
    const methodName = parts[parts.length - 1];
    const packageName = parts.slice(0, -2).join('.');
    
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
  return `total (${toReadableValue('ns', node.value)})`;
}

// 复制文本到剪贴板
function copyToClipboard(text: string): void {
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).catch(err => {
      console.error('Failed to copy text: ', err);
    });
  } else {
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

function FlameRect({ 
  node, 
  x, 
  y, 
  width, 
  height, 
  depth, 
  onHover, 
  isActive, 
  onClick, 
  onNodeSelect,
  isZoomed, 
  isHighlighted, 
  isRoot, 
  totalValue, 
  dimension,
  darkMode 
}: any) {
  const displayText = width > MIN_TEXT_WIDTH ? truncateText(node.name, width) : '';
  const showText = displayText.length > 0;
  
  const fontSize = width < 120 ? 10 : width < 180 ? 11 : 12;
  
  const textToShow = isRoot ? generateRootText(node, totalValue, dimension) : displayText;
  const showRootText = isRoot && width > MIN_TEXT_WIDTH;
  
  // 根据深度调整透明度，防止覆盖
  const opacity = isActive ? 1 : Math.max(0.85, 1 - depth * 0.03);
  
  // 智能颜色选择
  const getNodeColor = () => {
    if (isRoot) return '#666666';
    if (isHighlighted) return '#ff8200';
    
    // 基于函数类型选择颜色
    const functionColor = colorForFunctionType(node.name);
    if (functionColor !== '#666666') return functionColor;
    
    // 基于性能热度选择颜色
    return colorForPerformance(node.value, totalValue);
  };
  
  const nodeColor = getNodeColor();
  
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'c') {
      e.preventDefault();
      copyToClipboard(node.name);
      const originalText = e.currentTarget.textContent;
      e.currentTarget.textContent = '已复制!';
      setTimeout(() => {
        if (e.currentTarget) {
          e.currentTarget.textContent = originalText;
        }
      }, 1000);
    }
  };

  const handleClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (onClick) {
      onClick(node);
    }
    if (onNodeSelect) {
      onNodeSelect(node);
    }
  };
  
  return (
    <g style={{ pointerEvents: 'all' }}>
      <rect
        x={x}
        y={y}
        width={width}
        height={height}
        fill={nodeColor}
        opacity={opacity}
        stroke={isHighlighted ? '#ff8200' : darkMode ? '#444' : '#fff'}
        strokeWidth={isHighlighted ? 2 : 0.5}
        rx={2}
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
        onClick={handleClick}
        onKeyDown={handleKeyDown}
        tabIndex={0}
        style={{ cursor: 'pointer' }}
      />
      {(showText || showRootText) && (
        <text
          x={x + TEXT_PADDING}
          y={y + height / 2 + 3}
          fontSize={fontSize}
          fill={darkMode ? '#fff' : '#000'}
          pointerEvents="none"
          style={{ userSelect: 'none' }}
          textAnchor="start"
          dominantBaseline="middle"
          fontWeight={isRoot ? 'bold' : 'normal'}
        >
          {textToShow}
        </text>
      )}
    </g>
  );
}

function renderFlame(
  node: FlameNode, 
  x: number, 
  y: number, 
  width: number, 
  depth: number, 
  total: number, 
  onHover: any, 
  activeNode: any, 
  onClick: any, 
  onNodeSelect: any,
  zoomNode: any, 
  search: string, 
  dimension: string, 
  isRoot: boolean = false,
  darkMode: boolean = false
): any[] {
  if (!node) return [];
  const isHighlighted = search && node.name.toLowerCase().includes(search.toLowerCase());
  
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
      onNodeSelect={onNodeSelect}
      isZoomed={zoomNode === node}
      isHighlighted={isHighlighted}
      isRoot={isRoot}
      totalValue={total}
      dimension={dimension}
      darkMode={darkMode}
    />
  ];
  
  if (node.children && node.children.length > 0) {
    // 按照权重排序子节点，确保重要节点优先显示
    const sortedChildren = [...node.children].sort((a, b) => b.value - a.value);
    const totalChildWeight = sortedChildren.reduce((sum, child) => sum + child.value, 0);
    
    if (totalChildWeight > 0) {
      let childX = x;
      const availableWidth = width;
      
      // 计算间隙 - 更紧凑的布局
      const gapCount = sortedChildren.length - 1;
      const minGap = 0.5;
      const maxGap = Math.min(2, availableWidth * 0.01);
      const gapWidth = gapCount > 0 ? Math.min(maxGap, Math.max(minGap, availableWidth / (sortedChildren.length * 20))) : 0;
      
      const totalGap = gapCount * gapWidth;
      const actualAvailableWidth = Math.max(availableWidth - totalGap, availableWidth * 0.9);
      
      let remainingWidth = actualAvailableWidth;
      const childWidths = [];
      
      for (let i = 0; i < sortedChildren.length; i++) {
        const child = sortedChildren[i];
        let childWidth = (child.value / totalChildWeight) * actualAvailableWidth;
        
        const minWidth = Math.max(1, availableWidth / (sortedChildren.length * 3));
        childWidth = Math.max(childWidth, minWidth);
        
        childWidth = Math.min(childWidth, remainingWidth);
        
        childWidths.push(childWidth);
        remainingWidth -= childWidth;
      }
      
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
            onNodeSelect,
            zoomNode, 
            search, 
            dimension,
            false,
            darkMode
          ));
          childX += childWidth + gapWidth;
        }
      }
    }
  }
  return rects;
}

const FlameGraph = forwardRef<{ resetZoom: () => void; zoomToNode: (node: FlameNode) => void }, FlameGraphProps>(
  ({ 
    data, 
    width, 
    height = 320, 
    search = '', 
    total, 
    onZoomChange, 
    onNodeSelect,
    dimension = 'CPU Time',
    darkMode = false,
    sandwichView = false
  }, ref) => {
    const [hovered, setHovered] = useState<any>(null);
    const [tooltip, setTooltip] = useState<{ x: number; y: number; node: FlameNode } | null>(null);
    const [zoomNode, setZoomNode] = useState<any>(null);
    const containerRef = React.useRef<HTMLDivElement>(null);
    const [svgWidth, setSvgWidth] = useState(width || 900);

    React.useEffect(() => {
      function updateWidth() {
        if (containerRef.current) {
          const containerWidth = containerRef.current.offsetWidth;
          setSvgWidth(Math.max(containerWidth, 300));
        }
      }
      updateWidth();
      window.addEventListener('resize', updateWidth);
      return () => window.removeEventListener('resize', updateWidth);
    }, []);

    React.useEffect(() => {
      onZoomChange && onZoomChange(zoomNode);
    }, [zoomNode, onZoomChange]);

    React.useImperativeHandle(ref, () => ({
      resetZoom: () => setZoomNode(null),
      zoomToNode: (node: FlameNode) => setZoomNode(node)
    }));

    React.useEffect(() => {
      const handleKeyDown = (e: KeyboardEvent) => {
        if ((e.ctrlKey || e.metaKey) && e.key === 'c' && hovered) {
          e.preventDefault();
          copyToClipboard(hovered.name);
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
          color: darkMode ? '#aaa' : '#aaa', 
          fontSize: 20,
          background: darkMode ? '#1a1a1a' : '#fafafa',
          borderRadius: 8,
          border: `1px dashed ${darkMode ? '#333' : '#ddd'}`
        }}>
          暂无火焰图数据
        </div>
      );
    }

    function getDepth(node: FlameNode, d = 1): number {
      if (!node.children || node.children.length === 0) return d;
      return Math.max(...node.children.map(c => getDepth(c, d + 1)));
    }
    const maxDepth = getDepth(zoomNode || data);
    const svgHeight = Math.max(maxDepth * (BAR_HEIGHT + BAR_GAP) + 40, 200);

    const handleHover = (node: FlameNode, e: any, active: boolean) => {
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

    const handleClick = (node: FlameNode) => {
      setZoomNode(node);
    };

    const handleDoubleClick = (e: any) => {
      setZoomNode(null);
    };

    function getTotal(node: FlameNode): number {
      return calculateTotalWeight(node);
    }
    const totalValue = total || getTotal(zoomNode || data);

    return (
      <div style={{ 
        position: 'relative', 
        width: '100%',
        minHeight: 200,
        background: darkMode ? '#1a1a1a' : '#fafafa',
        borderRadius: 8,
        overflow: 'hidden'
      }} ref={containerRef}>
        <svg 
          width={svgWidth} 
          height={svgHeight} 
          style={{ 
            display: 'block', 
            background: darkMode ? '#1a1a1a' : '#fafafa', 
            borderRadius: 8, 
            width: '100%', 
            minWidth: 300,
            maxWidth: '100%'
          }} 
          onDoubleClick={handleDoubleClick}
          viewBox={`0 0 ${svgWidth} ${svgHeight}`}
          preserveAspectRatio="xMidYMid meet"
        >
          {renderFlame(
            zoomNode || data, 
            0, 
            20, 
            svgWidth, 
            0, 
            totalValue, 
            handleHover, 
            hovered, 
            handleClick, 
            onNodeSelect,
            zoomNode, 
            search, 
            dimension, 
            true,
            darkMode
          )}
        </svg>
        {tooltip && (
          <div
            style={{
              position: 'fixed',
              left: Math.min(tooltip.x + 12, window.innerWidth - 300),
              top: Math.min(tooltip.y + 12, window.innerHeight - 200),
              background: darkMode ? '#262626' : '#fff',
              border: `1px solid ${darkMode ? '#434343' : '#ddd'}`,
              borderRadius: 8,
              boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
              padding: '12px 16px',
              zIndex: 1000,
              pointerEvents: 'none',
              color: darkMode ? '#fff' : '#222',
              fontSize: 14,
              minWidth: 250,
              maxWidth: 400,
              wordBreak: 'break-all',
              whiteSpace: 'pre-wrap',
              backdropFilter: 'blur(10px)',
            }}
          >
            <div style={{ 
              fontWeight: 'bold', 
              marginBottom: 8, 
              wordBreak: 'break-all',
              lineHeight: '1.4',
              fontSize: 13,
              color: darkMode ? '#fff' : '#000'
            }}>
              {tooltip.node.name === 'root' ? 'Total' : formatMethodName(tooltip.node.name)}
            </div>
            <div style={{ fontSize: 12, color: darkMode ? '#ccc' : '#666', marginBottom: 4 }}>
              耗时: {toReadableValue('ns', tooltip.node.value)}
            </div>
            <div style={{ fontSize: 12, color: darkMode ? '#ccc' : '#666', marginBottom: 4 }}>
              占比: {((tooltip.node.value / totalValue) * 100).toFixed(2)}%
            </div>
            {tooltip.node.children && (
              <div style={{ fontSize: 12, color: darkMode ? '#ccc' : '#666', marginBottom: 8 }}>
                子函数: {tooltip.node.children.length}
              </div>
            )}
            <div style={{ 
              fontSize: 11, 
              color: '#1890ff', 
              marginTop: 8, 
              borderTop: `1px solid ${darkMode ? '#434343' : '#eee'}`, 
              paddingTop: 8,
              fontWeight: 'bold',
              backgroundColor: darkMode ? '#1a1a1a' : '#f0f8ff',
              padding: '6px 10px',
              borderRadius: '4px',
            }}>
              💡 Ctrl+C 复制方法名
            </div>
          </div>
        )}
      </div>
    );
  }
);

export default FlameGraph; 