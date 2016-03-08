/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package netty.client.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;


/**
 * Sends one message when a connection is open and echoes back any received
 * data to the server.  Simply put, the echo client initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public final class NettyHelloWorldClient {

  static final boolean SSL = System.getProperty("ssl") != null;
  static final String HOST = System.getProperty("host", "127.0.0.1");
  static final String URL = System.getProperty("url", "/whatever");
  static final int PORT = Integer.parseInt(System.getProperty("port", "8080"));
  static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

  public static void main(String[] args) throws Exception {
    // Configure SSL.git
    final SslContext sslCtx;
    if (SSL) {
      sslCtx = SslContextBuilder.forClient()
          .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    } else {
      sslCtx = null;
    }

    // Configure the client.
    EventLoopGroup group = new NioEventLoopGroup();
    NettyResponseLogger loggingHandler = new NettyResponseLogger();
    try {
      Bootstrap b = new Bootstrap();
      b.group(group)
          .channel(NioSocketChannel.class)
          .option(ChannelOption.TCP_NODELAY, true)
          .remoteAddress(HOST, PORT)
          .handler(new ChannelInitializer<SocketChannel>()
          {
            @Override
            public void initChannel(SocketChannel ch)
                throws Exception
            {
              ChannelPipeline p = ch.pipeline();
              //p.addLast(new LoggingHandler(LogLevel.INFO));
              p.addLast(new HttpClientCodec());
              p.addLast(loggingHandler);
            }
          });

      // Start the client.
      Channel channel = b.connect().syncUninterruptibly().channel();

      channel.writeAndFlush(new DefaultFullHttpRequest(HTTP_1_1, GET, URL)).syncUninterruptibly();
      Thread.sleep(5000);
      channel.writeAndFlush(new DefaultFullHttpRequest(HTTP_1_1, GET, URL)).syncUninterruptibly();
      Thread.sleep(5000);
      channel.writeAndFlush(new DefaultFullHttpRequest(HTTP_1_1, GET, URL)).syncUninterruptibly();

      // Wait until the connection is closed.
      System.err.println("Waiting for connection to be closed.");
      channel.closeFuture().sync();
    } finally {
      // Shut down the event loop to terminate all threads.
      group.shutdownGracefully();
    }
  }
}