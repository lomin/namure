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

(defn sym-xs->tree
  ([xs]
   (sym-xs->tree xs {:functions #{}}))
  ([xs result]
   (if (seq xs)
     (recur (next xs)
            (let [e (first xs)]
              (if (= 'defn (first e))
                (update result
                        :functions
                        conj
                        (second e))
                result)))
     result)))

(def test-code '((ns
                   me.lomin.namure.core-test
                   (:require
                     [clojure.test :refer :all]
                     [me.lomin.namure.core :as namure]
                     [clojure.edn :as edn]))
                  (defn
                    read-namespace-of
                    [{file-str :file}]
                    (with-open
                      [rdr
                       (new
                         java.io.PushbackReader
                         (clojure.java.io/reader (clojure.java.io/resource file-str)))]
                      (vec (take-while some? (repeatedly (partial read {:eof nil} rdr))))))))

(deftest finds-all-user-functions-test
  (is (= #{'read-namespace-of}
         (:functions (sym-xs->tree test-code)))))

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

(deftest finds-all-function-dependencies-test
  (is (= #{'with-open 'take-while}
         (get-f-dependencies (second test-code)
                             #{'with-open 'take-while 'frequencies}))))
