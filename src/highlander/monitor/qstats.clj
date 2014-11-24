(ns highlander.monitor.qstats
  (:import [lmax OneToOneConcurrentArrayQueue3])
  (:use [clojure.tools.logging]
        [highlander.util.schedule]))

(defonce total-rate (atom {}))

(defn rate [interval current previous]
  (double (/ (- current previous) interval)))

(defn stats [id current previous depth interval]
  ;; TODO: consider time unit. for now assuming seconds
  (let [qrate (rate interval current @previous)]
    (swap! total-rate assoc id qrate)
    (reset! previous current)
    (str "\ntotal throughput (ALL partitions): " (reduce + (vals @total-rate)) " msg/s"
         "\n---------------------------------------------"
         "\nqueue  [" id "]"
         "\n---------------------------------------------"
         "\n       message rate: " qrate " msg/s"
         "\n      current depth: " depth
         "\n pass through total: " current)))

(defn z-monitor [id interval]
  "returns a cancelable queue stats monitor"
  (let [[depth current previous] [(atom 0) (atom 0) (atom 0)]]
    {:monitor (every interval #(info (stats id
                                            @current
                                            previous
                                            @depth
                                            interval)))
     :current current
     :depth depth}))                                        ;; zmq has no ".size" (depth visibility)

(defn swpq-monitor [id ^OneToOneConcurrentArrayQueue3 queue interval]
  "returns a cancelable queue stats monitor"
  (let [[current previous] [(atom 0) (atom 0)]]
    {:monitor (every interval #(info (stats id
                                            @current
                                            previous
                                            (.size queue)   ;; ala depth
                                            interval)))
     :current current}))


