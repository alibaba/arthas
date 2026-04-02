package com.taobao.arthas.core.command.model;

/**
 * Cat命令结果模型类
 * <p>
 * 该类用于封装Cat命令的执行结果，继承自ResultModel基类并实现Countable接口。
 * 主要用于存储文件路径和文件内容信息，支持通过size()方法计算内容的大致大小。
 * </p>
 *
 * @author gongdewei 2020/5/11
 */
public class CatModel extends ResultModel implements Countable {

    /**
     * 文件路径
     * 表示要查看的文件的完整路径
     */
    private String file;

    /**
     * 文件内容
     * 存储文件的文本内容
     */
    private String content;

    /**
     * 默认构造函数
     * 创建一个空的CatModel对象
     */
    public CatModel() {
    }

    /**
     * 带参数的构造函数
     *
     * @param file    文件路径
     * @param content 文件内容
     */
    public CatModel(String file, String content) {
        this.file = file;
        this.content = content;
    }

    /**
     * 获取结果类型
     * 用于标识这是一个Cat命令的结果
     *
     * @return 返回"cat"字符串
     */
    @Override
    public String getType() {
        return "cat";
    }

    /**
     * 获取文件路径
     *
     * @return 文件路径字符串
     */
    public String getFile() {
        return file;
    }

    /**
     * 设置文件路径
     *
     * @param file 文件路径字符串
     */
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * 获取文件内容
     *
     * @return 文件内容字符串
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置文件内容
     *
     * @param content 文件内容字符串
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 计算内容的大致大小
     * 用于Countable接口，粗略估算内容的行数作为大小
     * 计算方式：内容长度除以100再加1
     *
     * @return 内容的大致大小，如果内容为null则返回0
     */
    @Override
    public int size() {
        if (content != null) {
            // 粗略计算行数作为item size
            return content.length()/100 + 1;
        }
        return 0;
    }
}
