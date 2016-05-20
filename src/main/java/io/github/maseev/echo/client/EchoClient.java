package io.github.maseev.echo.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public final class EchoClient {

  public static void main(String[] args) throws Exception {
    final String HOST = System.getProperty("host", "127.0.0.1");
    final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    final int MAX_BUFF_SIZE = 512 * 1024;
    EventLoopGroup group = new NioEventLoopGroup();

    try {
        Bootstrap b = new Bootstrap();
        b.group(group)
         .channel(NioSocketChannel.class)
         .option(ChannelOption.TCP_NODELAY, true)
         .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
         .handler(new ChannelInitializer<SocketChannel>() {
           @Override
           public void initChannel(SocketChannel ch) throws Exception {
             ch.pipeline().addLast(new EchoClientHandler(MAX_BUFF_SIZE));
           }
         });

        ChannelFuture f = b.connect(HOST, PORT).sync();
        f.channel().closeFuture().sync();
    } finally {
        group.shutdownGracefully();
    }
  }
}
