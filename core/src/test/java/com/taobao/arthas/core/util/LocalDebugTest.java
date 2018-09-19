package com.taobao.arthas.core.util;

import com.taobao.arthas.core.Arthas;
import sun.management.VMManagement;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * 本机调试
 *
 * @author BlueT
 * 2018/9/17 23:34
 */
public class LocalDebugTest {

    private static final Random RANDOM=new Random();

    /**
     * 首先执行./mvnw clean package -DskipTests打包，生成的zip在 packaging/target/ 下面，然后解压。
     * 以debug方式执行此方法，如果出现NPE，将tools.jar添加到classpath中。
     * 然后运行as.sh
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        final int pid = getCurrentJVMPid();
        System.out.println("pid:" + pid);
        String path = LocalDebugTest.class.getResource("/").getPath();
        final String npath = path.substring(1, path.indexOf("core")) + "packaging/target/";
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Arthas.main(new String[]{
                        "-jar",
                        npath + "arthas-core.jar",
                        "-pid",
                        pid + "",
                        "-target-ip",
                        "127.0.0.1",
                        //"-telnet-port",
                        //"3658",
                        //"-http-port",
                        //"8563",
                        "-core",
                        npath + "arthas-core.jar",
                        "-agent",
                        npath + "arthas-agent.jar"
                });
            }
        });
        thread.start();
        thread.join();
        System.out.println("代码植入成功");
        while (true) {
            // ignore return
            int i = forLoop();
            Thread.sleep(2000);
        }
    }

    private static int getCurrentJVMPid() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        try {
            Field jvm = runtime.getClass().getDeclaredField("jvm");
            jvm.setAccessible(true);
            VMManagement mgmt = (VMManagement) jvm.get(runtime);
            Method pid_method = mgmt.getClass().getDeclaredMethod("getProcessId");
            pid_method.setAccessible(true);
            return (int) (Integer) pid_method.invoke(mgmt);
        } catch (Exception e) {
            e.printStackTrace();
        }

        throw new RuntimeException("Wow!");

    }

   private static int forLoop(){
       return RANDOM.nextInt();
   }
}
