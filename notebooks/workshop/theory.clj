(ns workshop.theory
  (:require [workshop.benchmark :refer [make-synthetic-data quick-bench-str]]
            [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]))

;; ## Efficiency

;; The Clojure data stack evolved organically out of work on large amounts of
;; data. Normally, as we'll see, you'd work with sequences of maps. And in many
;; cases that is fine. But sometimes, you run into limits. Let's take a look.

(def N 10000000)

;; A sequence of maps - row major
(def clj-data
  (doall (make-synthetic-data N)))

(take 3 clj-data)
;;=> ({:A 0.1609610807238414, :B 0.13091527472290998, :G "0"}
;;    {:A 0.5076005921426744, :B 0.4274085497119422, :G "1"}
;;    {:A 0.36144653864047704, :B 0.2868503683146011, :G "2"})

;; First we'll benchmark using standard data structures. Importantly, this is
;; also a row-major operation. We are detaling with a sequence of maps, where
;; each map is a row. We'll come back to this in a second when we talk about 
;; the results.
(quick-bench-str
 (->> clj-data
      (group-by :G)
      (map (fn [[group-key rows]]
             {:group-key group-key
              :sum-a (reduce + (map :A rows))}))
      doall ;; result is a lazy sequence, results are deferred unless we do this
      ))
;;=> "Execution time mean : 1.231470 sec"

(def ds-data
  (tc/dataset (make-synthetic-data N)))

(quick-bench-str
 (-> ds-data
     (tc/group-by :G)
     (tc/aggregate {:sum-a #(tcc/sum (:A %))})))
;;=> "Execution time mean : 50.273668 ms"

;; What accounts for this speed up? 
;; - Row-major (sequences of maps) versus column-major (datasets!) 
;; - Don't need to carry column names in each row. Operate on column.
;; - Instead of dealing with 1 million map objects we have three column objects
;; - Strongly typed and packed in continuous memory enables optimizations

;; Clojure isn't slow but the dataset structure enables further optimizations in
;; a circumstance in which we often want to know exactly what our data within
;; each column is. 

;; ## Let's peer into the stack. 

;; What is the ds-data?

(type ds-data)

;; But the dataset itself is just a map
(map? ds-data)

;; It's keys are the column headers.
(keys ds-data)

;; It's values are the columns.
(vals ds-data)

;; If we look at it as rows it's also still a sequence of maps.
;; So we can also think of this data as a sequence of maps still!
(-> ds-data
    (tc/rows :as-maps)
    (->> (take 5))
    (->> (map map?)))

;; You can assoc onto this map!
(-> ds-data
    (tc/rows :as-maps)
    first
    (assoc :z 10))

;; But actually this is not just a map.
(-> ds-data
    (tc/rows :as-maps)
    first
    type)

;; It is another special type -- a view -- onto the packed typed arrays. We can
;; interact with this data still as a sequence of maps but underneath what we
;; have are packed type arrays that enable radical optimziation.
