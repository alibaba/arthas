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

    public static final Map<String, String> m = new HashMap<>();
    public static final Map<Type, String> n = new HashMap<>();
    public static final Map<?, ?> p = new HashMap<>();

    static {
        m.put("a", "aaa");
        m.put("b", "bbb");

        n.put(Type.RUN, "aaa");
        n.put(Type.STOP, "bbb");
    }

    public static void main(String[] args) {
        List<Pojo> list = new ArrayList<>();

        for (int i = 0; i < 40; i++) {
            Pojo pojo = new Pojo();
            pojo.setName("name " + i);
            pojo.setAge(i + 2);

            list.add(pojo);
        }

        System.out.println(p);

        Random random = new Random();
        while (true) {
            int randomNumber = random.nextInt(40);
            String name = list.get(randomNumber).getName();
            list.get(randomNumber).setName(null);
            test(list);
            list.get(randomNumber).setName(name);
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break; // handle InterruptedException and exit
            }
        }
    }

    public static void test(List<Pojo> list) {
        // do nothing
    }

    public static void invoke(String a) {
        System.out.println(a);
    }
}
