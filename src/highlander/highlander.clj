(ns highlander
  (:require [highlander.util.netty :as netty]
            [highlander.util.zmq :as zmq]
            [highlander.util.swpq :as swpq]
            [highlander.util.redis :as redis])
  (:use [clojure.tools.cli :only [cli]]
        [clojure.tools.logging]))

(defn rock-and-roll [{:keys [host port produce consume] :as props}]
  (let [handler (netty/data-handler produce)]
    (netty/start handler props)
    (future-cancel consume)))

(defn plug-and-play [store-it
                     {:keys [queue monterval zqueue qcapacity host port] :as props}]
  (case queue
    "zmq" (rock-and-roll (merge (zmq/pc store-it zqueue monterval) props))
    "swpq" (rock-and-roll (merge (swpq/pc store-it qcapacity monterval) props))))

(defn store-timeseries [message]
  "an example of a storage fun"
  (let [msg (String. message "UTF-8")]
    (redis/store-kv (System/nanoTime) message)))

(defn -main [& args]
  (let [[props args usage] 
          (cli args ["-h" "--host" "start on this hostname" :default "0.0.0.0"]
                    ["-p" "--port" "listen on this port" :parse-fn #(Integer. %) :default 4242]
                    ["-zq" "--zqueue" "use this zmq queue" :default "inproc://zhulk.ipc"]
                    ;; ["-n" "--number" "number of things to accept" :parse-fn #(Integer. %) :default 100000000]
                    ["-q" "--queue" "queue type [e.g. zmq, swpq]" :default "zmq"]
                    ["-qc" "--qcapacity" "queue capacity. used for JVM queues" :parse-fn #(Integer. %) :default (* 32 1024 1024)]
                    ["-mi" "--monterval" "queue monitor interval" :default 5])]
    (info usage)
    (plug-and-play store-timeseries props)))

