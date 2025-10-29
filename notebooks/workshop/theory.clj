(ns workshop.theory
  (:require [workshop.benchmark :refer [make-synthetic-data quick-bench-str]]
            [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]))


;; ## Efficiency

;; The Clojure data stack evolved out of the work of a consulting company
;; of people who really love Clojure! They started out solving data problems
;; simply by working with Clojure data structures, sequences of maps. 

(def N 10000000)

(def clj-data
  (doall (make-synthetic-data N)))

(first clj-data)

(quick-bench-str
 (->> clj-data
      (group-by :G)
      (map (fn [[group-key rows]]
             {:group-key group-key
              :sum-a (reduce + (map :A rows))}))
      doall))


(def ds-data
  (tc/dataset (make-synthetic-data N)))

(first ds-data)

(quick-bench-str
 (-> ds-data
     (tc/group-by :G)
     (tc/aggregate {:sum-a #(tcc/sum (:A %))})))

;; TBD: Add screenshot of Chris N's comparison with other libs?

;; What accounts for this speed up? When we use core Clojure data structures, as
;; convenient as they are, we end up with an explosion of intermediate copies
;; that slow down processing when working with large amounts of data. These
;; inefficiences are magnified as the processes become more complex.

;;; ## Looking into the stack

;; TBD The data stack visualization

;; Let's look at these relationships a bit. 

;; What is the ds-data?

(type ds-data)

(map? ds-data)

;; It's keys are the column headers
(keys ds-data)

;; It's values are the columns
(vals ds-data)

;; If we look at it as rows it's also still
;; a sequence of maps
(-> ds-data
    (tc/rows :as-maps)
    (->> (take 5))
    (->> (map map?)))

;; you can assoc!
(-> ds-data
    (tc/rows :as-maps)
    first
    (assoc :z 10))

;; but actually this is not just a map
(-> ds-data
    (tc/rows)
    first
    type)

;; This is a type defined in one of the libraries underlying
;; this that provides a view onto what is the actual way this
;; data is stored: in packed, typed arrays.

;; We'll look at that next.

;; it prints out in this fancy way
(-> ds-data
    tc/columns
    first)

;; if we look at it's type. it is a column as defined by 
;; tech.ml.dataset
(-> ds-data
    tc/columns
    first
    class)

;; but we can also inspect the type of the items it contains
;; using the colun api (tcc)
(-> ds-data
    tc/columns
    first
    tcc/typeof)

;; Properties
(def a-column
  (ds-data :A))

;; Random access
(nth a-column 5)

;; Countable
(count a-column)


