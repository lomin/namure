(ns me.lomin.poker.util
  (:require [clojure.test :refer :all]))

(defn combine [& args]
  (reduce into [] args))