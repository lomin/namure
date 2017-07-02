# namure

Visualizes direct dependencies of clojure functions.

## Leiningen

*namure* is available from Clojars.  Add the following dependency to your *project.clj*:

[![Clojars Project](https://img.shields.io/clojars/v/me.lomin/namure.svg)](https://clojars.org/me.lomin/namure)
## Usage ##

### Example 1 ###

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

### Example 2 ###
```clojure
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
                                               my-past-actions))))))
                                               
 =>
                                               
                                    +--------+
                                    | answer |
                                    +---+----+
                                        |
                     +------------------+------------------+
                     |                                     |
               +-----+------+                        +-----+------+
               | decide-act |                        | make-execu |
               |    ion     |                        | tion-plan  |
               +-----+------+                        +-----+------+
                     |                                     |
      +--------------+--------------+             +--------++--------+
      |              |              |             |         |        |
+-----+------+ +-----+------+ +-----+------+ +----+----+ +--+---+ +--+---+
| check-or-c | | decide-act | | i-have-the | | archive | | init | | play |
|    all     | | ion-when-i | | -best-poss | +---------+ +------+ +------+
+------------+ | -have-not- | | ible-hand  |
               | the-best-p | +-----+------+
               | ossible-ha |       |
               |     nd     |       +-----+-------------+-----------+
               +-----+------+             |             |           |
                     |              +-----+------+ +----+----+ +----+----+
               +-----+-----+        | calculate- | | combine | | ranking |
               |           |        | best-hand- | +---------+ +---------+
            +--+---+ +-----+------+ |  possible  |
            | ev+? | | time-to-bl | +------------+
            +------+ |    uff?    |
                     +------------+
```

## Limitations

Only visualizes dependencies from the same namespace.

## About

The word Namure is a combination of Namu (나무, meaning tree in Korean) and Clojure.

## License

Copyright © 2017 Steven Collins

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.