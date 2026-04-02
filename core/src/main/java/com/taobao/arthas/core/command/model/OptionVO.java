package com.taobao.arthas.core.command.model;

/**
 * 选项视图对象
 * 用于封装Arthas命令行选项的详细信息，包括选项的名称、类型、值、描述等
 * 主要用于options命令，展示当前会话的所有配置选项
 *
 * @author gongdewei 2020/4/15
 */
public class OptionVO {
    /**
     * 选项的级别
     * 用于表示选项的层级或优先级
     */
    private int level;

    /**
     * 选项的数据类型
     * 表示选项值的数据类型，如String、Integer、Boolean等
     */
    private String type;

    /**
     * 选项的名称
     * 选项的唯一标识符，用于在命令中引用该选项
     */
    private String name;

    /**
     * 选项的当前值
     * 存储选项的实际值，以字符串形式表示
     */
    private String value;

    /**
     * 选项的简要说明
     * 提供选项功能的简短描述，用于快速了解选项的用途
     */
    private String summary;

    /**
     * 选项的详细描述
     * 提供选项功能的详细说明，包括使用方法、注意事项等
     */
    private String description;

    /**
     * 获取选项的级别
     *
     * @return 选项的级别值
     */
    public int getLevel() {
        return level;
    }

    /**
     * 设置选项的级别
     *
     * @param level 要设置的级别值
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * 获取选项的数据类型
     *
     * @return 选项的数据类型字符串
     */
    public String getType() {
        return type;
    }

    /**
     * 设置选项的数据类型
     *
     * @param type 选项的数据类型字符串
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取选项的名称
     *
     * @return 选项的名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置选项的名称
     *
     * @param name 选项的名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取选项的简要说明
     *
     * @return 选项的简要说明文本
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 设置选项的简要说明
     *
     * @param summary 选项的简要说明文本
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 获取选项的详细描述
     *
     * @return 选项的详细描述文本
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置选项的详细描述
     *
     * @param description 选项的详细描述文本
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取选项的当前值
     *
     * @return 选项的当前值，以字符串形式表示
     */
    public String getValue() {
        return value;
    }

    /**
     * 设置选项的当前值
     *
     * @param value 选项的值，以字符串形式表示
     */
    public void setValue(String value) {
        this.value = value;
    }
}
