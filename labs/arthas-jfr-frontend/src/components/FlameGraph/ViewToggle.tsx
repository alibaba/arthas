import React from 'react';
import { Button, Space, Typography } from 'antd';
import { 
  TableOutlined, 
  BarChartOutlined, 
  AppstoreOutlined 
} from '@ant-design/icons';

const { Text } = Typography;

export type ViewMode = 'table' | 'flame' | 'both';

interface ViewToggleProps {
  currentView: ViewMode;
  onViewChange: (view: ViewMode) => void;
  darkMode: boolean;
}

const ViewToggle: React.FC<ViewToggleProps> = ({
  currentView,
  onViewChange,
  darkMode
}) => {
  const buttonStyle = {
    border: `1px solid ${darkMode ? '#434343' : '#d9d9d9'}`,
    borderRadius: '6px',
    fontSize: '12px',
    height: '32px',
    padding: '0 12px',
    transition: 'all 0.2s ease'
  };

  const activeButtonStyle = {
    ...buttonStyle,
    background: '#1890ff',
    borderColor: '#1890ff',
    color: '#fff'
  };

  const inactiveButtonStyle = {
    ...buttonStyle,
    background: darkMode ? '#262626' : '#fff',
    color: darkMode ? '#d9d9d9' : '#666',
    borderColor: darkMode ? '#434343' : '#d9d9d9'
  };

  return (
    <div 
      className="view-toggle-container"
      style={{ 
        display: 'flex', 
        alignItems: 'center', 
        gap: 8,
        padding: '8px 0'
      }}
    >
      <Text style={{ 
        fontSize: '12px', 
        color: darkMode ? '#ccc' : '#666',
        marginRight: 8
      }}>
        视图:
      </Text>
      <Space size={4}>
        <Button
          type="text"
          icon={<TableOutlined />}
          style={currentView === 'table' ? activeButtonStyle : inactiveButtonStyle}
          onClick={() => onViewChange('table')}
        >
          Top Table
        </Button>
        <Button
          type="text"
          icon={<BarChartOutlined />}
          style={currentView === 'flame' ? activeButtonStyle : inactiveButtonStyle}
          onClick={() => onViewChange('flame')}
        >
          Flame Graph
        </Button>
        <Button
          type="text"
          icon={<AppstoreOutlined />}
          style={currentView === 'both' ? activeButtonStyle : inactiveButtonStyle}
          onClick={() => onViewChange('both')}
        >
          Both
        </Button>
      </Space>
    </div>
  );
};

export default ViewToggle;
