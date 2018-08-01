(ns me.lomin.namure.core
  (:require [clojure.string :as string])
  (:import (java.io PushbackReader)))

(declare make-tree*)

(defn only-if [pred f]
  #(if (pred %) (f %) %))

(defn only-if-let [pred f]
  #(if-let [x (pred %)] (f x %) %))

(defn first* [x]
  (and (sequential? x) (first x)))

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

(defn collect-dependencies [vresult user-functions ns-m f x]
  (do (cond
        (and (symbol? f)
             (namespace f)
             (get-in ns-m [:as (symbol (namespace f))]))
        (vswap! vresult conj f)

        (user-functions f)
        (vswap! vresult conj f))
      x))

(defn get-f-dependencies
  ([xs result]
   (get-f-dependencies xs
                       (get-user-functions result)
                       (select-keys result [:as :refer])
                       #{}))
  ([xs user-functions ns-m result]
   (let [vresult (volatile! result)]
     (clojure.walk/prewalk (only-if-let first*
                                        (partial collect-dependencies
                                                 vresult
                                                 user-functions
                                                 ns-m))
                           xs)
     @vresult)))

(defn require-expr? [x]
  (and (sequential? x) (= :require (first x))))

(defn make-require-map [pair-seq]
  (reduce (fn [m [k v]]
            (condp = k
              :as (assoc m k v)
              :refer (update m k into (if (= v :all) [] v))))
          {}
          (partition 2 pair-seq)))

(defn insert-ns
  ([result f-xs]
   (let [m-collect (volatile! {})]
     (clojure.walk/prewalk (only-if require-expr?
                                    (partial insert-ns m-collect result))
                           f-xs)
     (merge result @m-collect)))
  ([m-collect result require-expr]
   (let [[_ & args] require-expr]
     (doseq [[ns & pair-seq] args]
       (let [require-map (make-require-map pair-seq)]
         (when (:as require-map) (vswap! m-collect assoc-in [:as (:as require-map)] ns))
         (when (:refer require-map) (doseq [r (:refer require-map)]
                                      (vswap! m-collect
                                              assoc-in
                                              [:refer r]
                                              (make-tree-from-sym ns r result)))))))))

(defn add-ns-prefix [ns-sym xs]
  (mapv (only-if symbol? #(symbol (name ns-sym) (name %)))
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

(defn insert-defn [result f-xs ns-sym]
  (assoc result
    (second f-xs)
    (into [(second f-xs)]
          (map (partial get-from-result result ns-sym)
               (sort-by str
                        (get-f-dependencies f-xs
                                            result))))))

(defn sym-xs->namure
  ([xs] (sym-xs->namure xs nil))
  ([xs ns-sym]
   (sym-xs->namure xs ns-sym {}))
  ([xs ns-sym result]
   (if (seq xs)
     (recur (next xs)
            ns-sym
            (let [[f-sym :as f-xs] (first xs)]
              (condp = f-sym
                'defn (insert-defn result f-xs ns-sym)
                'ns (insert-ns result f-xs)
                result)))
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