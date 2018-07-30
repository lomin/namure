(ns me.lomin.namure.core
  (:require [clojure.string :as string])
  (:import (java.io PushbackReader)))

(declare make-tree*)

(defn to-relative-resource [ns]
  (string/join "/" (string/split ns #"\.")))

(defn to-file-str [sym]
  {:file (-> sym
             (name)
             (to-relative-resource)
             (string/replace #"-" "_")
             (str ".clj"))})

(defn read-namespace-of [{file-str :file}]
  (try
    (with-open [rdr (new PushbackReader
                         (clojure.java.io/reader (clojure.java.io/resource file-str)))]
      (vec (take-while some?
                       (repeatedly (partial read {:eof nil} rdr)))))
    (catch Exception _
      [])))

(defn make-tree-from-sym [ns-sym sym result]
  (->> ns-sym
       (to-file-str)
       (read-namespace-of)
       (make-tree* result ns-sym sym)))

(defn get-user-functions [m]
  (set (remove #(contains? #{:refer :as} %) (keys m))))

(defn get-f-dependencies
  ([xs result]
   (get-f-dependencies xs
                       (get-user-functions result)
                       (select-keys result [:as :refer])
                       #{}))
  ([xs user-functions ns-m result]
   (let [r* (volatile! result)]
     (clojure.walk/prewalk #(if-let [f (and (sequential? %) (first %))]
                              (do (cond
                                    (and (symbol? f)
                                         (namespace f)
                                         (get-in ns-m [:as (symbol (namespace f))]))
                                    (vswap! r* conj f)

                                    (user-functions f)
                                    (vswap! r* conj f))
                                  %)
                              %)
                           xs)
     @r*)))

(defn insert-ns [result f-xs]
  (let [m-collect (volatile! {})]
    (clojure.walk/prewalk #(if (and (sequential? %)
                                    (= :require (first %)))
                             (let [[_ & args] %]
                               (doseq [[ns & pair-seq] args]
                                 (let [pairs (partition 2 pair-seq)
                                       require-map (reduce (fn [m* [k v :as x]]
                                                             (condp = k
                                                               :as (assoc m* k v)
                                                               :refer (update m* k into (if (= v :all) [] v))))
                                                           {}
                                                           pairs)]

                                   (when (:as require-map) (vswap! m-collect assoc-in [:as (:as require-map)] ns))
                                   (when (:refer require-map) (doseq [r (:refer require-map)]
                                                                (vswap! m-collect assoc-in [:refer r] (make-tree-from-sym ns r result)))))))
                             %)
                          f-xs)
    (merge result @m-collect)))

(defn add-ns-prefix [ns-sym xs]
  (mapv #(if (symbol? %)
           (symbol (name ns-sym) (name %))
           %)
        xs))

(defn get-from-result [{:keys [as refer] :as result} ns-sym f]
  (if (namespace f)
    (let [ns-sym (get as (symbol (namespace f)))
          raw-sym (symbol (name f))]
      (add-ns-prefix (symbol (namespace f))
                     (first (make-tree-from-sym ns-sym raw-sym result))))
    (if ns-sym
      (add-ns-prefix ns-sym (get result f))
      (get result f))))

(defn sym-xs->namure
  ([xs] (sym-xs->namure xs nil))
  ([xs ns-sym]
   (sym-xs->namure xs ns-sym {}))
  ([xs ns-sym result]
   (if (seq xs)
     (recur (next xs)
            ns-sym
            (let [f-xs (first xs)]
              (cond
                (= 'defn (first f-xs)) (assoc result
                                              (second f-xs)
                                              (into [(second f-xs)] (map (partial get-from-result result ns-sym)
                                                                         (sort-by str (get-f-dependencies f-xs
                                                                                                          result)))))
                (= 'ns (first f-xs)) (insert-ns result f-xs)
                :else result)))
     result)))

(defn make-tree*
  ([result ns-sym sym xs]
   [(get (sym-xs->namure xs ns-sym result) sym)])
  ([sym xs]
   (make-tree* {} nil sym xs)))

(defmacro make-tree [sym]
  `[(-> ~sym
        (var)
        (meta)
        (read-namespace-of)
        (sym-xs->namure)
        (get (symbol (:name (meta (var ~sym))))))])
