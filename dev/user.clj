(ns user
  (:require [figwheel.main.api]))

(defn start-figwheel!
  ([]
   (start-figwheel! "dev"))
  ([build-id]
   (figwheel.main.api/start {:mode :serve} build-id)))

(defn cljs-repl
  ([]
   (cljs-repl "dev"))
  ([build-id]
   (figwheel.main.api/cljs-repl build-id)))
