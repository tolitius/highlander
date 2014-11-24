(ns bench.streamer
  (:import [java.util.concurrent Executors]
           [bench Streamer])
  (:use [clojure.tools.cli :only [cli]]
        [clojure.tools.logging]))

(defonce ULTIMATE_TRUTH "[Did you know that the Answer to the Ultimate Question of Life, the Universe, and Everything is 42?]")
(defonce DEFAULT_HOSTNAME "0.0.0.0")
(defonce DEFAULT_PORT 4242)
(defonce DEFAULT_THINGS_TO_STREAM (* 200 1000 1000))
(defonce DEFAULT_NUMBER_OF_CLIENTS 5)

(defn rock-and-roll [{:keys [host port number-of-things thing clients]}]
  (let [pool (Executors/newFixedThreadPool clients)
        tasks (map
                #(Streamer. (str %) host port thing number-of-things) ;; a new streamer
                (range clients))]                                     ;; for every client
    (doseq [f (.invokeAll pool tasks)]
      (.get f))
    (.shutdown pool)))

(defn -main [& args]
  (let [[props args usage] 
          (cli args ["-h" "--host" "start on this hostname" :default DEFAULT_HOSTNAME]
                    ["-p" "--port" "listen on this port" :parse-fn #(Integer. %) :default DEFAULT_PORT]
                    ["-c" "--clients" "number of clients" :parse-fn #(Integer. %) :default DEFAULT_NUMBER_OF_CLIENTS]
                    ["-n" "--number-of-things" "number of things to stream" :parse-fn #(Long. %) :default DEFAULT_THINGS_TO_STREAM]
                    ["-t" "--thing" "a thing/message to send" :default ULTIMATE_TRUTH])]
    (info usage)
    (rock-and-roll props)))