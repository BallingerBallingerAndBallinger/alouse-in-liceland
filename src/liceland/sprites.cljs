(ns liceland.sprites
  (:require-macros 
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [chan >! close!]]))

(defonce image-cache (atom {}))

(defn cache-image [symbol image]
  (do (swap! image-cache assoc symbol image)))

(defn return-image-and-close [symbol out]
  (go (>! out (symbol @image-cache))
       (close! out)))

(defn load [symbol url]
  (let [out (chan)]
    (.log js/console (str "Loading image for " (name symbol)))
    (if (symbol @image-cache) (return-image-and-close symbol out)
        (do
          (let [img (js/Image.)]
            (aset img "onload" #(do ((cache-image symbol img)
                                     (return-image-and-close symbol out))))
            (aset img "src" url))))
    out))

(defn get-loaded [symbol]
  (symbol @image-cache))

