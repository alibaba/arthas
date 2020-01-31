package com.taobao.arthas.core.spi.impl;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSONObject;
import com.taobao.arthas.core.spi.ServerConnectorMetricsProvider;
import com.taobao.arthas.core.util.StringUtils;

/**
 * JMX方式监控tomcat Connector/Thread
 * 
 * @author qxo
 * @date 2020/01/31
 */
public class ServerConnectorMetricsProvider4TomcatJmx implements ServerConnectorMetricsProvider {

    private static final Logger logger = LoggerFactory.getLogger(ServerConnectorMetricsProvider4TomcatJmx.class);

    static final boolean IS_TOMCAT = System.getProperty("catalina.base") != null;
    private static final Pattern NAME_PATTERN = Pattern.compile(",name=[\"]?([^=,\"]+)[\"]?");

    // mbean 'Catalina:type=ThreadPool,name="http-bio-8080"'
    // mbean 'Catalina:type=GlobalRequestProcessor,name="http-bio-8080"'

    private static final class Const {
        static final Set<ObjectName> THREAD_POOL_NAMES;
        static final Set<ObjectName> GLOBAL_REQUEST_PROCESSOR_NAMES;
        static final Map<String, String> THREAD_POOL_ATTR_MAPPING = splitToMap(
                "name=name,currentThreadsBusy=threadBusy,currentThreadCount=threadCount");
        static final Map<String, String> GLOBAL_REQUEST_PROCESSOR_ATTR_MAPPING = splitToMap(
                "name,bytesReceived,bytesSent,processingTime,requestCount,errorCount");
        static {
            THREAD_POOL_NAMES = new HashSet<ObjectName>();
            GLOBAL_REQUEST_PROCESSOR_NAMES = new HashSet<ObjectName>();
            Set<ObjectName> names = queryObjectNames("Catalina:*");
            for (ObjectName objectName : names) {
                String name = objectName.toString();
                if (name.startsWith("Catalina:type=ThreadPool,") && name.indexOf(",subType=") == -1) {
                    THREAD_POOL_NAMES.add(objectName);
                    continue;
                }
                if (name.startsWith("Catalina:type=GlobalRequestProcessor,")) {
                    GLOBAL_REQUEST_PROCESSOR_NAMES.add(objectName);
                    continue;
                }
            }
//            System.out.println("THREAD_POOL_NAMES:" + THREAD_POOL_NAMES);
//            System.out.println("GLOBAL_REQUEST_PROCESSOR_NAMES:" + GLOBAL_REQUEST_PROCESSOR_NAMES);
//            System.out.println("THREAD_POOL_ATTR_MAPPING:" + THREAD_POOL_ATTR_MAPPING);
//            System.out.println("GLOBAL_REQUEST_PROCESSOR_ATTR_MAPPING:" + GLOBAL_REQUEST_PROCESSOR_ATTR_MAPPING);
        }

        private static Map<String, String> splitToMap(final String str) {
            final Map<String, String> ret = new HashMap<String, String>();
            String[] arr = str.split(",");
            for (String a : arr) {
                final int idx = a.indexOf('=');
                if (idx == -1) {
                    ret.put(a, a);
                } else {
                    String key = a.substring(0, idx);
                    String value = a.substring(idx + 1);
                    ret.put(key, value);
                }
            }
            return ret;
        }
    }

    @Override
    public boolean isMetricOn() {
        return IS_TOMCAT && Const.THREAD_POOL_NAMES.size() > 0;
    }

    private static Set<ObjectName> queryObjectNames(String name) {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> objectNames = new HashSet<ObjectName>();
        try {
            if (StringUtils.isEmpty(name)) {
                name = "*:*";
            }
            objectNames = platformMBeanServer.queryNames(new ObjectName(name), null);
        } catch (MalformedObjectNameException e) {
            logger.warn("queryObjectNames error", e);
        }
        return objectNames;
    }

    private List<JSONObject> queryByNames(Set<ObjectName> objectNames, Map<String, String> attrs) {
        final List<JSONObject> retList = new ArrayList<JSONObject>();
        try {
            MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            for (ObjectName objectName : objectNames) {
                MBeanInfo mBeanInfo = platformMBeanServer.getMBeanInfo(objectName);
                MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();
                JSONObject json = new JSONObject();
                for (MBeanAttributeInfo attribute : attributes) {
                    String attributeName = attribute.getName();
                    String key = attrs.get(attributeName);
                    if (key == null) {
                        continue;
                    }
                    //System.out.println("attributeName:"+attributeName);
                    Object value;
                    if (!attribute.isReadable()) {
                        continue;
                    } else {
                        Object attributeObj = platformMBeanServer.getAttribute(objectName, attributeName);
                        value = attributeObj;// String.valueOf(attributeObj);
                    }
                    json.put(key, value);
                }
              
                if(!json.containsKey("name")) {
                    String nameStr = objectName.toString();
                    Matcher matcher = NAME_PATTERN.matcher(nameStr);
                    String name;
                    if (matcher.find()) {
                        name = matcher.group(1);
                    } else {
                        name = nameStr;
                    }
                    json.put("name", name);
                }
                retList.add(json);
            }
        } catch (Throwable e) {
            logger.warn("mbean error", e);
        }
        //System.out.println("objectNames: " + objectNames + " =>"+retList);
        return retList;
    }

    @Override
    public List<JSONObject> getConnectorStats() {
        return queryByNames(Const.GLOBAL_REQUEST_PROCESSOR_NAMES, Const.GLOBAL_REQUEST_PROCESSOR_ATTR_MAPPING);
    }

    @Override
    public List<JSONObject> getThreadPoolInfos() {
        return queryByNames(Const.THREAD_POOL_NAMES, Const.THREAD_POOL_ATTR_MAPPING);
    }
}
