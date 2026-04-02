package com.alibaba.arthas;

/**
 * Pojo（Plain Ordinary Java Object）类
 *
 * 这是一个普通的Java对象类，用于Arthas测试用例中。
 * 该类包含三个属性：姓名、年龄和爱好，并提供了相应的getter和setter方法。
 *
 * 在Arthas的测试场景中，这个类经常被用作演示对象，
 * 用于展示如何使用Arthas命令来查看和修改对象的属性值。
 *
 * @author Arthas Team
 */
public class Pojo {

    /**
     * 姓名属性
     * 用于存储对象的名称信息
     */
    String name;

    /**
     * 年龄属性
     * 用于存储对象的年龄信息，类型为整数
     */
    int age;

    /**
     * 爱好属性
     * 用于存储对象的爱好或兴趣信息
     */
    String hobby;

    /**
     * 获取姓名
     *
     * @return 返回当前对象的姓名
     */
    public String getName() {
        return name;
    }

    /**
     * 设置姓名
     *
     * @param name 要设置的姓名值
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取年龄
     *
     * @return 返回当前对象的年龄
     */
    public int getAge() {
        return age;
    }

    /**
     * 设置年龄
     *
     * @param age 要设置的年龄值
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * 获取爱好
     *
     * @return 返回当前对象的爱好
     */
    public String getHobby() {
        return hobby;
    }

    /**
     * 设置爱好
     *
     * @param hobby 要设置的爱好值
     */
    public void setHobby(String hobby) {
        this.hobby = hobby;
    }
}
