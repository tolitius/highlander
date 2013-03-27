(ns bench.server-bench
  (:require [highlander.util.nio :as nio]
            [highlander.util.netty :as netty]
            [highlander.util.qstats :as q])
  (:use [clojure.tools.cli :only [cli]]
        [clojure.tools.logging]))

(defn rock-and-roll [{:keys [server host port produce consume fixed-length] :as props}]
  (case server
    "netty" (let [handler (netty/data-handler produce)]
              (netty/start handler props))
    "nio"   (let [handler (partial nio/decode-fixed-lengh-frame produce fixed-length)]
              (nio/start handler props))
  (future-cancel consume)))

(defn- floor-drop [message]
  (swap! q/current inc))

(defn plug-and-play [{:keys [server host port monterval fixed-length] :as props}]
  (let [monitor (q/monitor monterval)]
  (rock-and-roll (merge {:produce floor-drop :consume (future #())} props))))

(defn -main [& args]
  (let [[props args usage] 
          (cli args ["-h" "--host" "start on this hostname" :default "0.0.0.0"]
                    ["-p" "--port" "listen on this port" :parse-fn #(Integer. %) :default 4242]
                    ["-s" "--server" "server type [e.g. netty, nio]" :default "netty"]
                    ;; ["-n" "--number" "number of things to accept" :parse-fn #(Integer. %) :default 100000000]
                    ["-fl" "--fixed-length" "fixed length messages" :parse-fn #(Integer. %) :default 107]
                    ["-mi" "--monterval" "queue monitor interval" :default 5])]
    (info usage)
    (plug-and-play props)))

