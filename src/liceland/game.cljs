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

(defn set-state [key value]
  #(assoc % key value))

(def demonwig
  {:positionX (* 0.3 width)
   :positionY (* 0.1 height)
   :image "/images/earwig.png"})

(def mosquito
  {:sound "/audio/mosquito.mp3"
   :positionX (* 0.76 width)
   :positionY (* 0.4 height)
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
  {:head-west
   {:background "/images/forest2.png"
    :description "Nothing but trees"
    :music "/audio/liceland.mp3"
    :forward :clearing
    :right :head
    :left :head-east }

   :clearing
   {:background "/images/forest2.png"
    :description "A nice little clearing"
    :sprites (if (not (:axe state)) [(clickable axe :get-axe)])
    :music "/audio/liceland.mp3"
    :back :head-west }

   :get-axe
   {:background "/images/forest2.png"
    :description "Still sharp"
    :music "/audio/liceland.mp3"
    :update (set-state :axe true)
    :back :head-west}

   :head
   {:background "/images/forest2.png"
    :description "A vast forest stretches as far as the eye can see"
    :music "/audio/liceland.mp3"
    :sprites [  ]
    :left :head-west
    :right :head-east }

   :heading-on
   {:background "/images/forest2.png"
    :forward :heading-on-2
    :back :head-east
    :sprites [  (clickable larger-mosquito :heading-on-2) ]
    :music "/audio/liceland.mp3"
    :description "It just keeps going"}

   :heading-on-2
   {:background "/images/forest2.png"
    :forward :heading-on-3
    :back :heading-on
    :music "/audio/liceland.mp3"
    :sprites [  (if (not (:talked-to-mosq state))
                  (clickable largest-mosquito :lookin-at-me)
                  (clickable largest-mosquito :not-lookin-at-me))]}

   :heading-on-3
   {:background "/images/forest2.png"
    :description "You've lost your way in the immensity"
    :back    :head-east
    :left    :head-east
    :right   :head-east
    :forward :head-east }

   :head-east
   {:background "/images/forest2.png"
    :forward :heading-on
    :music "/audio/liceland.mp3"
    :sprites [  (clickable mosquito :heading-on) ]
    :right :head-west
    :left :head}})

(defn mosquito-dialog [state] {

   :not-lookin-at-me
   {:forward :heading-on-3
    :left :heading-on-trees3
    :back :heading-on
    :music "/audio/liceland.mp3"
    :background "/images/forest2.png"
    :sprites [ (clickable largest-mosquito :heading-on-2) ]
    :description "\"...\""}
   
   :lookin-at-me
   {:forward :heading-on-3
    :left :heading-on-trees3
    :back :heading-on
    :music "/audio/liceland.mp3"
    :background "/images/forest2.png"
    :sprites [ (clickable largest-mosquito :lookin-at-me-2) ]
    :description "\"Oh, another one\""}

   :lookin-at-me-2
   {:forward :heading-on-3
    :left :heading-on-trees3
    :back :heading-on
    :music "/audio/liceland.mp3"
    :background "/images/forest2.png"
    :sprites [ (clickable largest-mosquito :lookin-at-me-3) ]
    :description "\"You're no different from the others. Leave me be.\""}

   :lookin-at-me-3
   {:forward :heading-on-3
    :left :heading-on-trees3
    :back :heading-on
    :music "/audio/liceland.mp3"
    :background "/images/forest2.png"
    :sprites [ (clickable largest-mosquito :lookin-at-me-4) ]
    :description "\"I can't help one like you.\""}

   :lookin-at-me-4
   {:forward :heading-on-3
    :left :heading-on-trees3
    :back :heading-on
    :music "/audio/liceland.mp3"
    :background "/images/forest2.png"
    :sprites [ (clickable largest-mosquito :lookin-at-me-5) ]
    :description "\"What would be the point? Most likely you'll spend what's left of your pitiful life on this tiny, dreary world, never knowing the great beyond.\""}
   
   :lookin-at-me-5
   {:forward :heading-on-3
    :left :heading-on-trees3
    :back :heading-on
    :music "/audio/liceland.mp3"
    :background "/images/forest2.png"
    :sprites [ (clickable largest-mosquito :lookin-at-me-6) ]
    :description "\"Beyond?\""}

   :lookin-at-me-6
   {:forward :heading-on-3
    :left :heading-on-trees3
    :back :heading-on
    :music "/audio/liceland.mp3"
    :background "/images/forest2.png":sprites [ (clickable largest-mosquito :lookin-at-me-7) ]
    :description "\"Nothing for you there, unless you can fly. That's the only way to escape this wretched place.\""}

   :lookin-at-me-7
   {:forward :heading-on-3
    :left :heading-on-trees3
    :back :heading-on
    :music "/audio/liceland.mp3"
    :background "/images/forest2.png":sprites [ (clickable largest-mosquito :heading-on-2) ]
    :update (set-state :talked-to-mosq true)
    :description "\"Stop eyeing my wings, creep. You couldn't use them anyways.\""}})

