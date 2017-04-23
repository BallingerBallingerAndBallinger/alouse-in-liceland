(ns liceland.core
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [liceland.sprites :as sprites]
   [liceland.sounds :as sounds]
   [cljs.core.async :refer [<! merge]]
   [thi.ng.typedarrays.core :as ta]))

(enable-console-print!)
(println "This text is printed from src/liceland/core.cljs. Go ahead and edit it and see reloading in action.")

(defonce app (.getElementById js/document "app"))
(defonce context (.getContext app "2d"))
(defonce width (.getAttribute app "width"))
(defonce height (.getAttribute app "height"))

(declare set-scene)

(def scenes
  {:head-west {:background "/images/hairs-low.png"
               :description "Nothing but trees"
               :music "/audio/liceland.mp3"
               :right :head }
   :head {:background "/images/hairs-low.png"
          :description "A vast forest stretches as far as the eye can see"
          :music "/audio/liceland.mp3"
          :left :head-west
          :right :head-east }
   :head-east {:background "/images/hairs-low.png"
               :left :head}})

(defonce current-scene (atom nil))

(def images
  (map #(:background %) (vals scenes)))

(def sounds
  (filter #(not (nil? %)) (map #(:music %) (vals scenes))))

(declare on-assets-loaded)
(defonce load-images (go
                       (<! (merge (map sprites/load images)))
                       (<! (merge (map sounds/load sounds)))
                       (on-assets-loaded)))

(defn set-cursor [cursor]
  (case cursor
    :left  (.setAttribute app "class" "left-cursor")
    :right (.setAttribute app "class" "right-cursor")
    :norm  (.setAttribute app "class" "norm-cursor")
    (.setAttribute app "class" "")))

(defonce watch-mouse-move
  (aset app "onmousemove"
        (fn [e]
          (this-as element
            (cond
              (and (< (- (.-pageX e) (.-offsetLeft element)) (* width 0.2))
                   (:left @current-scene))
              (set-cursor :left)

              (and (> (- (.-pageX e) (.-offsetLeft element)) (* width 0.8))
                   (:right @current-scene))
              (set-cursor :right)

              :else
              (set-cursor :norm))))))

(defonce watch-mouse-down
  (aset app "onmousedown"
        (fn [e]
          (this-as element
            (cond
              (and (< (- (.-pageX e) (.-offsetLeft element)) (* width 0.2))
                   (:left @current-scene))
              (set-scene (:left @current-scene))
              (and (> (- (.-pageX e) (.-offsetLeft element)) (* width 0.8))
                   (:right @current-scene))
              (set-scene (:right @current-scene)))))))

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

(defn draw-scene [scene]
  (if (:music scene)
    (sounds/start-loaded-audio-loop (:music scene)))
  (draw #(.fill % 0xfffff0ff))
  (draw-image (:background scene) 0 0)
  (if (:description scene)
    (draw-text (:description scene))))

(defn set-scene [scene]
  (reset! current-scene (scene scenes))
  (draw-scene (scene scenes)))

(defn on-assets-loaded [] (set-scene :head))

(defn on-js-reload []
  (on-assets-loaded)
  (.log js/console "Reload success!"))

