(ns highlander.util.nio
  (:use [clojure.tools.logging])
  (:require [highlander.util.qstats :as q])
  (:import [java.io IOException]
           [java.net InetSocketAddress]
           [java.nio ByteBuffer HeapByteBuffer]
           [java.nio.channels SocketChannel ServerSocketChannel]
           [sun.nio.ch SocketChannelImpl]))

(defn- shutdown [channel] 
  (try 
    (if channel (.close channel))
    (catch IOException ioe (error "[NIO]: Could not close the channel: " (.getMessage ioe)))))

;; TODO: This is for an example sake. 
;;         1. Implement more "FrameDecoders": e.g. http://netty.io/3.6/api/org/jboss/netty/handler/codec/frame/FrameDecoder.html
;;         2. Decouple channel reading from the decoding the frame
(defn decode-fixed-lengh-frame [handle-it 
                                ^Long length 
                                ^SocketChannelImpl channel]   ;; hinting as an experiment
  (let [^HeapByteBuffer bbuffer (ByteBuffer/allocate length)]
    (.read channel bbuffer)
    (handle-it (.array bbuffer))))

;; TODO: A tight loop of no return :) 
;;       Add a shutdown hook 
(defn read-loop [client decode-frame]
  (try 
    (while true
      (decode-frame client))
    (catch Exception e (error "[NIO]: Exception in the read loop: " (.getMessage e)))))

;; TODO: A tight loop of no return :) 
;;       Add a shutdown hook + collect all the client futures to cancel
(defn multiplex-connections [server decode-frame]
  (while true
    (let [client (.accept server)]
      (info "got a client connection " client)
      (future (read-loop client decode-frame)))))

(defn start [handler {:keys [host port]}]
  (let [channel (ServerSocketChannel/open)
        socket (.socket channel)]
    (.bind socket (InetSocketAddress. host port))
    (.setReceiveBufferSize socket (* 16 1024 1024))
    (info "[nio]: highlander is ready for the lightning {:host" host ":port" port "}")
    (multiplex-connections channel handler)))


