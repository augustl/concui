(ns concui.cli
  (:require concui.runner)
  (:gen-class))


(defn -main
  [& args]
  (concui.runner/run))