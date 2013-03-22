(ns highlander.util.schedule
  (:use [clojure.tools.logging])
  (:import
    [java.util.concurrent Executors TimeUnit]))

(defn every 
  ([interval fun] 
   (every interval fun TimeUnit/SECONDS))
  ([interval fun time-unit] 
    (.scheduleAtFixedRate (Executors/newScheduledThreadPool 1) 
      fun 0 interval time-unit)))
