package io.github.maseev.echo.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class EchoClientHandler extends ChannelInboundHandlerAdapter {

  private final ByteBuf message;

  public EchoClientHandler(final int chunkSize) {
    message = Unpooled.directBuffer(chunkSize);

    for (int i = 0; i < message.capacity(); i ++) {
      message.writeByte((byte) i);
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    ctx.writeAndFlush(message.retain(), ctx.voidPromise());
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ctx.write(msg, ctx.voidPromise());
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    message.release();
    ctx.close();
  }
}
