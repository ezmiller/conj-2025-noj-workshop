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
  (make-synthetic-data N))

(take 3 clj-data)
;;=> ({:A 0.4095895733406444, :B 0.25517276910804876, :G "0"}
;;    {:A 0.009452408966254544, :B 0.005903529809523689, :G "1"}
;;    {:A 0.47752798731142865, :B 0.6525807580073596, :G "2"})

;; First we'll benchmark using standard data structures. Importantly, this is
;; also a row-major operation. We are detaling with a sequence of maps, where
;; each map is a row. We'll come back to this in a second when we talk about 
;; the results.
(quick-bench-str
 (->> clj-data
      (group-by :G)
      (map (fn [[group-key rows]]
             {:group-key group-key
              :sum-a (reduce + (map :A rows))}))))

;; A dataset -- a tabular structure (rows & columns), a frankly ancient way to
;; think about data, but also what modern data scientits so frequently use in
;; popular libraries like PANDAS (dataframe).
(def ds-data
  (tc/dataset (make-synthetic-data N)))

(quick-bench-str
 (-> ds-data
     (tc/group-by :G)
     (tc/aggregate {:sum-a #(tcc/sum (:A %))})))
;;=> "Execution time mean : 49.319388 ms"

;; What accounts for this speed up? 
;; - Row-major (sequences of maps) versus column-major (datasets!) 
;; - Don't need to carry column names in each row. Operate on column.
;; - Instead of dealing with 1 million map objects we have three column objects
;; - Strongly typed and packed in continuous memory enables optimizations

;; Clojure isn't slow but the dataset structure enables further optimizations.
;; For many uses in
;; a circumstance in which we often want to know exactly what our data within
;; each column is. 

;; ## Let's peer into the stack. Starting with the dataset itself.

;; What is the ds-data?
(type ds-data)

;; But the dataset itself is just a map
(map? ds-data)

;; It's keys are the column headers.
(keys ds-data)

;; It's values are the columns.
(vals ds-data)

;; If we look at it's type. it is a column as defined by 
;; tech.ml.dataset
(-> ds-data
    tc/columns
    first
    type)

;; Column Properties - like vectors
(def a-column
  (ds-data :A))

;; Random access
(nth a-column 5)

;; Countable
(count a-column)

;; Conj-able
(-> a-column
    (conj -9999)
    count)

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

;; It is another special type that provides a view onto the packed typed arrays.
;; We can interact with this data still as a sequence of maps but underneath
;; what we have are packed type arrays that enable radical optimziation.
