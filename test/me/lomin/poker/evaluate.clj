(ns me.lomin.poker.evaluate
  (:require [clojure.test :refer :all]
            [me.lomin.poker.util :as util]))

(defn ranking [cards] 0)
(defn combinations-of [cards up-to] [])
(defn time-to-bluff? [player-reactions my-past-actions] false)
(defn guess-opponent-score [player-reactions community-cards] 0)

(defn calculate-best-hand-possible [cards]
  (sort-by :ranking (map (fn [cards] {:ranking (ranking cards)
                                      :cards cards})
                         ranking
                         (combinations-of cards 5))))

(defn ev+? [my-cards community-cards player-reactions]
  (or (= my-cards
         (:cards (first (calculate-best-hand-possible (util/combine my-cards community-cards)))))
      (< (guess-opponent-score player-reactions community-cards) (ranking my-cards))))
