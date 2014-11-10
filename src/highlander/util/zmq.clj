(ns highlander.util.zmq
  (:use [clojure.tools.logging]
        [highlander.util.schedule])
  (:refer-clojure :exclude [send])
  (:import [org.zeromq ZMQ ZMQ$Context ZMQ$Socket ZMQQueue])
  (:require [highlander.util.zhelpers :as mq]
            [highlander.util.qstats :as q]))

(defn- qreceive [queue consume monterval]
  (try                                   ;; avoiding a silent "future" death
    (let [socket (-> mq/single-context 
                     (mq/socket mq/sub))
          monitor (q/monitor monterval)]
        (mq/connect socket queue)
        (mq/subscribe socket)
        (info "[zmq]: consumer is connected to " queue)
        (while true   ;; TODO Have a shutdown hook
          (let [^bytes msg (mq/recv-bytes socket)]
            (swap! q/depth dec)
            (consume msg)))
        (future-cancel monitor))
    (catch Exception e (error "receiver caught: " e))))

(defn- qsend [^ZMQ$Socket socket message]
  (swap! q/depth inc)
  (swap! q/current inc)
  (mq/send-bytes socket message))

(defn- zsocket [queue]
  (let [sock (-> mq/single-context 
                 (mq/socket mq/pub)
                 (mq/bind queue))]
    (info "[zmq]: producer is bound to [" queue "], let's rock'n'roll")
    sock))

(defn pc [consumer queue monterval]
  "inits zmq produce and consume"
  (let [socket (zsocket queue)]
    {:produce (partial qsend socket) 
     :consume (future (qreceive queue consumer monterval))}))
