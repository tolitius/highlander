(ns bench.netty-bench
  (:require [highlander.util.netty :as netty]
            [highlander.util.qstats :as q])
  (:use [clojure.tools.cli :only [cli]]
        [clojure.tools.logging]))

(defn rock-and-roll [{:keys [host port produce consume] :as props}]
  (let [handler (netty/data-handler produce)]
    (netty/start handler props)
    (future-cancel consume)))

(defn- floor-drop [message]
  (swap! q/current inc))

(defn plug-and-play [{:keys [host port monterval] :as props}]
  (let [monitor (q/monitor monterval)]
  (rock-and-roll (merge {:produce floor-drop :consume (future #())} props))))

(defn -main [& args]
  (let [[props args usage] 
          (cli args ["-h" "--host" "start on this hostname" :default "0.0.0.0"]
                    ["-p" "--port" "listen on this port" :parse-fn #(Integer. %) :default 4242]
                    ;; ["-n" "--number" "number of things to accept" :parse-fn #(Integer. %) :default 100000000]
                    ["-mi" "--monterval" "queue monitor interval" :default 5])]
    (info usage)
    (plug-and-play props)))

