package com.taobao.arthas.grpcweb.demo;

import com.taobao.arthas.grpcweb.demo.server.GrpcServer;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try{
            GrpcServer grpcServer = new GrpcServer(8566);
            grpcServer.start();
            System.in.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
