package com.taobao.arthas;

import com.taobao.arthas.protobuf.*;
import com.taobao.arthas.protobuf.utils.MiniTemplator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author: 風楪
 * @date: 2024/6/30 上午1:22
 */
public class Main {

    private static final String TEMPLATE_FILE = "/class_template.tpl";

    public static void main(String[] args) throws IOException {

        String path = Objects.requireNonNull(Main.class.getResource(TEMPLATE_FILE)).getPath();

        MiniTemplator miniTemplator = new MiniTemplator(path);

        miniTemplator.setVariable("importPackage","test");
        miniTemplator.addBlock("imports");
        System.out.println(miniTemplator.generateOutput());
    }
}