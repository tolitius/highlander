(ns highlander.util.redis
  (:require [taoensso.carmine :as db])
  (:use lasync))

;; in case we'd need even better performance, fall back on vanilla Jedis

(def pool (db/make-conn-pool ))
(def server (db/make-conn-spec))

;; lasync thread pool
(def ^{:private true} tpool (limit-pool))

(comment
  (defmacro with-conn [& body]
    "with thread pool.
     observations: message rate goes down, but persistence rate goes up => redis likes concurrency
                   hence, tpool is useful in case a higher persistence vs. consumption throughput is needed"
    `(.submit tpool 
              #(db/with-conn pool server ~@body))))

(defmacro with-conn [& body]
  `(db/with-conn pool server ~@body))

(defn store-kv [k v]
  (with-conn (db/set k v)))

