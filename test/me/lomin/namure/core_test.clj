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

(defn sym-xs->tree [param1]
  {:functions nil})

(deftest finds-all-user-functions-test
  (is (= [read-namespace-of]
         (:functions (sym-xs->tree '((ns
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
                                          (vec (take-while some? (repeatedly (partial read {:eof nil} rdr))))))))))))
