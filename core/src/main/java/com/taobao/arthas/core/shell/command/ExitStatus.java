package com.taobao.arthas.core.shell.command;

/**
 * 命令执行的结束状态
 */
public class ExitStatus {

    /**
     * 异步执行的命令返回此状态，注意避免状态码冲突
     */
    public static final ExitStatus PENDING_STATUS = new ExitStatus(0x300000);

    /**
     * TODO 暂时兼容老代码使用，忽略检查此命令的status，命令改造完毕后需要移除此变量
     */
    public static final ExitStatus IGNORED_STATUS = new ExitStatus(0x100000);

    /**
     * 命令执行成功的状态
     * @return
     */
    public static ExitStatus success() {
        return new ExitStatus(0);
    }

    /**
     * 命令执行失败
     * @param statusCode
     * @param message
     * @return
     */
    public static ExitStatus failure(int statusCode, String message) {
        if (statusCode == 0) {
            throw new IllegalArgumentException("failure status code cannot be 0");
        }
        if (statusCode == PENDING_STATUS.statusCode) {
            throw new IllegalArgumentException("failure status cannot equals to PENDING_STATUS");
        }
        return new ExitStatus(statusCode, message);
    }

    /**
     * 判断是否为失败状态
     * @param exitStatus
     * @return
     */
    public static boolean isFailed(ExitStatus exitStatus) {
        return exitStatus != null && exitStatus.getStatusCode() != 0;
    }

    /**
     * 判断是否为异步执行等待状态
     * @param exitStatus
     * @return
     */
    public static boolean isPending(ExitStatus exitStatus) {
        return exitStatus != null && exitStatus.getStatusCode() == PENDING_STATUS.getStatusCode();
    }

    /**
     * TODO 判断是否为IGNORE状态，兼容老代码使用，改造完毕后需要移除
     * @param exitStatus
     * @return
     */
    public static boolean isIgnored(ExitStatus exitStatus) {
        return exitStatus != null && exitStatus.getStatusCode() == IGNORED_STATUS.getStatusCode();
    }

    private int statusCode;
    private String message;

    private ExitStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    private ExitStatus(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

}
