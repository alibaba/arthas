package com.taobao.arthas.core.command.model;

/**
 * 版本信息模型
 *
 * 该类用于封装version命令的返回结果，返回当前Arthas的版本信息。
 * 该类继承自ResultModel，是Arthas命令结果模型体系的一部分。
 *
 * 通过该模型，用户可以查看当前使用的Arthas版本，便于问题排查和版本管理。
 */
public class VersionModel extends ResultModel {

    /**
     * 版本号字符串
     *
     * 存储Arthas的版本信息，格式通常为：主版本号.次版本号.修订号（如：3.5.1）
     * 可能还会包含构建信息、发布日期等附加信息
     */
    private String version;

    /**
     * 获取模型类型
     *
     * 返回该模型的类型标识，用于前端或客户端识别模型类型。
     * 该方法继承自ResultModel父类，是命令结果模型的标准接口。
     *
     * @return 模型类型字符串"version"
     */
    @Override
    public String getType() {
        return "version";
    }

    /**
     * 获取版本号
     *
     * 返回Arthas的版本号字符串，用于显示当前Arthas的版本信息
     *
     * @return 版本号字符串，如果未设置则返回null
     */
    public String getVersion() {
        return version;
    }

    /**
     * 设置版本号
     *
     * 设置Arthas的版本号信息，通常在命令执行完成后由服务端设置
     *
     * @param version 版本号字符串
     */
    public void setVersion(String version) {
        this.version = version;
    }

}
