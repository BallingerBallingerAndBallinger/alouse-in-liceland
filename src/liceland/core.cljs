(ns liceland.core
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<! merge]]
   [liceland.sprites :as sprites]
   [thi.ng.typedarrays.core :as ta]))

(enable-console-print!)
(println "This text is printed from src/liceland/core.cljs. Go ahead and edit it and see reloading in action.")

(defonce app (.getElementById js/document "app"))
(defonce context (.getContext app "2d"))
(defonce width (.getAttribute app "width"))
(defonce height (.getAttribute app "height"))
(defonce images {:hairs "/images/hairs-low.png"})

(declare on-images-loaded)
(defonce load-images (go
                       (<! (merge (map (fn[[k v]](sprites/load k v)) (seq images))))
                       (on-images-loaded)))

(defn set-cursor [cursor]
  (case cursor
    :left  (.setAttribute app "class" "left-cursor")
    :right (.setAttribute app "class" "right-cursor")
    (.setAttribute app "class" "")))

(defonce watch-mouse-move
  (aset app "onmousemove"
        (fn [e]
          (this-as element
            (cond
              (< (- (.-pageX e) (.-offsetLeft element)) (* width 0.2))
              (set-cursor :left)

              (> (- (.-pageX e) (.-offsetLeft element)) (* width 0.8))
              (set-cursor :right)

              :else
              (set-cursor :none))))))

(defn draw [draw-fn]
  (let [raw-image-data (.getImageData context 0 0 width height)]
    (do (draw-fn (-> raw-image-data .-data ta/uint32-view))
        (.putImageData context raw-image-data 0 0))))

(defn text-box-fn [pixels]
  (doall (map #(aset pixels % 0xff000000) 
              (range (* width (* 0.8 height)) (* width height)))))

(defn draw-text [text]
  (draw text-box-fn)
  (aset context "fillStyle" "#FFFFFF")
  (aset context "font"  "24px Arial, sans-serif")
  (.fillText context text 12 (* 0.85 height)))

(defn draw-image [image x y]
  (.drawImage context (sprites/get-loaded image) x y))

(defn on-images-loaded []
  (draw #(.fill % 0xfffff0ff))
  (draw-image :hairs 0 0)
  (draw-text "A vast forest extends to the edge..."))

(defn on-js-reload []
  (on-images-loaded)
  (.log js/console "Reload success!"))

