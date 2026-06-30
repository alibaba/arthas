import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { getFiles } from '../services/fileService';
import { FileView } from '../services/fileService';

interface FileContextType {
  files: FileView[];
  hasFiles: boolean;
  loading: boolean;
  refreshFiles: () => Promise<void>;
}

const FileContext = createContext<FileContextType | undefined>(undefined);

export const useFileContext = () => {
  const context = useContext(FileContext);
  if (context === undefined) {
    throw new Error('useFileContext must be used within a FileProvider');
  }
  return context;
};

interface FileProviderProps {
  children: ReactNode;
}

export const FileProvider: React.FC<FileProviderProps> = ({ children }) => {
  const [files, setFiles] = useState<FileView[]>([]);
  const [loading, setLoading] = useState(false);

  const refreshFiles = async () => {
    setLoading(true);
    try {
      const res = await getFiles({ page: 1, pageSize: 1000 });
      if (res.code === 1 && res.data && Array.isArray(res.data.items)) {
        setFiles(res.data.items);
      } else {
        setFiles([]);
      }
    } catch (error) {
      setFiles([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshFiles();
  }, []);

  const hasFiles = files.length > 0;

  const value: FileContextType = {
    files,
    hasFiles,
    loading,
    refreshFiles,
  };

  return (
    <FileContext.Provider value={value}>
      {children}
    </FileContext.Provider>
  );
}; 