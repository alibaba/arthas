// @ts-nocheck
// eslint-disable-next-line
import React from 'react';
import { Layout, Menu, Avatar, Typography } from 'antd';
import { UserOutlined, ClusterOutlined, HomeOutlined, BarChartOutlined } from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import { useFileContext } from '../stores/FileContext';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

const BasicLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { hasFiles, files } = useFileContext();

  const handleMenuClick = ({ key }: { key: string }) => {
    if (key === '/analysis') {
      // 如果有文件，跳转到第一个文件的分析页面
      if (hasFiles && files.length > 0) {
        navigate(`/analysis/${files[0].id}`);
      } else {
        // 如果没有文件，跳转到默认分析页面
        navigate('/analysis');
      }
    } else {
      navigate(key);
    }
  };

  const menuItems = [
    { key: '/home', icon: <HomeOutlined />, label: '文件管理' },
    {
      key: '/analysis',
      icon: <BarChartOutlined />,
      label: '分析详情',
      disabled: !hasFiles,
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={200} style={{ background: '#fff' }}>
        <div style={{ height: 48, display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: 18, borderBottom: '1px solid #f0f0f0' }}>
          <ClusterOutlined style={{ marginRight: 8, color: '#1890ff' }} />JFR分析平台
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname.startsWith('/analysis') ? '/analysis' : location.pathname]}
          style={{ height: '100%', borderRight: 0 }}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      <Layout>
        <Header style={{ display: 'flex', alignItems: 'center', background: '#fff', boxShadow: '0 2px 8px #f0f1f2', padding: '0 24px' }}>
          {/* <div style={{ flex: 1 }} />
          <div style={{ marginRight: 32 }}>
            <Text type="secondary">节点角色：</Text>
            <Text strong>{role}</Text>
          </div>
          <div>
            <Avatar icon={<UserOutlined />} style={{ marginRight: 8 }} />
            <Text>{username}</Text>
          </div> */}
        </Header>
        <Content style={{ padding: 0, background: '#f5f6fa' }}>
          {children}
        </Content>
      </Layout>
    </Layout>
  );
};

export default BasicLayout; 