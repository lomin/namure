(ns me.lomin.namure.core-test
  (:require [clojure.test :refer :all]
            [me.lomin.namure.core :as namure]
            [clojure.edn :as edn]))

(defn read-namespace-of [{file-str :file}]
  (with-open [rdr (new java.io.PushbackReader
                       (clojure.java.io/reader (clojure.java.io/resource file-str)))]
    (vec (take-while some?
                     (repeatedly (partial read
                                          {:eof nil}
                                          rdr))))))

(defmacro make-tree [sym]
  `(read-namespace-of (meta (var ~sym))))

(deftest converts-namespace-of-var-to-list-test
  (is (<= 4
          (count (read-namespace-of {:file "me/lomin/namure/core_test.clj"})))))

(defn get-user-functions [m]
  (set (keys m)))

(defn get-f-dependencies
  ([xs user-functions]
   (get-f-dependencies xs user-functions #{}))
  ([xs user-functions result]
   (if-let [f (first xs)]
     (if (sequential? f)
       (recur (concat f (next xs)) user-functions result)
       (if (user-functions f)
         (recur (next xs) user-functions (conj result f))
         (recur (next xs) user-functions result)))
     result)))

(defn sym-xs->tree
  ([xs]
   (sym-xs->tree xs {}))
  ([xs result]
   (if (seq xs)
     (recur (next xs)
            (let [f-xs (first xs)]
              (if (= 'defn (first f-xs))
                (assoc result
                  (second f-xs)
                  (into [(second f-xs)]
                        (map (partial get result))
                        (sort (get-f-dependencies f-xs
                                                  (get-user-functions result)))))
                result)))
     result)))

(def test-code '((ns
                   me.lomin.namure.core-test
                   (:require
                     [clojure.test :refer :all]
                     [me.lomin.namure.core :as namure]
                     [clojure.edn :as edn]))
                  (defn read-namespace-of
                    [{file-str :file}]
                    (with-open
                      [rdr
                       (new
                         java.io.PushbackReader
                         (clojure.java.io/reader (clojure.java.io/resource file-str)))]
                      (vec (take-while some? (repeatedly (partial read {:eof nil} rdr))))))
                  (defn test-f []
                    (take-while some (read-namespace-of {:file nil})))))

(deftest finds-all-user-functions-test
  (is (= #{'read-namespace-of 'test-f}
         (get-user-functions (sym-xs->tree test-code)))))

(deftest finds-all-function-dependencies-test
  (is (= #{'with-open 'take-while}
         (get-f-dependencies (second test-code)
                             #{'with-open 'take-while 'frequencies}))))

(deftest creates-tree-test
  (is (= ['test-f
          ['read-namespace-of
           ['take-while]
           ['with-open]]
          ['take-while]]
         (get (sym-xs->tree test-code
                            {'with-open   ['with-open]
                             'take-while  ['take-while]
                             'frequencies ['frequencies]})
              'test-f))))
