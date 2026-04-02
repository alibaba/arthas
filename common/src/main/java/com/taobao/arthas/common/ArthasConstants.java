package com.taobao.arthas.common;

/**
 * Arthas常量定义类
 * 用于存储Arthas系统中使用的各种常量值
 *
 * @author hengyunabc 2020-09-02
 *
 */
public class ArthasConstants {
    /**
     * VM内部通信使用的本地地址
     * 用于Netty本地通道通信
     *
     * @see io.netty.channel.local.LocalAddress
     * @see io.netty.channel.local.LocalChannel
     */
    public static final String NETTY_LOCAL_ADDRESS = "arthas-netty-LocalAddress";

    /**
     * HTTP内容最大长度限制
     * 单位：字节，默认为10MB (1024 * 1024 * 10)
     */
    public static final int MAX_HTTP_CONTENT_LENGTH = 1024 * 1024 * 10;

    /**
     * Arthas输出目录名称
     * 用于存放Arthas运行时生成的输出文件
     */
    public static final String ARTHAS_OUTPUT = "arthas-output";

    /**
     * 应用名称配置键
     * 用于标识或配置应用程序名称
     */
    public static final String APP_NAME = "app-name";

    /**
     * 项目名称配置键
     * 用于从配置文件中读取项目名称
     */
    public static final String PROJECT_NAME = "project.name";

    /**
     * Spring应用名称配置键
     * 用于从Spring配置中读取应用名称
     */
    public static final String SPRING_APPLICATION_NAME = "spring.application.name";

    /**
     * Telnet服务默认端口
     * Arthas Telnet服务器监听的端口号
     */
    public static final int TELNET_PORT = 3658;

    /**
     * WebSocket默认路径
     * WebSocket服务的默认访问路径
     */
    public static final String DEFAULT_WEBSOCKET_PATH = "/ws";

    /**
     * WebSocket空闲连接超时时间
     * 单位：秒，默认为10秒
     */
    public static final int WEBSOCKET_IDLE_SECONDS = 10;

    /**
     * HTTP会话Cookie键名
     * 用于标识和管理用户的HTTP会话
     */
    public static final String ASESSION_KEY = "asession";

    /**
     * 默认用户名
     * Arthas默认登录用户名
     */
    public static final String DEFAULT_USERNAME = "arthas";

    /**
     * 主题键名
     * 用于认证系统中存储用户主体信息
     */
    public static final String SUBJECT_KEY = "subject";

    /**
     * 认证键名
     * 用于标识认证相关的配置或参数
     */
    public static final String AUTH = "auth";

    /**
     * 用户名键名
     * 用于在请求或配置中传递用户名
     */
    public static final String USERNAME_KEY = "username";

    /**
     * 密码键名
     * 用于在请求或配置中传递密码
     */
    public static final String PASSWORD_KEY = "password";

    /**
     * 用户ID键名
     * 用于在请求或配置中传递用户唯一标识
     */
    public static final String USER_ID_KEY = "userId";
}
