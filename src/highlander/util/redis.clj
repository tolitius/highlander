(ns highlander.util.redis
  (:require [taoensso.carmine :as db]))

;; in case we'd need even better performance, fall back on vanilla Jedis

(def pool (db/make-conn-pool ))
(def server (db/make-conn-spec))

(defmacro with-conn [& body]
  `(db/with-conn pool server ~@body))

(defn store-kv [k v]
  (with-conn (db/set k v)))

