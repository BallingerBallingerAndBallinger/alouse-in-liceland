(ns liceland.core
  (:require [thi.ng.typedarrays.core :as ta]))

(enable-console-print!)
(println "This text is printed from src/liceland/core.cljs. Go ahead and edit it and see reloading in action.")

(defonce app (.getElementById js/document "app"))
(defonce context (.getContext app "2d"))
(defonce width (.getAttribute app "width"))
(defonce height (.getAttribute app "width"))

(defn set-cursor [cursor]
  (case cursor
    :left (.setAttribute app "class" "left-cursor")
    (.setAttribute app "class" "")))

(defonce watch-mouse-move
  (aset app "onmousemove"
                     (fn [e]
                       (this-as element
                         (if (< (- (.-pageX e) (.-offsetLeft element)) (* width 0.2))
                           (set-cursor :left)
                           (set-cursor :none))))))

(def raw-image-data (.getImageData context 0 0 width height))
(defn draw [draw-fn]
  (do (draw-fn (-> raw-image-data .-data ta/uint32-view))
      (.putImageData context raw-image-data 0 0)))
(defn draw-image [image x y]
  (let [img (js/Image.)]
    (do
      (aset img "onload" #(.drawImage context img x y))
      (aset img "src" "/images/hairs-low.png"))))

;; Make her red to demonstrate functioning
(draw #(.fill % 0xff0000ff))
(draw-image :hairs 0 0)

(defn on-js-reload []
  (.log js/console "Reload success!"))
