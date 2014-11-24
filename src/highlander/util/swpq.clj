(ns highlander.util.swpq
  (:use [clojure.tools.logging]
        [highlander.util.schedule])
  (:import [lmax OneToOneConcurrentArrayQueue3])
  (:require [highlander.util.qstats :as q]))

(defn- qsend [queue current message]
  (swap! current inc)
  (.offer queue message))

(defn- qpoll [queue]
  (loop [thing (.poll queue)]
    (if-not thing
      ;;(Thread/yield)
      (recur (.poll queue))
      thing)))

(defn- qreceive [queue consume monitor]
  (try                                    ;; avoiding a silent "future" death
      (info "[swpq]: single write principle queue is ready to roll")
      (while true                         ;; TODO Have a shutdown hook
        (let [msg (qpoll queue)]
          (consume msg)))
      (future-cancel monitor)
    (catch Exception e (error "receiver caught: " e))))

(defn pc [consumer capacity monterval]
  "inits 'single writer principle queue' (swpq) produce and consume"
  (let [queue (OneToOneConcurrentArrayQueue3. capacity)
        thread-id (.getId (Thread/currentThread))
        {:keys [monitor current]} (q/swpq-monitor thread-id queue monterval)]
    {:produce (partial qsend queue current)
     :consume (future (qreceive queue consumer monitor))}))

