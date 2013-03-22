(ns highlander.util.zhelpers
  (:refer-clojure :exclude [send])
  (:import [org.zeromq ZMQ ZMQ$Context ZMQ$Socket ZMQQueue])
  (:use [clojure.tools.logging]))

(defonce single-context 
  (ZMQ/context 1))

(defn context [threads]
  (ZMQ/context threads))

(def router ZMQ/XREP)
(def dealer ZMQ/XREQ)
(def req ZMQ/REQ)
(def rep ZMQ/REP)
(def xreq ZMQ/XREQ)
(def xrep ZMQ/XREP)
(def pub ZMQ/PUB)
(def sub ZMQ/SUB)
(def pair ZMQ/PAIR)
(def push ZMQ/PUSH)
(def pull ZMQ/PULL)

(defn socket
  [#^ZMQ$Context context type]
  (.socket context type))

(defn queue
  [#^ZMQ$Context context #^ZMQ$Socket frontend #^ZMQ$Socket backend]
  (ZMQQueue. context frontend backend))

(defn bind
  [#^ZMQ$Socket socket url]
  (doto socket
    (.bind url)))

(defn connect
  [#^ZMQ$Socket socket url]
  (doto socket
    (.connect url)))

(defn subscribe
  ([#^ZMQ$Socket socket #^String topic]
     (doto socket
       (.subscribe (.getBytes topic))))
  ([#^ZMQ$Socket socket]
     (subscribe socket "")))

(defn send-bytes [#^ZMQ$Socket socket #^bytes message]
  (.send socket message 0))

(defn recv-bytes [socket]
  (.recv socket 0))

