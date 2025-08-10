// @ts-nocheck
// eslint-disable-next-line
// 由于项目初始化阶段，暂时关闭类型检查和eslint报错，后续完善依赖后移除。
import React, { useState } from 'react';
import { Card, Row, Col, Typography, Modal, Tooltip } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import FileUpload from '../../components/FileUpload';
import FileTable from '../../components/FileTable';
import { useWindowSize } from '../../hooks/useWindowSize';

const { Title } = Typography;

const Home: React.FC = () => {
  const { isMobile } = useWindowSize();
  const [uploadVisible, setUploadVisible] = useState(false);
  
  const handleUploadSuccess = () => {
    setUploadVisible(false);
  };

  return (
    <div style={{ 
      padding: isMobile ? 12 : 24,
      minHeight: '100vh',
      backgroundColor: '#f5f6fa'
    }}>
      <Title level={isMobile ? 3 : 2} style={{ marginBottom: 16 }}>
        JFR 文件管理
      </Title>
      <Row gutter={isMobile ? 12 : 24}>
        <Col xs={24} sm={24} md={24} lg={24} xl={24}>
          <Card
            title={
              <div style={{ 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: 8
              }}>
                <span style={{ fontSize: isMobile ? 14 : 16 }}>
                  文件列表
                </span>
                <Tooltip title="上传文件">
                  <UploadOutlined 
                    style={{ 
                      fontSize: isMobile ? 16 : 20, 
                      cursor: 'pointer',
                      color: '#1890ff'
                    }} 
                    onClick={() => setUploadVisible(true)} 
                  />
                </Tooltip>
              </div>
            }
            bordered={false}
            style={{ 
              borderRadius: 8,
              boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
            }}
            bodyStyle={{ 
              padding: isMobile ? 12 : 24 
            }}
          >
            <FileTable />
          </Card>
          <Modal
            open={uploadVisible}
            title="上传 .jfr 文件"
            footer={null}
            onCancel={() => setUploadVisible(false)}
            destroyOnClose
            width={isMobile ? '90%' : 520}
            centered
          >
            <FileUpload onUploadSuccess={handleUploadSuccess} />
          </Modal>
        </Col>
      </Row>
    </div>
  );
};

export default Home; 