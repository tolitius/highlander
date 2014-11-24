(ns bench.qpusher
  (:require [highlander.monitor.qstats :as q]
            [highlander.queue.zmq :as zmq]
            [highlander.queue.swpq :as swpq])
  (:use [clojure.tools.cli :only [cli]]
        [clojure.tools.logging]))

(defonce log-every 10000000)
(defonce thing "[Did you know that the Answer to the Ultimate Question of Life, the Universe, and Everything is 42?]")

(defn push-things [push-it number]
  (loop [i number]
    (when (> i 0)
      (push-it)
      (if (= (rem i log-every) 0)
        (info "pushed " (- number i) " things"))
      (recur (dec i)))))

(defn receive [message]
  (Thread/sleep 1))

(defn push-to-queue [{:keys[number produce consume]}]
  (let [thing-bytes (.getBytes thing)
        pusher #(produce thing-bytes)]
    (push-things pusher number)
    (future-cancel consume))) 

(defn plug-and-play [store-it
                     {:keys [queue monterval zqueue qcapacity] :as props}]
  (case queue
    "zmq" (push-to-queue (merge (zmq/pc store-it zqueue monterval) props))
    "swpq" (push-to-queue (merge (swpq/pc store-it qcapacity monterval) props))))

(defn -main [& args]
  (let [[props args usage] 
          (cli args ["-zq" "--zqueue" "use this zmq queue" :default "inproc://zhulk"]
                    ["-q" "--queue" "queue type (e.g. zmq, swpq)" :default "zmq"]
                    ["-n" "--number" "number of things" :parse-fn #(Integer. %) :default 100000000]
                    ["-qc" "--qcapacity" "queue capacity. used for JVM queues" :parse-fn #(Integer. %) :default (* 512 1024 1024)]
                    ["-mi" "--monterval" "queue monitor interval" :default 5])]
    (info usage)
    (plug-and-play #(receive %) props)))
                    
