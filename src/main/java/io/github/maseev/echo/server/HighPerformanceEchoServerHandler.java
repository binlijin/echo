package io.github.maseev.echo.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class HighPerformanceEchoServerHandler extends ChannelInboundHandlerAdapter {

  private final int frameSize;

  private final ByteBuf buffer;

  public HighPerformanceEchoServerHandler(final int frameSize, final int bufferSize) {
    this.frameSize = frameSize;
    buffer = PooledByteBufAllocator.DEFAULT.directBuffer(bufferSize);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    buffer.release();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    final ByteBuf msgBuff = (ByteBuf) msg;

    try {
      buffer.writeBytes(msgBuff);
    } finally {
      msgBuff.release();
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    int readableBytes = buffer.readableBytes();
    int length = readableBytes == 0 ? 0 : (readableBytes / frameSize) * frameSize;

    if (length > 0) {
      final ByteBuf result = ctx.alloc().directBuffer(length, length).writeBytes(buffer, length);
      buffer.discardReadBytes();
      ctx.writeAndFlush(result, ctx.voidPromise());
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      buffer.release();
      cause.printStackTrace();
      ctx.close();
  }
}
