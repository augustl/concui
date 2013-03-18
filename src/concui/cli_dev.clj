(ns concui.cli-dev
  (:require clojure.tools.nrepl.server
            concui.runner)
  (:gen-class))

(def cake (atom 0))

(defn -main
  [& args]
  (let [repl (clojure.tools.nrepl.server/start-server :port 0)]
    (println (str "Repl started at " (:port repl))))
  (concui.runner/run))