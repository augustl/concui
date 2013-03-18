(defproject concui "0.1.0-SNAPSHOT"
  :description "Truly concurrent UI framework, completely free of mutable values."
  :url "http://example.com/FIXME"
  :license {:name "BSD 3-clause license"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.lwjgl.lwjgl/lwjgl "2.8.5"]]
  :profiles {:dev {:jvm-opts ["-Djava.library.path=native"]
                   :dependencies [[org.clojure/tools.nrepl "0.2.2"]]}})
