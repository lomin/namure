(ns me.lomin.namure.core
  (:import (java.io PushbackReader)))

(defn read-namespace-of [{file-str :file}]
  (with-open [rdr (new PushbackReader
                       (clojure.java.io/reader (clojure.java.io/resource file-str)))]
    (vec (take-while some?
                     (repeatedly (partial read {:eof nil} rdr))))))

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

(defn sym-xs->namure
  ([xs]
   (sym-xs->namure xs {}))
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

(defn make-tree* [sym xs]
  [(get (sym-xs->namure xs) sym)])

(defmacro make-tree [sym]
  `[(-> ~sym
        (var)
        (meta)
        (read-namespace-of)
        (sym-xs->namure)
        (get (symbol (:name (meta (var ~sym))))))])
