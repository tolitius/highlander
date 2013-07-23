(defproject highlander "0.1.0-SNAPSHOT"
  :description "taking in massive load of data form the sky and absobs it into a system with a controlled rate"
  :url "https://github.com/tolitius/highlander"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src" "src/highlander"]
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [org.clojure/tools.logging "0.2.6"]
                 [com.taoensso/carmine "1.6.0"]
                 [io.netty/netty "3.6.3.Final"]
                 [clj-time "0.4.5"]
                 [org.clojure/tools.cli "0.2.2"]
                 [zmq/zmq "3.2.2"]]
  
  :java-source-paths ["src/bench" "src/lmax"]
  :native-path "/usr/local/lib"
  :jvm-opts [~(str "-Djava.library.path=native/:" (System/getenv "LD_LIBRARY_PATH")) 
             "-Xmx4G" 
             ;; "-XX:+UseG1GC"
             "-XX:MaxPermSize=512M"]

  :main highlander)
