(ns liceland.core
  (:require ))

(enable-console-print!)
(println "This text is printed from src/liceland/core.cljs. Go ahead and edit it and see reloading in action.")

(defonce app (.getElementById js/document "app"))
(defonce context (.getContext app "2d"))
(defonce width (.getAttribute app "width"))
(defonce height (.getAttribute app "width"))

(def image-data (atom (.-data (.getImageData context 0 0 width height))))
(defonce observer (add-watch image-data :draw #(.putImageData %2 0 0)))

(defn on-js-reload []
  (.log js/console "Reload success!"))
