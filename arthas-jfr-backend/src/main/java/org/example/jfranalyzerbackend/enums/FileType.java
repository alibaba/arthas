package org.example.jfranalyzerbackend.enums;

import static org.example.jfranalyzerbackend.exception.CommonException.CE;

public enum FileType {


    JFR, LOG, OTHER
//    JFR_FILE("jfr-file");

//    private final String storageDirectoryName;
//
//    private final String apiNamespace;
//
//    private final String analysisUrlPath;
//
//    FileType(String tag) {
//        storageDirectoryName = tag;
//        apiNamespace = tag;
//        analysisUrlPath = tag + "-analysis";
//    }
//
//    public String getStorageDirectoryName() {
//        return storageDirectoryName;
//    }
//
//    public String getApiNamespace() {
//        return apiNamespace;
//    }
//
//    public String getAnalysisUrlPath() {
//        return analysisUrlPath;
//    }
//
//    public static FileType getByApiNamespace(String expected) {
//        for (FileType type : FileType.values()) {
//            if (expected.equals(type.apiNamespace)) {
//                return type;
//            }
//        }
//        throw CE(ServerErrorCode.UNSUPPORTED_NAMESPACE);
//    }
}
