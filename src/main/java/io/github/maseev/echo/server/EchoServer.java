package io.github.maseev.echo.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;

public final class EchoServer {

  public static void main(String[] args) throws Exception {
    final int FRAME_LENGTH = 100;
    final int MAX_BUFF_SIZE = 512 * 1024;
    final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    final boolean OPTIMIZED = Boolean.parseBoolean(System.getProperty("optimized", "true"));

    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
        ServerBootstrap b = new ServerBootstrap();

        b.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
         .childOption(ChannelOption.TCP_NODELAY, true)
         .childOption(ChannelOption.RCVBUF_ALLOCATOR,
           new AdaptiveRecvByteBufAllocator(64, 1024, MAX_BUFF_SIZE))
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             public void initChannel(SocketChannel ch) throws Exception {
               if (OPTIMIZED) {
                 ch.pipeline().addLast(new EchoServerHandler(FRAME_LENGTH, MAX_BUFF_SIZE));
               } else {
                 ch.pipeline()
                   .addLast(new FixedLengthFrameDecoder(FRAME_LENGTH), new DummyEchoServerHandler());
               }
             }
         });

        ChannelFuture f = b.bind(PORT).sync();

        f.channel().closeFuture().sync();
    } finally {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
  }
}
