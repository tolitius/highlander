(ns highlander.util.netty4
  (:use [clojure.tools.logging])
  (:require [highlander.util.qstats :as q])
  (:import
    [java.net InetSocketAddress]
    [java.nio ByteOrder]
    [java.util.concurrent Executors]
    [io.netty.buffer ByteBuf]
    [io.netty.bootstrap ServerBootstrap]
    [io.netty.channel ChannelHandlerContext 
                      ChannelInboundHandlerAdapter
                      ChannelFuture
                      ChannelPipeline
                      ChannelHandler
                      ChannelFuture
                      ChannelInitializer
                      ChannelOption
                      EventLoopGroup]
    [io.netty.channel.nio NioEventLoopGroup]
    [io.netty.channel.socket SocketChannel]
    [io.netty.channel.socket.nio NioServerSocketChannel]
    [io.netty.handler.codec FixedLengthFrameDecoder]))


(defn data-handler [handle-it]
  "receives a message as ByteBuf
   converts it to a byte array
   and handles it with a 'handle-it' function"

  (proxy [ChannelInboundHandlerAdapter] []

    (channelRead [^ChannelHandlerContext ctx 
                  ^ByteBuf msg]
      (let [^ByteBuf buf (cast ByteBuf msg)
            ^bytes data (byte-array (.readableBytes buf))]
        (.readBytes buf data)
        (handle-it data)
        (.release buf)))

    (channelActive [ctx]
      (let [c (.channel ctx)]
        (info "connected: " c)))

    (channelInactive [ctx]
      (let [c (.channel ctx)]
        (info "disconnected: " c)))

    (exceptionCaught [ctx throwable]
      (let [channel (.channel ctx)
            factory (.getFactory channel)]
        (error "[data handler] could not receive a message: " throwable)
        (.printStackTrace throwable)
        (.close ctx)))))

(defn start [handler {:keys [host port]}]
  (let [connection-group (NioEventLoopGroup.)
        worker-group (NioEventLoopGroup.)]
    (try
      (let [bootstrap (ServerBootstrap.)]

        (-> (.group bootstrap connection-group worker-group)
            (.channel NioServerSocketChannel)
            (.childHandler (proxy [ChannelInitializer] []
                             (initChannel [^SocketChannel channel]
                               (let [^ChannelPipeline pipeline (.pipeline channel)]
                                 (.addLast pipeline (into-array ChannelHandler [(FixedLengthFrameDecoder. (int 107))
                                                                                handler]))))))
            (.option (ChannelOption/WRITE_BUFFER_LOW_WATER_MARK) (int (* 64 1024)))
            (.option (ChannelOption/WRITE_BUFFER_HIGH_WATER_MARK) (int (dec (* 2 1024 1024 1024))))
            (.option (ChannelOption/SO_RCVBUF) (int (* 16 1024 1024)))
            (.childOption (ChannelOption/TCP_NODELAY) true)
            (.childOption (ChannelOption/SO_KEEPALIVE) true))

        (-> (.bind bootstrap (InetSocketAddress. host port))
            ((fn [channel] 
               (info "[netty]: highlander is ready for the lightning {:host" host ":port" port "}")
               channel))
            (.sync)
          
            (.channel)     ;; wait until the server socket is closed
            (.closeFuture)
            (.sync)))

      (finally 
        (.shutdownGracefully worker-group)
        (.shutdownGracefully connection-group)))))


