(ns liceland.core)

(defonce app (.getElementById js/document "app"))
(defonce width (.getAttribute app "width"))
(defonce height (.getAttribute app "height"))

(defn clickable [sprite target]
  (cljs.core/merge sprite {:click target}))

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
                :sprites [ (clickable larger-mosquito :heading-on-2) ]
                :music "/audio/liceland.mp3"
                :description "It just keeps going"}
   :heading-on-2 {:background "/images/hairs-low.png"
                  :forward :heading-on-3
                  :music "/audio/liceland.mp3"
                  :sprites [ (clickable largest-mosquito :lookin-at-me) ]}
   :lookin-at-me {:background "/images/hairs-low.png"
                  :forward :heading-on-3
                  :music "/audio/liceland.mp3"
                  :sprites [ (clickable largest-mosquito :lookin-at-me-2) ]
                  :description "\"Oh, another one\""}
   :lookin-at-me-2 {:background "/images/hairs-low.png"
                  :forward :heading-on-3
                  :music "/audio/liceland.mp3"
                  :sprites [ (clickable largest-mosquito :heading-on-2) ]
                  :description "\"You're just like all the others\""}
   :heading-on-3 {:background "/images/hairs-low.png"
                  :description "You've lost your way in the immensity"
                  :forward :head-east }
   :head-east {:background "/images/hairs-low.png"
               :forward :heading-on
               :music "/audio/liceland.mp3"
               :sprites [ (clickable mosquito :heading-on) ]
               :right :head-west
               :left :head}})

