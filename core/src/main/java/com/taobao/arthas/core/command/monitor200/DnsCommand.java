package com.taobao.arthas.core.command.monitor200;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;
import sun.net.util.IPAddressUtil;

/**
 * Description:DNS小工具,用于查看/变更运行时DNS配置
 * User: ouzhouyou@raycloud.com
 * Date: 19/6/21
 * Time: 下午4:15
 * Version: 1.0
 */
@Name("dns")
@Summary("Dns config util")
@Description("Examples:\n" +
        "dns list\n" +
        "dns get xx.domain |  dns get taobao.com\n" +
        "dns set xx.domain host expire time(second),default -1L forever time |   dns set taobao.com 127.0.0.1 -1   \n" +
        "dns remove xx.domain | dns remove taobao.com   \n"
)
public class DnsCommand extends AnnotatedCommand {

    //兼容JDK6string使用switch case的情况
    enum ACTION {
        list, get, set, remove
    }

    private String action;
    private String host;
    private String address;
    //默认过期时间为不过期
    private long exp = -1;

    @Argument(argName = "action", index = 0)
    @Description("get/set/list/delete")
    public void setAction(String action) {
        this.action = action;
    }


    @Argument(argName = "domain", index = 1, required = false)
    @Description("domain")
    public void setHost(String host) {
        this.host = host;
    }


    @Argument(argName = "address", index = 2, required = false)
    @Description("ip address")
    public void setAddress(String address) {
        this.address = address;
    }

    @Argument(argName = "exptime", index = 3, required = false)
    @Description(" A value of 0 indicates \"never cache\".\n" +
            " A value of -1 indicates \"cache forever\". ")
    public void setExp(long exp) {
        this.exp = exp;
    }

    /**
     * 检查参数是否合法
     */
    private void check() {
        if (ACTION.list.equals(ACTION.valueOf(action))) {
            return;
        }
        //非List都需要host参数
        if (host == null) {
            throw new IllegalArgumentException("host is expected");
        }
        if (ACTION.set.equals(ACTION.valueOf(action)) && address == null) {
            throw new IllegalArgumentException("address is expected ;\nexample:  dns set a.b.c 127.0.0.1 1800");
        }
    }

    @Override
    public void process(CommandProcess process) {
        RowAffect affect = new RowAffect();
        try {
            check();
            switch (ACTION.valueOf(action)) {
                case list:
                    process.write(dumpDns().toString());
                    break;
                case get:
                    process.write(get(host));
                    break;
                case set: {
                    put(host, address, exp);
                    process.write(get(host));
                    break;
                }
                case remove:
                    remove(host);
                    break;
                default:
                    process.write("not found action!").write("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            process.write("dns process exception:" + e.getMessage()).write("\n");
        } finally {
            process.write(affect.toString()).write("\n");
            process.end();
        }
    }


    public StringBuffer dumpDns() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("hostName").append("\t").append("ip").append("\t").append("expireTime").append("\n");
        try {
            Map cacheFiledOfAddressCacheFiledOfInetAddress = getCacheFiledOfAddressCacheFiledOfInetAddress();
            for (Object o : cacheFiledOfAddressCacheFiledOfInetAddress.entrySet()) {
                if (o instanceof Map.Entry) {
                    Map.Entry entry = (Map.Entry) o;
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    get(buffer, key.toString(), value);
                }
            }
        } catch (Exception e) {
            buffer.append("runtime Exception:").append(e.getMessage());
        }
        return buffer;
    }

    public void put(String domain, String ips, long exp) throws Exception {
        String className = "java.net.InetAddress$CacheEntry";
        Class<?> clazz = Class.forName(className);
        Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object entry = constructor.newInstance(toInetAddressArray(domain, new String[]{ips}), exp < 0 ? -1 : System.currentTimeMillis() + (exp * 1000));

        synchronized (getAddressCacheFieldOfInetAddress()) {
            getCacheFiledOfAddressCacheFiledOfInetAddress().put(domain, entry);
            getCacheFiledOfNegativeCacheFiledOfInetAddress().remove(domain);
        }
    }

    public String get(String domain) throws Exception {
        try {
            InetAddress.getByName(domain);
        } catch (Throwable e) {
            return domain + "\t" + "UnknownHost\n";
        }
        StringBuffer buffer = new StringBuffer();
        get(buffer, domain, getCacheFiledOfAddressCacheFiledOfInetAddress().get(domain));
        return buffer.toString();
    }

    private void get(StringBuffer buffer, String host, Object value) throws NoSuchFieldException, IllegalAccessException {
        if (value == null) {
            buffer.append(host).append("\t").append("\tUnknownHost\n");
            return;
        }
        String ip = null;
        String expireTime = null;
        Field[] fields = value.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getName().contains("address")) {
                fields[i].setAccessible(true);
                Object result = fields[i].get(value);
                if (result.getClass().isArray()) {
                    InetAddress[] inetAddresses = (InetAddress[]) result;
                    ip = inetAddresses[0].getHostAddress();
                }
            }
            if (fields[i].getName().contains("expir")) {
                fields[i].setAccessible(true);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                long expire = ((Long) fields[i].get(value));
                expireTime = format.format(new Date(expire));
            }
        }
        buffer.append(host).append("\t").append(ip).append("\t").append(expireTime).append("\n");
    }

    public void remove(String domain) throws Exception {
        getCacheFiledOfAddressCacheFiledOfInetAddress().remove(domain);
        getCacheFiledOfNegativeCacheFiledOfInetAddress().remove(domain);
    }

    Map<String, Object> getCacheFiledOfNegativeCacheFiledOfInetAddress() throws NoSuchFieldException, IllegalAccessException {
        return getCacheFiledOfInetAddress$Cache0(getNegativeCacheFieldOfInetAddress());
    }

    /**
     * @return {@link InetAddress.Cache#cache} in {@link InetAddress#addressCache}
     */
    Map<String, Object> getCacheFiledOfAddressCacheFiledOfInetAddress() throws
            NoSuchFieldException, IllegalAccessException {
        return getCacheFiledOfInetAddress$Cache0(getAddressCacheFieldOfInetAddress());
    }


    @SuppressWarnings("unchecked")
    Map<String, Object> getCacheFiledOfInetAddress$Cache0(Object inetAddressCache) throws
            NoSuchFieldException, IllegalAccessException {
        Class clazz = inetAddressCache.getClass();
        final Field cacheMapField = clazz.getDeclaredField("cache");
        cacheMapField.setAccessible(true);
        return (Map<String, Object>) cacheMapField.get(inetAddressCache);
    }


    /**
     * @return {@link InetAddress#negativeCache}
     */
    Object getNegativeCacheFieldOfInetAddress() throws NoSuchFieldException, IllegalAccessException {
        return getAddressCacheFieldsOfInetAddress0()[1];
    }


    /**
     * @return {@link InetAddress#addressCache}
     */
    Object getAddressCacheFieldOfInetAddress() throws NoSuchFieldException, IllegalAccessException {
        return getAddressCacheFieldsOfInetAddress0()[0];
    }

    volatile Object[] ADDRESS_CACHE_AND_NEGATIVE_CACHE = null;

    /**
     * @return {@link InetAddress#addressCache} and {@link InetAddress#negativeCache}
     */
    Object[] getAddressCacheFieldsOfInetAddress0() throws NoSuchFieldException, IllegalAccessException {
        if (ADDRESS_CACHE_AND_NEGATIVE_CACHE == null) {
            synchronized (DnsCommand.class) {
                if (ADDRESS_CACHE_AND_NEGATIVE_CACHE == null) {  // double check
                    final Field cacheField = InetAddress.class.getDeclaredField("addressCache");
                    cacheField.setAccessible(true);

                    final Field negativeCacheField = InetAddress.class.getDeclaredField("negativeCache");
                    negativeCacheField.setAccessible(true);
                    ADDRESS_CACHE_AND_NEGATIVE_CACHE = new Object[]{cacheField.get(InetAddress.class), negativeCacheField.get(InetAddress.class)};
                }
            }
        }
        return ADDRESS_CACHE_AND_NEGATIVE_CACHE;
    }


    InetAddress[] toInetAddressArray(String host, String[] ips) throws UnknownHostException {
        InetAddress[] addresses = new InetAddress[ips.length];
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = InetAddress.getByAddress(host, ip2ByteArray(ips[i]));
        }
        return addresses;
    }

    /**
     * dig from InetAddress#getAllByName(java.lang.String, java.net.InetAddress)
     */
    public byte[] ip2ByteArray(String ip) {
        boolean ipv6Expected = false;
        if (ip.charAt(0) == '[') {
            // This is supposed to be an IPv6 literal
            if (ip.length() > 2 && ip.charAt(ip.length() - 1) == ']') {
                ip = ip.substring(1, ip.length() - 1);
                ipv6Expected = true;
            } else {
                // This was supposed to be a IPv6 address, but it's not!
                throw new IllegalArgumentException(ip + ": invalid IPv6 address");
            }
        }

        if (Character.digit(ip.charAt(0), 16) != -1 || (ip.charAt(0) == ':')) {
            // see if it is IPv4 address
            byte[] address = IPAddressUtil.textToNumericFormatV4(ip);
            if (address != null) {
                return address;
            }
            // see if it is IPv6 address
            // Check if a numeric or string zone id is present
            address = IPAddressUtil.textToNumericFormatV6(ip);
            if (address != null) {
                return address;
            }
            if (ipv6Expected) {
                throw new IllegalArgumentException(ip + ": invalid IPv6 address");
            } else {
                throw new IllegalArgumentException(ip + ": invalid IP address");
            }
        } else {
            throw new IllegalArgumentException(ip + ": invalid IP address");
        }
    }
}
