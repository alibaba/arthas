package com.taobao.arthas.grpcweb.grpc;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: arthas
 * *
 * @author: XY
 * @create: 2023-09-22 14:12
 **/

public class Test {

    private int number;

    private Map<String,Object> mapExample;

    public Test(){
        this.number = 10086;
        this.mapExample = null;
    }

    public Test(int number){
        this.number = number;
        Map<String, Object> map = new HashMap<>();
        map.put("1","111");

        for(int i =0; i < number; i++){
            map.put(String.valueOf(i),"666");
            Map<String, Object> newMap = new HashMap<>();
            newMap.put("656",map);
            map = newMap;
        }
        this.mapExample = map;
    }



    public int getNumber() {
        return number;
    }

    public Map<String, Object> getMapExample() {
        return mapExample;
    }

    public static void main(String[] args) {
        Test test = new Test(4);
        System.out.println(test.getMapExample());
    }
}
