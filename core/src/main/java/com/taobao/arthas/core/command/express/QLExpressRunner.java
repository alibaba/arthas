package com.taobao.arthas.core.command.express;


import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;

/**
 * @Author TaoKan
 * @Date 2024/9/22 12:20 PM
 */
public class QLExpressRunner {
    private volatile static QLExpressRunner instance = null;
    private Express4Runner expressRunner;

    private QLExpressRunner(){
        expressRunner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);
    }

    //对外提供静态方法获取对象
    public static Express4Runner getInstance(){
        //第一次判断,如果instance不为null,不进入抢锁阶段,直接返回实例
        if(instance == null){
            synchronized (QLExpressRunner.class){
                //抢到锁之后再次进行判断是否为null
                if(instance == null){
                    instance = new QLExpressRunner();
                }
            }
        }
        return instance.expressRunner;
    }


}
