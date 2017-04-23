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

(def mosquito
  {:sound "/audio/mosquito.mp3"
   :positionX (* 0.7 width)
   :positionY (* 0.34 height)
   :image "/images/mosquito-flit1.png"})

(def larger-mosquito
  (cljs.core/merge mosquito
         {:positionX (* 0.4 width)
          :positionY (* 0.265 height)
          :scale 2 }))

(def largest-mosquito
  (cljs.core/merge mosquito
         {:positionX (* 0.1 width)
          :positionY (* -0.2 width)
          :scale 16}))

(def scenes
  {:head-west {:background "/images/hairs-low.png"
               :description "Nothing but trees"
               :music "/audio/liceland.mp3"
               :right :head
               :left :head-east }
   :head {:background "/images/hairs-low.png"
          :description "A vast forest stretches as far as the eye can see"
          :music "/audio/liceland.mp3"
          :left :head-west
          :right :head-east }
   :heading-on {:background "/images/hairs-low.png"
                :forward :heading-on-2
                :sprites [ larger-mosquito ]
                :music "/audio/liceland.mp3"
                :description "It just keeps going"}
   :heading-on-2 {:background "/images/hairs-low.png"
                  :forward :heading-on-3
                  :music "/audio/liceland.mp3"
                  :sprites [ largest-mosquito ]
                  :description "Is there no end?"}
   :heading-on-3 {:background "/images/hairs-low.png"
                  :description "You've lost your way in the immensity"
                  :forward :head-east }
   :head-east {:background "/images/hairs-low.png"
               :forward :heading-on
               :music "/audio/liceland.mp3"
               :sprites [ mosquito ]
               :right :head-west
               :left :head}})

(defonce current-scene (atom nil))
(defonce current-scene-tag (atom nil))

(def images
  (distinct (concat
             (map #(:image %) (mapcat #(:sprites %) (vals scenes)))
             (map #(:background %) (vals scenes)))))

(def sounds
  (distinct (concat
             (map #(:sound %) (mapcat #(:sprites %) (vals scenes)))
             (remove nil? (map #(:music %) (vals scenes))))))

(declare on-assets-loaded)
(defonce load-images (go
                       (<! (merge (map sprites/load images)))
                       (<! (merge (map sounds/load sounds)))
                       (on-assets-loaded)))

(defn set-cursor [cursor]
  (case cursor
    :left    (.setAttribute app "class" "left-cursor")
    :right   (.setAttribute app "class" "right-cursor")
    :forward (.setAttribute app "class" "forward-cursor")
    :norm    (.setAttribute app "class" "norm-cursor")
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

              (and (< (- (.-pageY e) (.-offsetTop element)) (* height 0.2))
                   (:forward @current-scene))
              (set-cursor :forward)
              
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

              (and (< (- (.-pageY e) (.-offsetTop element)) (* height 0.2))
                   (:forward @current-scene))
              (set-scene (:forward @current-scene))
              
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

(defn draw-scaled-image [image x y s]
  (let [img (sprites/get-loaded image)]
    (.drawImage context img x y (* s (.-width img)) (* s (.-height img)))))

(defn draw-sprite [sprite]
  (if (:scale sprite)
    (draw-scaled-image (:image sprite) (:positionX sprite) (:positionY sprite) (:scale sprite))
    (draw-image (:image sprite) (:positionX sprite) (:positionY sprite)))
  (if (:sound sprite) (sounds/start-loaded-audio-loop (:sound sprite))))

(defn cleanup-sprite [sprite]
  (if (:sound sprite) (sounds/stop-loaded-audio (:sound sprite))))

(defn cleanup-scene [scene]
  (if (:sprites scene)
    (doall (map cleanup-sprite (:sprites scene)))))

(defn draw-scene [scene]
  (if (:music scene)
    (sounds/start-loaded-audio-loop (:music scene)))
  (draw #(.fill % 0xfffff0ff))
  (draw-image (:background scene) 0 0)
  (if (:sprites scene)
    (doall (map draw-sprite (:sprites scene))))
  (if (:description scene)
    (draw-text (:description scene))))

(defn set-scene [scene]
  (if @current-scene (cleanup-scene @current-scene))
  (reset! current-scene-tag scene)
  (reset! current-scene (scene scenes))
  (draw-scene (scene scenes)))

(defn on-assets-loaded []
  (if (nil? @current-scene)
    (set-scene :head)
    (set-scene @current-scene-tag)))

(defn on-js-reload []
  (on-assets-loaded)
  (.log js/console "Reload success!"))

