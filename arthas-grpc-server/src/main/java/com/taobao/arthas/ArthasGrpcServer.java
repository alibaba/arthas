package com.taobao.arthas;/**
 * @author: 風楪
 * @date: 2024/7/3 上午12:30
 */

import com.taobao.arthas.h2.Http2Handler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author: FengYe
 * @date: 2024/7/3 上午12:30
 * @description: ArthasGrpcServer
 */
public class ArthasGrpcServer {
    public static void main(String[] args) throws Exception {
        //自签名生成密钥
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                .build();


        // 指定生成自签名证书和密钥的位置
        File certFile = new File(System.getProperty("user.dir"),"certificate.crt");
        File keyFile = new File(System.getProperty("user.dir"),"privateKey.key");

        // 将生成的证书和私钥移动到指定位置
        moveFile(ssc.certificate(), certFile);
        moveFile(ssc.privateKey(), keyFile);

        System.out.println(certFile.getAbsolutePath());
        System.out.println(keyFile.getAbsolutePath());

        System.out.println("Certificate: " + ssc.certificate());
        System.out.println("Private Key: " + ssc.privateKey());

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
//                            ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
                            ch.pipeline().addLast(Http2FrameCodecBuilder.forServer().build());
                            ch.pipeline().addLast(new Http2Handler());
                        }
                    });

            // Bind and start to accept incoming connections.
            b.bind(9090).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static void moveFile(File source, File target) throws IOException {
        if (!target.getParentFile().exists()) {
            target.getParentFile().mkdirs();
        }
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
