package com.taobao.arthas.grpc.server;

import com.taobao.arthas.grpc.server.temp.TempImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: 風楪
 * @date: 2024/6/30 上午1:22
 */
public class Main {

    private static final String TEMPLATE_FILE = "/class_template.tpl";

    private static Server server;

    public static void main(String[] args) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        Method plus1 = Main.class.getDeclaredMethod("plus", int.class, int.class);

        MethodHandle unreflect = lookup.unreflect(plus1);

        System.out.println(unreflect.invoke(new Main(), 1, 2));
    }

    private static List<Class<?>> findClasses(String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        try {
            URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
            if (resource != null) {
                File directory = new File(resource.toURI());
                if (directory.exists()) {
                    for (File file : directory.listFiles()) {
                        if (file.isFile() && file.getName().endsWith(".class")) {
                            String className = packageName + '.' + file.getName().replace(".class", "");
                            classes.add(Class.forName(className));
                        }
                    }
                }
            }
        } catch (Exception e) {

        }
        return classes;
    }

    public int plus(int a,int b){
        return a+b;
    }

    public static void start() throws IOException {
        ServerBuilder builder = ServerBuilder.forPort(9090)
                .addService(new TempImpl());
        server = builder.build();
        server.start();
    }

    public void blockUntilShutdown() throws InterruptedException {
        server.awaitTermination();
    }
}