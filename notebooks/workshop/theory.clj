(ns workshop.theory
  (:require [workshop.benchmark :refer [make-synthetic-data quick-bench-str]]
            [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]))

;; ## Efficiency

;; The Clojure data stack evolved organically out of work on large amounts of
;; data. Normally, as we'll see, you'd work with sequences of maps. And in many
;; cases that is fine. But sometimes, you runinto limits. Let's take a look.

(def N 10000000)

(def clj-data
  (doall (make-synthetic-data N)))

(first clj-data)

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
      doall))

(def ds-data
  (tc/dataset (make-synthetic-data N)))

(first ds-data)

(quick-bench-str
 (-> ds-data
     (tc/group-by :G)
     (tc/aggregate {:sum-a #(tcc/sum (:A %))})))

;; What accounts for this speed up? Several things:
;;  - In the row-major approach, we hold a sequence of maps. In each of those
;;    maps, we hold the column names as labels for each value. We don't need to
;;    do that in the column-major approach. 
;;  - When we use core Clojure data structures, as convenient as they are, we
;;    end up with an explosion of intermediate copies that slow down processing
;;    when working with large amounts of data. For many uses this is just fine.
;;    These inefficiences are magnified as the processes become more complex.
;;  - The dataset data structures are optimized to avoid the explosion of
;;    intermediate copies that woudl normally occur. Moreover, as we'll see, the
;;    columns are strongly typed and packed in continuous memory, which opens up
;;    further optimizations.

;; Is Clojure slow? The point here is not that Clojure is slow. In many cases,
;; clojure data structures are exactly what you want. But sometimes you need
;; further optimization when you are operating over millions or billions of
;; items. Moreover, sometimes you need the mental model of a tabular dataset
;; where you can know the type of each column.



;; ## Quick tour down the stack

;; Okay so now we are going to do a quick exploration of the values we were just
;; using for the benchmark and look at their types. This will hopefully show some
;; of the relationships in this tech stack. 

;; What is the ds-data?

(type ds-data)

;; What is a dataset? It is just a tabular arrangement of data, like a
;; spreadsheet or a DB table in a sense but in a form that is processable
;; programatically. Many data scientists use this type of structure in Python's
;; Pandas library, where such a structure is called a DataFrame. So table
;; ergonomics are common in data analysis, and in Clojure's toolkit we call it
;; a dataset.

(map? ds-data)

;; It's keys are the column headers.
(keys ds-data)

;; It's values are the columns.
(vals ds-data)

;; If we look at it as rows it's also still a sequence of maps
(-> ds-data
    (tc/rows :as-maps)
    (->> (take 5))
    (->> (map map?)))

;; You can assoc onto this map!
(-> ds-data
    (tc/rows :as-maps)
    first
    (assoc :z 10))

;; But actually this is not just a map...
(-> ds-data
    (tc/rows)
    first
    type)

;; This is a type defined in one of the libraries underlying
;; this that provides a view onto what is the actual way this
;; data is stored: in packed, typed arrays. 

;; Now we'll look at these arrays or "columns" as we call them in the dataset context.
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
