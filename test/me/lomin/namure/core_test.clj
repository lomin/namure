(ns me.lomin.namure.core-test
  (:require [clojure.test :refer :all]
            [vijual]
            [me.lomin.namure.core :as namure]
            [me.lomin.poker.strategy :as strategy]))

(deftest converts-namespace-of-var-to-list-test
  (is (<= 4
          (count (namure/read-namespace-of {:file "me/lomin/namure/core_test.clj"})))))

(def test-code '((ns
                  me.lomin.namure.core-test
                   (:require
                    [clojure.test :refer :all]
                    [me.lomin.namure.core :as namure]
                    [clojure.edn :as edn]))
                 (defn read-namespace-of
                   [{file-str :file}]
                   (with-open
                    [rdr (new java.io.PushbackReader
                              (clojure.java.io/reader (clojure.java.io/resource file-str)))]
                     (vec (take-while some? (repeatedly (partial read {:eof nil} rdr))))))
                 (defn test-f []
                   (take-while some (read-namespace-of {:file nil})))))

(deftest finds-all-user-functions-test
  (is (= #{'read-namespace-of 'test-f}
         (namure/get-user-functions (namure/sym-xs->namure test-code)))))

(deftest finds-all-function-dependencies-test
  (is (= #{'with-open 'take-while}
         (namure/get-f-dependencies (second test-code)
                                    {'with-open nil 'take-while nil 'frequencies nil}))))

(deftest creates-tree-test
  (is (= ['test-f
          ['read-namespace-of
           ['take-while]
           ['with-open]]
          ['take-while]]
         (get (namure/sym-xs->namure test-code
                                     nil
                                     {'with-open   ['with-open]
                                      'take-while  ['take-while]
                                      'frequencies ['frequencies]})
              'test-f))))

(deftest integration-test-0
  (is (= '[[sym-xs->namure
            [insert-defn
             [get-f-dependencies
              [get-user-functions]
              [only-if-let]]]
            [insert-ns
             [make-require-map]
             [make-tree-from-sym
              [read-namespace-of]
              [to-file-str
               [string/replace]
               [to-relative-resource
                [string/join]
                [string/split]]]]
             [only-if]]]]
         (namure/make-tree namure/sym-xs->namure))))

(deftest integration-test-1
  (vijual/draw-tree
   (namure/make-tree*
    'answer
    '((defn ranking [])
      (defn combine [])
      (defn calculate-best-hand-possible [])
      (defn ev+? [])
      (defn time-to-bluff? [])

      (defn i-have-the-best-possible-hand [community-cards my-cards]
        (= (ranking (combine community-cards my-cards))
           (ranking (calculate-best-hand-possible community-cards))))

      (defn check-or-call [player-reactions])

      (defn decide-action-when-i-have-not-the-best-possible-hand [my-cards community-cards player-reactions my-past-actions]
        (if (or (ev+? my-cards community-cards player-reactions)
                (time-to-bluff? player-reactions my-past-actions))
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
                                            my-past-actions)))))))

(deftest integration-test-2
  (vijual/draw-tree
   (namure/make-tree
    strategy/answer)))