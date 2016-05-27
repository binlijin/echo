package io.github.maseev.echo.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class BatchingEchoServerHandler extends ChannelInboundHandlerAdapter {

  private ByteBuf buffer;

  private int size;

  public BatchingEchoServerHandler(int size) {
    this.size = size;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    buffer = ctx.alloc().buffer(size);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object obj) throws Exception {
    final ByteBuf msg = (ByteBuf) obj;

    try {
      buffer.writeBytes(msg);
    } finally {
      msg.release();
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.writeAndFlush(buffer, ctx.voidPromise());
    buffer = ctx.alloc().buffer(size);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
