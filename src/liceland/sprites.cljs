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

