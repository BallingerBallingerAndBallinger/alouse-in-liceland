(ns liceland.core
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [liceland.sprites :as sprites]
   [liceland.audio :as sounds]
   [liceland.game :refer [scenes]]
   [cljs.core.async :refer [<! merge]]
   [thi.ng.typedarrays.core :as ta]))

(enable-console-print!)
(println "This text is printed from src/liceland/core.cljs. Go ahead and edit it and see reloading in action.")

(defonce app (.getElementById js/document "app"))
(defonce context (.getContext app "2d"))
(defonce width (.getAttribute app "width"))
(defonce height (.getAttribute app "height"))

(declare set-scene)

(defonce current-scene (atom nil))
(defonce current-scene-tag (atom nil))
(defonce current-state (atom {}))

(def images
  (distinct (concat
             (map #(:image %) (mapcat #(:sprites %) (vals (scenes @current-state))))
             (map #(:background %) (vals (scenes @current-state))))))

(def sounds
  (distinct (concat
             (remove nil? (map #(:sound %) (mapcat #(:sprites %) (vals (scenes @current-state)))))
             (remove nil? (map #(:sound %) (vals (scenes @current-state))))
             (remove nil? (map #(:music %) (vals (scenes @current-state)))))))

(declare on-assets-loaded)
(defn load-assets []
  (let [resources (merge [(merge (map sprites/load images))
                          (merge (map sounds/load sounds))])]
    (go
      (while (<! resources) nil)
      (on-assets-loaded))))

(defonce initial-load (load-assets))

(defn set-cursor [cursor]
  (case cursor
    :left    (.setAttribute app "class" "left-cursor")
    :right   (.setAttribute app "class" "right-cursor")
    :forward (.setAttribute app "class" "forward-cursor")
    :back    (.setAttribute app "class" "back-cursor")
    :click   (.setAttribute app "class" "click-cursor")
    :norm    (.setAttribute app "class" "norm-cursor")
    (.setAttribute app "class" "")))

(defn clicked-sprite [sprite x y]
  (let [img (sprites/get-loaded (:image sprite))
        width (* (or (:scale sprite) 1) (.-width img))
        height (* (or (:scale sprite) 1) (.-height img))
        right (+ (:positionX sprite) width)
        bottom (+ (:positionY sprite) height)]
    (if (and (> x (:positionX sprite))
             (< x right)
             (> y (:positionY sprite))
             (< y bottom)) (:click sprite) nil)))

(defn clicked-sprite-in-scene [scene x y]
  (first (filter #(clicked-sprite % x y) (:sprites scene))))

(defonce watch-mouse-move
  (aset app "onmousemove"
        (fn [e]
          (this-as element
            (let [x (* (/ width  (or (.-scrollWidth app)  (.-offsetWidth app)))  (- (.-pageX e) (.-offsetLeft element)))
                  y (* (/ height (or (.-scrollHeight app) (.-offsetHeight app))) (- (.-pageY e) (.-offsetTop element)))
                  sprite (clicked-sprite-in-scene @current-scene x y)]
              (cond
                (and (< x (* width 0.2))
                     (:left @current-scene))
                (set-cursor :left)
                
                (and (> x (* width 0.8))
                     (:right @current-scene))
                (set-cursor :right)

                (and (< y (* height 0.2))
                     (:forward @current-scene))
                (set-cursor :forward)

                (and (> y (* height 0.8))
                     (:back @current-scene))
                (set-cursor :back)
                
                (and sprite (:click sprite))
                (set-cursor :click)

                :else
                (set-cursor :norm)))))))

(defonce watch-mouse-down
  (aset app "onmousedown"
        (fn [e]
          (this-as element

            (let [x (* (/ width  (or (.-scrollWidth app)  (.-offsetWidth app)))  (- (.-pageX e) (.-offsetLeft element)))
                  y (* (/ height (or (.-scrollHeight app) (.-offsetHeight app))) (- (.-pageY e) (.-offsetTop element)))
                  sprite (clicked-sprite-in-scene @current-scene x y)]
            (cond
              (and (< x (* width 0.2))
                   (:left @current-scene))
              (set-scene (:left @current-scene))

              (and (< y (* height 0.2))
                   (:forward @current-scene))
              (set-scene (:forward @current-scene))

              (and (> y (* height 0.8))
                   (:back @current-scene))
              (set-scene (:back @current-scene))
 
              (and (> x (* width 0.8))
                   (:right @current-scene))
              (set-scene (:right @current-scene))

              (and sprite (:click sprite))
              (set-scene (:click sprite))))))))

(defn draw [draw-fn]
  (let [raw-image-data (.getImageData context 0 0 width height)]
    (do (draw-fn (-> raw-image-data .-data ta/uint32-view))
        (.putImageData context raw-image-data 0 0))))

(defn text-box-fn [pixels]
  (doall (map #(aset pixels % 0xff000000) 
              (range (* width (int (* 0.8 height))) (* width height)))))

(defn rgb-mult [a color]
  (let [blue  (rem color 0x000100)
        green (/ (rem (- color blue) 0x010000) 0x000100)
        red   (/ (- color (* green 0x000100) blue) 0x010000)]
    (+ (* (int (* a red)) 0x010000)
       (* (int (* a green)) 0x000100)
       (int (* a blue)))))

(defn alpha-blend [dst src]
  (let [dst-a (/ (int (/ dst 0x01000000)) 0xff)
        src-a (/ (int (/ src 0x01000000)) 0xff)
        out-a (+ (* (- 1 src-a) dst-a) src-a)]
    (+  (int (* (* 0xff out-a) 0x1000000))
        (int (/
              (+ (rgb-mult src-a (rem src 0x01000000))
                 (rgb-mult (* dst-a (- 1 src-a)) (rem dst 0x01000000)))
              out-a)))))

(defn color-pixel-fn [pixels x y color]
  (let [dest (aget pixels (int (+ x (* width y))))]
    (if (and (> x 0) (> y 0) (< x width) (< y height))
      (aset pixels (+ x (* width y)) (alpha-blend dest color)))))

(defn compute-source-pixel [img scale idx]
  (cond
    (= 1 scale) idx
    :default (let [row (/ idx (* scale (:width img)))
                   col (int (rem idx (* scale (:width img))))
                   source-row (int (/ row scale))
                   source-col (int (/ col scale))]
               (+ source-col (* source-row (:width img))))))

(defn splat-fn [pixels x y s image]
  (let [img (sprites/get-image-data image)
        img-pixels (:data img)
        w (* s (:width img))
        h (* s (:height img))
        row #(int (/ % w))]
    (doall (map #(color-pixel-fn pixels
                                 (+ x (rem % w))
                                 (+ y (row %))
                                 (aget img-pixels (compute-source-pixel img s %)))
                (range (* w h))))))

(defn draw-letter [index letter]
  (case letter
    (.fillText context letter (* (+ 2 index) 4) (* 0.92 height))))

(defn draw-text [text]
  (draw text-box-fn)
  (aset context "fillStyle" "#FFFFFF")
  (aset context "font"  "9px \"Lucida Console\", Monaco, monospace")
  (doall (map-indexed draw-letter text)))

(defn draw-image [image x y]
  (.drawImage context (sprites/get-loaded image) x y))

(defn draw-scaled-image [image x y s]
  (let [img (sprites/get-loaded image)]
    (.drawImage context img x y (* s (.-width img)) (* s (.-height img)))))

(defn draw-sprite [sprite]
  (draw #(splat-fn % (int (:positionX sprite)) (int (:positionY sprite)) (or (:scale sprite) 1) (:image sprite)))
  (if (:sound sprite) (sounds/start-loaded-audio-loop (:sound sprite))))

(defn cleanup-sprite [sprite]
  (if (:sound sprite) (sounds/stop-loaded-audio (:sound sprite))))

(defn cleanup-scene [scene]
  (if (:music scene)
    (sounds/stop-loaded-audio (:music scene)))
  (if (:sprites scene)
    (doall (map cleanup-sprite (:sprites scene)))))

(defn draw-scene [scene]
  (if (:update scene) (swap! current-state (:update scene)))
  (if (:music scene)
    (sounds/start-loaded-audio-loop (:music scene)))
  (if (:sound scene)
    (sounds/start-loaded-audio (:sound scene)))
  (draw #(.fill % 0xfffff0ff))
  (draw-image (:background scene) 0 0)
  (if (:sprites scene)
    (doall (map draw-sprite (:sprites scene))))
  (if (:description scene)
    (draw-text (:description scene))))

(defn set-scene [scene]
  (if @current-scene (cleanup-scene @current-scene))
  (reset! current-scene-tag scene)
  (reset! current-scene (scene (scenes @current-state))
  (draw-scene (scene (scenes @current-state)))))

(defn on-assets-loaded []
  (if (nil? @current-scene)
    (set-scene :head)
    (set-scene @current-scene-tag)))

(defn on-js-reload []
  (load-assets)
  (.log js/console "Reload success!"))

