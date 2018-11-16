package com.taobao.arthas.core.config;

import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.arthas.core.util.reflect.ArthasReflectUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static java.lang.reflect.Modifier.isStatic;

/**
 * 配置类
 *
 * @author vlinux
 * @author hengyunabc 2018-11-12
 */
public class Configure {
    public static final long DEFAULT_SESSION_TIMEOUT_SECONDS = ShellServerOptions.DEFAULT_SESSION_TIMEOUT/1000;
    private String ip;
    private int telnetPort;
    private int httpPort;
    private int javaPid;
    private String arthasCore;
    private String arthasAgent;

    /**
     * session timeout seconds
     */
    private long sessionTimeout = DEFAULT_SESSION_TIMEOUT_SECONDS;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getTelnetPort() {
        return telnetPort;
    }

    public void setTelnetPort(int telnetPort) {
        this.telnetPort = telnetPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public int getJavaPid() {
        return javaPid;
    }

    public void setJavaPid(int javaPid) {
        this.javaPid = javaPid;
    }

    public String getArthasAgent() {
        return arthasAgent;
    }

    public void setArthasAgent(String arthasAgent) {
        this.arthasAgent = arthasAgent;
    }

    public String getArthasCore() {
        return arthasCore;
    }

    public void setArthasCore(String arthasCore) {
        this.arthasCore = arthasCore;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    // 对象的编码解码器
    private final static FeatureCodec codec = new FeatureCodec(';', '=');

    /**
     * 序列化成字符串
     *
     * @return 序列化字符串
     */
    @Override
    public String toString() {

        final Map<String, String> map = new HashMap<String, String>();
        for (Field field : ArthasReflectUtils.getFields(Configure.class)) {

            // 过滤掉静态类
            if (isStatic(field.getModifiers())) {
                continue;
            }

            // 非静态的才需要纳入非序列化过程
            try {
                map.put(field.getName(), String.valueOf(ArthasReflectUtils.getFieldValueByField(this, field)));
            } catch (Throwable t) {
                //
            }

        }

        return codec.toString(map);
    }

    /**
     * 反序列化字符串成对象
     *
     * @param toString 序列化字符串
     * @return 反序列化的对象
     */
    public static Configure toConfigure(String toString) {
        final Configure configure = new Configure();
        final Map<String, String> map = codec.toMap(toString);

        for (Map.Entry<String, String> entry : map.entrySet()) {
            try {
                final Field field = ArthasReflectUtils.getField(Configure.class, entry.getKey());
                if (null != field && !isStatic(field.getModifiers())) {
                    ArthasReflectUtils.set(field, ArthasReflectUtils.valueOf(field.getType(), entry.getValue()), configure);
                }
            } catch (Throwable t) {
                //
            }
        }
        return configure;
    }

}
