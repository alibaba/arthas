package com.taobao.arthas.core.command.model;

/**
 * <pre>
 * 包装一层，解决json输出问题
 * https://github.com/alibaba/arthas/issues/2261
 * </pre>
 * 
 * @author hengyunabc 2022-08-24
 *
 */
public class ObjectVO {
    private Object object;
    private Integer expand;

    public ObjectVO(Object object, Integer expand) {
        this.object = object;
        this.expand = expand;
    }

    public static ObjectVO[] array(Object[] objects, Integer expand) {
        ObjectVO[] result = new ObjectVO[objects.length];
        for (int i = 0; i < objects.length; ++i) {
            result[i] = new ObjectVO(objects[i], expand);
        }
        return result;
    }

    public int expandOrDefault() {
        if (expand != null) {
            return expand;
        }
        return 1;
    }

    public boolean needExpand() {
        return null != expand && expand > 0;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public Integer getExpand() {
        return expand;
    }

    public void setExpand(Integer expand) {
        this.expand = expand;
    }
}
