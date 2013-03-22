(ns highlander.util.swpq
  (:use [clojure.tools.logging]
        [highlander.util.schedule])
  (:import [lmax OneToOneConcurrentArrayQueue3]
           [org.jboss.netty.buffer BigEndianHeapChannelBuffer])
  (:require [highlander.util.qstats :as q]))

(defn- qsend [queue message]
  (swap! q/depth inc)
  (swap! q/current inc)
  (.offer queue message))

(defn- qpoll [queue]
  (loop [thing (.poll queue)]
    (if-not thing
      ;;(Thread/yield)
      (recur (.poll queue))
      thing)))

(defn- qreceive [queue consume monterval]
  (try                                    ;; avoiding a silent "future" death
    (let [monitor (q/monitor monterval)]
      (info "[swpq]: single write principle queue is ready to roll")
      (while true                         ;; TODO Have a shutdown hook
        (let [msg (qpoll queue)]
          (swap! q/depth dec)
          (consume msg)))
      (future-cancel monitor))
    (catch Exception e (error "receiver caught: " e))))

(defn pc [consumer capacity monterval]
  "inits 'single writer principle queue' (swpq) produce and consume"
  (let [queue (OneToOneConcurrentArrayQueue3. capacity)]
    {:produce (partial qsend queue) 
     :consume (future (qreceive queue consumer monterval))}))

