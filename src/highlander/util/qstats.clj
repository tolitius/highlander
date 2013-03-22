(ns highlander.util.qstats
  (:use [clojure.tools.logging]
        [highlander.util.schedule]))

;; zero mq does not allow to peek and see the depth
(def depth (atom 0))
(def current (atom 0))
(def previous (atom 0))

(defn rate [interval current previous]
  (double (/ (- current previous) interval)))

(defn stats [interval] 
  ;; TODO: consider time unit. for now assuming seconds
  (let [qrate (rate interval @current @previous)]
    (reset! previous @current)
    (str "\n       message rate: " qrate " msg/s"
         "\n      current depth: " @depth
         "\n pass through total: " @current)))

(defn monitor [interval]
  "returns a cancelable queue stats monitor"
    (every interval #(info (stats interval))))

