(ns highlander
  (:require [highlander.server.netty4 :as netty]
            [highlander.server.nio :as nio]
            [highlander.queue.zmq :as zmq]
            [highlander.queue.swpq :as swpq]
            [highlander.store.redis :as redis]
            [clojure.edn :as edn])
  (:use [clojure.tools.cli :only [cli]]
        [clojure.tools.logging])
  (:import [io.netty.handler.codec FixedLengthFrameDecoder])
  (:gen-class))

;; For vannila NIO a fixed length frame decoder is used as an example. TODO: Needs to be pluggable
(defn rock-and-roll [handler
                     {:keys [server fixed-length] :as props}]
  (case server
    "netty" (netty/start handler (merge props))
    "nio" (nio/start handler props)
    ;; (future-cancel consume) ;; TODO: think about multi threaded "consume" access here (via promise?)
    ))

(defn plug-and-play [store-it
                     {:keys [queue monterval zqueue qcapacity] :as props}]
  (case queue
    "zmq" (rock-and-roll #(zmq/pc store-it zqueue monterval) props)
    "swpq" (rock-and-roll #(swpq/pc store-it qcapacity monterval) props)))

(defn store-timeseries [message]
  "an example of a storage fun"
;;  (let [msg (String. (.getBytes message "UTF-8"))]
    (redis/store-kv (System/nanoTime) message))

(defn -main [& args]
  (let [[props args usage] 
          (cli args ["-h" "--host" "start on this hostname" :default "0.0.0.0"]
                    ["-p" "--port" "listen on this port" :parse-fn #(Integer. %) :default 4242]
                    ["-zq" "--zqueue" "use this zmq queue" :default "inproc://zhulk.ipc"]
                    ;; ["-n" "--number" "number of things to accept" :parse-fn #(Integer. %) :default 100000000]
                    ["-q" "--queue" "queue type [e.g. zmq, swpq]" :default "zmq"]
                    ["-qc" "--qcapacity" "queue capacity. used for JVM queues" :parse-fn #(Integer. %) :default (* 32 1024 1024)]
                    ["-s" "--server" "server type [e.g. netty, nio]" :default "netty"]
                    ["-fd" "--frame-decoder" "netty message frame decoder" :parse-fn #(eval (edn/read-string %)) :default #(FixedLengthFrameDecoder. (int 100))]
                    ["-fl" "--fixed-length" "fixed length messages" :parse-fn #(Integer. %) :default 100] ;; @depricated (currently used to make NIO pass)
                    ["-mi" "--monterval" "queue monitor interval" :parse-fn #(Integer. %) :default 5])]
    (info usage)
    (plug-and-play store-timeseries props)))
