package com.alibaba.arthas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author diecui1202 on 2017/9/13.
 */
public class Test {

    public static final Map m = new HashMap();
    public static final Map n = new HashMap();

    public static final Map p = null;

    static {
        m.put("a", "aaa");
        m.put("b", "bbb");

        n.put(Type.RUN, "aaa");
        n.put(Type.STOP, "bbb");
    }

    public static void main(String[] args) throws InterruptedException {
        List<Pojo> list = new ArrayList<Pojo>();

        for (int i = 0; i < 40; i ++) {
            Pojo pojo = new Pojo();
            pojo.setName("name " + i);
            pojo.setAge(i + 2);

            list.add(pojo);
        }

        System.out.println(p);

        while (true) {
            int random = new Random().nextInt(40);
            String name = list.get(random).getName();
            list.get(random).setName(null);
            test(list);
            list.get(random).setName(name);
            Thread.sleep(1000L);
        }
    }

    public static void test(List<Pojo> list) {
        // do nothing
    }

    public static void invoke(String a) {
        System.out.println(a);
    }
}
