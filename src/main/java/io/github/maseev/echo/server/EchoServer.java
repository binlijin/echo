package io.github.maseev.echo.server;

import java.util.logging.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;

public final class EchoServer {

  private final static Logger LOG = Logger.getLogger(EchoServer.class.getName());

  private enum HandlerMode {
    DUMMY,
    BATCHING,
    OPTIMISED
  }

  public static void main(String[] args) throws Exception {
    final int FRAME_LENGTH = 100;
    final int MAX_BUFF_SIZE = 512 * 1024;
    final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    final HandlerMode MODE = fromString(System.getProperty("mode", "DUMMY"));

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
               final ChannelPipeline pipeline = ch.pipeline();

               switch (MODE) {
                 case DUMMY:
                   pipeline.addLast(
                     new FixedLengthFrameDecoder(FRAME_LENGTH),
                     new DummyEchoServerHandler());
                   break;
                 case BATCHING:
                   pipeline.addLast(
                     new FixedLengthFrameDecoder(FRAME_LENGTH),
                     new BatchingEchoServerHandler(MAX_BUFF_SIZE));
                   break;
                 case OPTIMISED:
                   pipeline.addLast(
                     new HighPerformanceEchoServerHandler(FRAME_LENGTH, MAX_BUFF_SIZE));
                   break;
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

  private static HandlerMode fromString(final String mode) {
    try {
      return HandlerMode.valueOf(mode);
    } catch (IllegalArgumentException ex) {
      LOG.warning("Can't find " + mode + " mode. Fallback to " + HandlerMode.DUMMY);
    }

    return HandlerMode.DUMMY;
  }
}
