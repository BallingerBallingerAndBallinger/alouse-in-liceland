(ns liceland.game)

(defonce app (.getElementById js/document "app"))
(defonce width (.getAttribute app "width"))
(defonce height (.getAttribute app "height"))
(declare forest)
(declare mosquito-dialog)

;; Sprites permit the following properties
;; sound: URL of a sound file to play when the sprite is visible (looping)
;; positionX: The x position of the sprite
;; positionY: The y position of the sprite
;; image:     URL of an image file for the sprite
;; click:     A symbol pointing to a scene to jump to when the sprite is clicked
;; scale:     A scaling factor

(defn clickable [sprite target]
  (cljs.core/merge sprite {:click target}))

(def mosquito
  {:sound "/audio/mosquito.mp3"
   :positionX (* 0.7 width)
   :positionY (* 0.34 height)
   :image "/images/mosquito-flit1.png"})

;; Demonstrating one way of building things up from simpler pieces.
(def larger-mosquito
  (cljs.core/merge mosquito
                   {:positionX (* 0.4 width)
                    :positionY (* 0.265 height)
                    :scale 2 }))

(def largest-mosquito
  (cljs.core/merge mosquito
                   {:positionX (* 0.1 width)
                    :positionY (* -0.5 height)
                    :scale 8}))

(def axe
  {:positionX (* 0.34 width)
   :positionY (* 0.65 height)
   :image "/images/axe.png"})

(def trees
  {:positionX 0
   :positionY 0
   :image "/images/trees1.png"})

;; Scenes permit the following properties
;; background:  URL of a background for the scene
;; description: Text that appears beneath a scene
;; music:       Music to play while you're in the scene
;; right:       A symbol pointing to a scene to jump to when right is clicked
;; left:        A symbol pointing to a scene to jump to when left is clicked
;; forward:     A symbol pointing to a scene to jump to when forward is clicked
;; back:        A symbol pointing to a scene to jump to when back is clicked
;; sprites:     An array of sprites present in the scene
;; update:      A function to update the state when a scene is entered.... AND FOR THE LOVE OF GOD

;; Be careful with the state...
;; It should be used extremely sparingly
;; e.g. It's easy to subtly break live-reloading by getting something stuck in your state.
;; e.g. It's easy to get into an "unreachable state" then keep developing worlds without realizing that no player will ever
;;      have the same state that you do...
;; e.g. Asset-preloading depends on sprites, backgrounds, music, and sounds to be reachable with an empty state.
;; However, with all of those warnings, having access to it give you great power, and with great power comes...! (Mayhem!)
;; You will find that things are happy and easy and everything is great and there are rainbows and unicorns
;; unless you ABUSE THE STATE THEN DEVIL HIMSELF WILL CRAWL SCREAMING OUT OF YOUR BUTTHOLE.

(defn scenes [state]
  (merge (mosquito-dialog state)
         (forest state)))

(defn forest [state]
  {:head-west {:background "/images/scalp-new.png"
               :description "Nothing but trees"
               :music "/audio/liceland.mp3"
               :sprites [ trees ]
               :forward :clearing
               :right :head
               :left :head-east }
   :clearing {:background "/images/scalp-new.png"
              :description "A nice little clearing"
              :sprites (if (not (:axe state)) [(clickable axe :get-axe)])
              :music "/audio/liceland.mp3"
              :back :head-west }
   :get-axe {:background "/images/scalp-new.png"
             :description "Still sharp"
             :music "/audio/liceland.mp3"
             :update #(assoc % :axe true)
             :back :head-west}
   :head {:background "/images/scalp-new.png"
          :description "A vast forest stretches as far as the eye can see"
          :music "/audio/liceland.mp3"
          :sprites [ trees ]
          :left :head-west
          :right :head-east }
   :heading-on {:background "/images/scalp-new.png"
                :forward :heading-on-2
                :back :head-east
                :sprites [ trees (clickable larger-mosquito :heading-on-2) ]
                :music "/audio/liceland.mp3"
                :description "It just keeps going"}
   :heading-on-2 {:background "/images/scalp-new.png"
                  :forward :heading-on-3
                  :back :heading-on
                  :music "/audio/liceland.mp3"
                  :sprites [ trees (if (not (:talked-to-mosq state))
                                     (clickable largest-mosquito :lookin-at-me)
                                     (clickable largest-mosquito :not-lookin-at-me))]}
   :heading-on-3 {:background "/images/scalp-new.png"
                  :description "You've lost your way in the immensity"
                  :back    :head-east
                  :left    :head-east
                  :right   :head-east
                  :sprites [ trees ]
                  :forward :head-east }
   :head-east {:background "/images/scalp-new.png"
               :forward :heading-on
               :music "/audio/liceland.mp3"
               :sprites [ trees (clickable mosquito :heading-on) ]
               :right :head-west
               :left :head}})

(defn mosquito-dialog [state]
  ;; Demonstrating how a base scene can be extended...  Imagine the possibilities.
  (let [base (partial merge {:forward :heading-on-3
                             :left :heading-on-3
                             :back :heading-on
                             :music "/audio/liceland.mp3"
                             :background "/images/scalp-new.png"})]

    {:lookin-at-me (base {:sprites [ trees (clickable largest-mosquito :lookin-at-me-2) ]
                          :description "\"Oh, another one\""})

     :not-lookin-at-me (base {:sprites [ trees (clickable largest-mosquito :heading-on-2) ]
                              :description "\"...\""})

     :lookin-at-me-2 (base {:sprites [ trees (clickable largest-mosquito :heading-on-2) ]
                            :update #(assoc % :talked-to-mosq true)
                            :description "\"You're just like all the others\""})}))

