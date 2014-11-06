(ns highlander
  (:require [highlander.util.netty :as netty]
            [highlander.util.nio :as nio]
            [highlander.util.zmq :as zmq]
            [highlander.util.swpq :as swpq]
            [highlander.util.redis :as redis])
  (:use [clojure.tools.cli :only [cli]]
        [clojure.tools.logging])
  (:gen-class))

;; For vannila NIO a fixed length frame decoder is used as an example. TODO: Needs to be pluggable
(defn rock-and-roll [{:keys [server host port produce consume fixed-length] :as props}]
  (case server
    "netty" (let [handler (netty/data-handler produce)]
              (netty/start handler props))
    "nio"   (let [handler (partial nio/decode-fixed-lengh-frame produce fixed-length)]
              (nio/start handler props))
  (future-cancel consume)))

(defn plug-and-play [store-it
                     {:keys [queue server monterval zqueue qcapacity host port fixed-length] :as props}]
  (case queue
    "zmq" (rock-and-roll (merge (zmq/pc store-it zqueue monterval) props))
    "swpq" (rock-and-roll (merge (swpq/pc store-it qcapacity monterval) props))))

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
                    ["-fl" "--fixed-length" "fixed length messages" :parse-fn #(Integer. %) :default 107]
                    ["-mi" "--monterval" "queue monitor interval" :parse-fn #(Integer. %) :default 5])]
    (info usage)
    (plug-and-play store-timeseries props)))

