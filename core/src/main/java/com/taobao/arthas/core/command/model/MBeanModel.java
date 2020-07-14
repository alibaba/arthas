package com.taobao.arthas.core.command.model;

import javax.management.MBeanInfo;
import java.util.List;
import java.util.Map;

/**
 * Model of 'mbean'
 *
 * @author gongdewei 2020/4/26
 */
public class MBeanModel extends ResultModel {

    private List<String> mbeanNames;

    private Map<String, MBeanInfo> mbeanMetadata;

    private Map<String, List<MBeanAttributeVO>> mbeanAttribute;

    public MBeanModel() {
    }

    public MBeanModel(List<String> mbeanNames) {
        this.mbeanNames = mbeanNames;
    }

    @Override
    public String getType() {
        return "mbean";
    }

    public List<String> getMbeanNames() {
        return mbeanNames;
    }

    public void setMbeanNames(List<String> mbeanNames) {
        this.mbeanNames = mbeanNames;
    }

    public Map<String, MBeanInfo> getMbeanMetadata() {
        return mbeanMetadata;
    }

    public void setMbeanMetadata(Map<String, MBeanInfo> mbeanMetadata) {
        this.mbeanMetadata = mbeanMetadata;
    }

    public Map<String, List<MBeanAttributeVO>> getMbeanAttribute() {
        return mbeanAttribute;
    }

    public void setMbeanAttribute(Map<String, List<MBeanAttributeVO>> mbeanAttribute) {
        this.mbeanAttribute = mbeanAttribute;
    }
}
