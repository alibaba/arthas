package com.taobao.arthas.core.util;

import com.alibaba.fastjson.JSON;
import com.taobao.arthas.core.GlobalOptions;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * options util methods
 * @author gongdewei 3/25/19 7:34 PM
 */
public class OptionsUtils {

    public static void saveOptions(File file) {
        OutputStream out = null;
        try {
            Map<String, Object> map = getOptionsMap();
            String json = JSON.toJSONString(map, true);
            out = FileUtils.openOutputStream(file, false);
            out.write(json.getBytes("utf-8"));
        } catch (Exception e) {
            // ignore
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    public static void loadOptions(File file){
        BufferedReader br = null;
        StringBuilder sbJson = new StringBuilder(128);
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
            String line;
            while ((line = br.readLine()) != null) {
                sbJson.append(line.trim());
            }
            //convert json string to map
            Map<String, Object> map = JSON.parseObject(sbJson.toString());
            setOptions(map);
        } catch (Exception e) {
            // ignore
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    private static void setOptions(Map<String, Object> map) {
        try {
            Field[] fields = GlobalOptions.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                field.set(null, map.get(field.getName()));
                field.setAccessible(false);
            }
        } catch (IllegalAccessException e) {
        }
    }

    private static Map<String, Object> getOptionsMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        try {
            Field[] fields = GlobalOptions.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                map.put(field.getName(), field.get(null));
                field.setAccessible(false);
            }
        } catch (IllegalAccessException e) {
        }
        return map;
    }

}
