(ns liceland.sprites
  (:require-macros 
   [cljs.core.async.macros :refer [go]])
  (:require
   [thi.ng.typedarrays.core :as ta]
   [cljs.core.async :refer [chan >! close!]]))

(defonce image-cache (atom {}))

(defn cache-image [symbol image]
  (do (swap! image-cache assoc symbol image)))

(defn return-image-and-close [symbol out]
  (go (>! out (symbol @image-cache))
      (close! out)))

(defn load [url]
  (let [out (chan)
        sym (symbol url)]
    (.log js/console (str "Loading image for " (name sym)))
    (if (sym @image-cache) (return-image-and-close sym out)
        (do
          (let [img (js/Image.)]
            (aset img "onload" #(do ((cache-image sym img)
                                     (return-image-and-close sym out))))
            (aset img "src" url))))
    out))

(defn get-loaded [url]
  ((symbol url) @image-cache))

(defn get-image-data [url]
  (let [img (get-loaded url)
        canvas (.createElement js/document "canvas")
        width (.-width img)
        height (.-height img)]
    (do (aset canvas "width" width)
        (aset canvas "height" height)
        (let [context (.getContext canvas "2d")]
          (.drawImage context img 0 0)
          {:data (ta/uint32-view (.-data (.getImageData context 0 0 width height)))
           :width width
           :height height}))))

