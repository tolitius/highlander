(ns highlander.util.netty
  (:use [clojure.tools.logging])
  (:require [highlander.util.qstats :as q])
  (:import
    [java.net InetSocketAddress]
    [java.nio ByteOrder]
    [java.util.concurrent Executors]
    [org.jboss.netty.bootstrap ServerBootstrap]
    [org.jboss.netty.handler.codec.frame FixedLengthFrameDecoder]
    [org.jboss.netty.channel ChannelHandlerContext MessageEvent Channels ChannelPipelineFactory SimpleChannelHandler]
    [org.jboss.netty.channel.socket.nio NioServerSocketChannelFactory]
    [org.jboss.netty.buffer HeapChannelBuffer HeapChannelBufferFactory ChannelBuffers]))

(defn- shutdown [factory] 
  "shutting down netty in a different thread not to cause deadlocks"
  (.start (Thread. 
            #(.releaseExternalResources factory))))

;; TODO: Pass through a FrameDecoder
(defn data-handler [handle-it]
  "receives a message as HeapChannelBuffer
   converts it to a byte array
   and handles it with a 'handle-it' function"

  (proxy [SimpleChannelHandler] []

    (messageReceived [^ChannelHandlerContext ctx 
                      ^MessageEvent event]
      (-> (cast HeapChannelBuffer (.getMessage event))
          (.array)
          (handle-it)))

    (channelConnected [ctx e]
      (let [c (.getChannel e)]
        (info "connected: " c)))

    (channelDisconnected [ctx e]
      (let [c (.getChannel e)]
        (info "disconnected: " c)))

    (exceptionCaught
      [ctx exc-event]
      (let [throwable (.getCause exc-event)
            channel (.getChannel exc-event)
            factory (.getFactory channel)]
        (error "[data handler] could not receive a message: " throwable)
        (.printStackTrace throwable)
        (.close channel)
        (shutdown factory)))))

(defn start [handler {:keys [host port]}]
  (let [channel-factory (NioServerSocketChannelFactory.
                          (Executors/newCachedThreadPool)
                          (Executors/newCachedThreadPool))
        bootstrap (ServerBootstrap. channel-factory)
        pipeline (.getPipeline bootstrap)]
    (.addLast pipeline "framer" (FixedLengthFrameDecoder. (int 107))) ;; decoder should be injected
    (.addLast pipeline "handler" handler)
    (.setOption bootstrap "child.tcpNoDelay", true)
    (.setOption bootstrap "child.keepAlive", true)
    (.setOption bootstrap "writeBufferLowWaterMark",      (* 10 1024 1024))
    (.setOption bootstrap "writeBufferHighWaterMark", (* 2 1024 1024 1024))
    ;; (.setOption bootstrap "child.bufferFactory", (HeapChannelBufferFactory. ByteOrder/LITTLE_ENDIAN))
    (.setOption bootstrap "receiveBufferSize", (* 16 1024 1024)) ;; TODO: make it configurable
    (.bind bootstrap (InetSocketAddress. host port))
    (info "[netty]: highlander is ready for the lightning {:host" host ":port" port "}")
    pipeline))

