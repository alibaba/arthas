package com.taobao.arthas.core.command.model;

public class StatusModel extends ResultModel {

    /**
     * 异步执行的命令返回此状态，注意避免状态码冲突
     */
    public static final StatusModel PENDING_STATUS = new StatusModel(0x300000);

    /**
     * TODO 暂时兼容老代码使用，忽略检查此命令的status，命令改造完毕后需要移除此变量
     */
    public static final StatusModel IGNORED_STATUS = new StatusModel(0x100000);

    /**
     * 命令执行成功的状态
     * @return
     */
    public static StatusModel success() {
        return new StatusModel(0);
    }

    /**
     * 命令执行失败
     * @param statusCode
     * @param message
     * @return
     */
    public static StatusModel failure(int statusCode, String message) {
        if (statusCode == 0) {
            throw new IllegalArgumentException("failure status code cannot be 0");
        }
        if (statusCode == PENDING_STATUS.statusCode) {
            throw new IllegalArgumentException("failure status cannot equals to PENDING_STATUS");
        }
        return new StatusModel(statusCode, message);
    }

    /**
     * 判断是否为失败状态
     * @param statusModel
     * @return
     */
    public static boolean isFailed(StatusModel statusModel) {
        return statusModel != null && statusModel.getStatusCode() != 0;
    }

    /**
     * 判断是否为异步执行等待状态
     * @param statusModel
     * @return
     */
    public static boolean isPending(StatusModel statusModel) {
        return statusModel != null && statusModel.getStatusCode() == PENDING_STATUS.getStatusCode();
    }

    /**
     * TODO 判断是否为IGNORE状态，兼容老代码使用，改造完毕后需要移除
     * @param statusModel
     * @return
     */
    public static boolean isIgnored(StatusModel statusModel) {
        return statusModel != null && statusModel.getStatusCode() == IGNORED_STATUS.getStatusCode();
    }

    private int statusCode;
    private String message;

    public StatusModel() {
    }

    public StatusModel(int statusCode) {
        this.statusCode = statusCode;
    }

    public StatusModel(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public StatusModel setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public StatusModel setMessage(String message) {
        this.message = message;
        return this;
    }

    public StatusModel setStatus(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
        return this;
    }

    public StatusModel setStatus(int statusCode) {
        return this.setStatus(statusCode, null);
    }

    @Override
    public String getType() {
        return "status";
    }

}
