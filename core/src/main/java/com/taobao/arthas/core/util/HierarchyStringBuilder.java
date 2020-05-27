package com.taobao.arthas.core.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多层级字符串拼接工具，多次拼接相同的字符串可以减少字符串拼接的小对象节省内存。
 *
 * @author gongdewei 2020/5/27
 */
public class HierarchyStringBuilder {
    private Map<String, Node> rootNodeMap = new ConcurrentHashMap<String, Node>();
    private String separator;

    public HierarchyStringBuilder(String separator) {
        this.separator = separator;
    }

    public String concat(String str1, String str2) {
        Node node = getRootNode(str1);
        node = node.append(str2);
        return node.getValue();
    }

    public String concat(String str1, String str2, String str3) {
        Node node = getRootNode(str1);
        node = node.append(str2);
        node = node.append(str3);
        return node.getValue();
    }

    public String concat(String str1, String str2, String str3, String str4) {
        Node node = getRootNode(str1);
        node = node.append(str2);
        node = node.append(str3);
        node = node.append(str4);
        return node.getValue();
    }

    private Node getRootNode(String str) {
        Node node = rootNodeMap.get(str);
        if (node == null) {
            node = new Node(str, separator);
            rootNodeMap.put(str, node);
        }
        return node;
    }

    private static class Node {
        private String value;
        private String separator;
        private Map<String, Node> children = new ConcurrentHashMap<String, Node>();

        public Node(String value, String separator) {
            this.value = value;
            this.separator = separator;
        }

        public Node append(String str) {
            Node child = children.get(str);
            if (child == null) {
                return newNode(str);
            }
            return child;
        }

        private Node newNode(String str) {
            synchronized (this) {
                Node child = children.get(str);
                if (child == null) {
                    String childValue;
                    if (separator != null) {
                        // childValue = value + separator + str;
                        StringBuilder sb = new StringBuilder(value.length()+separator.length()+str.length());
                        sb.append(value).append(separator).append(str);
                        childValue = sb.toString();
                    } else {
                        //childValue = value + str;
                        StringBuilder sb = new StringBuilder(value.length()+str.length());
                        sb.append(value).append(str);
                        childValue = sb.toString();
                    }
                    child = new Node(childValue, separator);
                    children.put(str, child);
                }
                return child;
            }
        }

        public String getValue() {
            return value;
        }
    }
}
