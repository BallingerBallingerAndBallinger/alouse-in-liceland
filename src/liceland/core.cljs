(ns liceland.core
  (:require [thi.ng.typedarrays.core :as ta]))

(enable-console-print!)
(println "This text is printed from src/liceland/core.cljs. Go ahead and edit it and see reloading in action.")

(defonce app (.getElementById js/document "app"))
(defonce context (.getContext app "2d"))
(defonce width (.getAttribute app "width"))
(defonce height (.getAttribute app "width"))

(def raw-image-data (.getImageData context 0 0 width height))
(defn draw [draw-fn]
  (do (draw-fn (-> raw-image-data .-data ta/uint32-view))
      (.putImageData context raw-image-data 0 0)))

;; Make her red to demonstrate functioning
(draw #(.fill % 0xff0000ff))

(defn on-js-reload []
  (.log js/console "Reload success!"))
