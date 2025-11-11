(ns workshop.benchmark
  (:require [criterium.core :as c]
            [clojure.core :as core]))

(defmacro quick-bench-str
  "Performs a quick benchmark and returns the cleaned string output."
  [expr]
  `(let [benchmark-output# (with-out-str (c/quick-bench ~expr))]
     (or (second (re-find #"(?m)^.*(Execution time mean : .*)\n" benchmark-output#))
         "Could not extract mean time.")))

(defn make-synthetic-data [n]
  (map (fn [i]
         {:A (rand) ; Numeric column A
          :B (rand) ; Numeric column B
          :G (str (mod i 20))}) ; Grouping column G (20 unique groups)
       (range n)))

