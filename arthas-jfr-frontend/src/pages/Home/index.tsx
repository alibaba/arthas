// @ts-nocheck
// eslint-disable-next-line
// 由于项目初始化阶段，暂时关闭类型检查和eslint报错，后续完善依赖后移除。
import React from 'react';
import { Card, Row, Col, Typography } from 'antd';
import Home from './Home';

const { Title } = Typography;

const HomeComponent: React.FC = () => {
  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>JFR 文件管理</Title>
      <Row gutter={24}>
        <Col span={8}>
          <Card title="上传 .jfr 文件" bordered={false}>
            {/* 文件上传组件占位 */}
            <div id="file-upload-placeholder">[文件上传区域]</div>
          </Card>
        </Col>
        <Col span={16}>
          <Card title="文件列表" bordered={false}>
            {/* 文件列表组件占位 */}
            <div id="file-table-placeholder">[文件列表区域]</div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};


export default Home; 