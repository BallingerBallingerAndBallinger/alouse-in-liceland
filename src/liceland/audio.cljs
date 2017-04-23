(ns liceland.sounds
  (:require-macros 
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [chan >! close!]]))

(defonce audio-cache (atom {}))

(defn cache-audio [symbol audio]
  (do (swap! audio-cache assoc symbol audio)))

(defn return-audio-and-close [symbol out]
  (go (>! out (symbol @audio-cache))
      (close! out)))

(defn load [url]
  (let [out (chan)
        sym (symbol url)]
    (if (sym @audio-cache) (return-audio-and-close sym out)
        (do
          (.log js/console (str "Loading audio for " (name sym)))
          (let [aud (js/Audio.)]
            (aset aud "src" url)
            (cache-audio sym aud)
            (return-audio-and-close sym out))))
    out))

(defn start-loaded-audio [url]
  (let [aud ((symbol url) @audio-cache)]
    (aset aud "loop" nil)
    (.play aud)))

(defn start-loaded-audio-loop [url]
  (let [aud ((symbol url) @audio-cache)]
    (aset aud "loop" "true")
    (.play aud)))

(defn stop-loaded-audio [url]
  (.stop ((symbol url) @audio-cache)))

