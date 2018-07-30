(ns me.lomin.poker.strategy
  (:require [clojure.test :refer :all]
            [me.lomin.poker.evaluate :as ev]
            [me.lomin.poker.util :refer [combine]]))

(defn i-have-the-best-possible-hand [community-cards my-cards]
  (= (ev/ranking (combine community-cards my-cards))
     (ev/ranking (ev/calculate-best-hand-possible community-cards))))

(defn check-or-call [player-reactions])

(defn decide-action-when-i-have-not-the-best-possible-hand [my-cards community-cards player-reactions my-past-actions]
  (if (or (ev/ev+? my-cards community-cards player-reactions)
          (ev/time-to-bluff? player-reactions my-past-actions))
    :raise
    :fold))

(defn decide-action [my-cards community-cards player-reactions my-past-actions]
  (if (i-have-the-best-possible-hand community-cards my-cards)
    (check-or-call player-reactions)
    (decide-action-when-i-have-not-the-best-possible-hand my-cards community-cards player-reactions my-past-actions)))

(defn init [action]
  {:action action})

(defn play [plan]
  plan)

(defn archive [plan]
  plan)

(defn make-execution-plan [action]
  (-> action
      (init)
      (play)
      (archive)))

(defn answer [my-cards community-cards player-reactions my-past-actions]
  (make-execution-plan (decide-action my-cards
                                      community-cards
                                      player-reactions
                                      my-past-actions)))
