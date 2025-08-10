// @ts-nocheck
// eslint-disable-next-line
import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import BasicLayout from './layouts/BasicLayout';
import Home from './pages/Home';
import Analysis from './pages/Analysis';
import { FileProvider } from './stores/FileContext';

const App: React.FC = () => {
  return (
    <FileProvider>
      <Router>
        <BasicLayout>
          <Routes>
            <Route path="/home" element={<Home />} />
            <Route path="/analysis" element={<Analysis />} />
            <Route path="/analysis/:fileId" element={<Analysis />} />
            <Route path="*" element={<Navigate to="/home" replace />} />
          </Routes>
        </BasicLayout>
      </Router>
    </FileProvider>
  );
};

export default App; 