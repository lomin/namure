# namure

Visualizes direct dependencies of clojure functions.

## Usage ##

```clojure
(ns com.company.core-test
  (:require [me.lomin.namure.core :as namure]
            [vijual]))
  
(defn my-helper-fn0 [x]
  ...)
             
(defn my-helper-fn1 [x]
  ...)
            
(defn my-fn []
  (my-helper-fn1 (my-helper-fn0 (+ 1 2))))

(vijual/draw-tree (namure/make-tree my-fn))
```

### Example ###
```clojure
TODO
```

# About

Namu (나무) means tree in Korean.

## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
