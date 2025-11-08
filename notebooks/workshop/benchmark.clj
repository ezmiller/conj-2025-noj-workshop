(ns workshop.benchmark
  (:require [criterium.core :as c]
            [tech.v3.dataset.reductions :as tmdr]
            [clojure.core :as core]))

(defmacro quick-bench-str
  "Performs a quick benchmark and returns the cleaned string output."
  [expr]
  `(let [benchmark-output# (with-out-str (c/quick-bench ~expr))]
     (or (second (re-find #"(?m)^.*(Execution time mean : .*)\n" benchmark-output#))
         "Could not extract mean time.")))

;; benchmark scale
(def N 1000000)

(defn make-synthetic-data [n]
  (map (fn [i]
         {:A (rand) ; Numeric column A
          :B (rand) ; Numeric column B
          :G (str (mod i 20))}) ; Grouping column G (20 unique groups)
       (range n)))


(defn time-map-group-by [data]
  (->> data
       ;; Grouping over maps requires iterating all records
       (core/group-by :G)
       (map (fn [[group-key rows]]
              {:G group-key
               ;; Aggregation forces iteration and likely intermediate boxing [3]
               :sum-A (core/reduce + (core/map :A rows))}))
       core/doall))
;; This approach leads to an "explosion of intermediate types" and millions of garbage objects.
;; It loses the benefit of CPU vectorization 

(comment 
(defn time-ds-group-by [ds-data]
  (tmdr/group-by-column-agg
   :G ; Grouping column
   {:sum-A (tmdr/sum :A)} ; Correct aggregation map
   ds-data))

;; The design of this function is a key reason why benchmarks are fast [9].
;; It goes through exactly one column (:G) to produce indexes and performs the aggregation in parallel [8].
;; It produces only a constant amount of regardless of N.

;; A. Row Major Structure: Standard Clojure sequence of maps
;; This requires 'doall' to force the realization of the lazy sequence before the benchmark runs.
(def row-data (core/doall (make-synthetic-data N))) 
;; This structure is almost ten times larger in memory than the dataset equivalent [2].

;; B. Column Major Structure: tech.ml.dataset
(def ds-data (tmd/->dataset row-data)) 

;; ### Benchmark Row Major

;; Expected result: Slower due to boxing, garbage creation, and lack of vectorization [3, 6].

(c/quick-bench (time-map-group-by row-data))

;; ### Benchmark Column Major 

;; Expected result: Significantly faster; the dataset library "dominates everything else by factors" [10].

(c/quick-bench (time-ds-group-by ds-data))

)
