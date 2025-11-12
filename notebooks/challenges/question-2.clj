(ns challenges.question-2
  (:require [tablecloth.api :as tc]
            [java-time :as jt]
            [scicloj.tableplot.v1.plotly :as plotly]))

(def workshop-data
  (tc/dataset "data/clt-311-workshop.csv"
              {:key-fn keyword}))

;; ## 2. Do requests peak on weekends versus weekdays? What are the weekly versus weekend rates?

(def src-fmt
  (jt/formatter "yyyy/MM/dd HH:mm:ssX"))

(defn ->datetime [date-str]
  (->> date-str
      (jt/local-date src-fmt)))

(-> workshop-data
    :RECEIVED_DATE
    first
    ->datetime
    jt/day-of-week
    str)

(defn ->day-of-week [date-str]
  (->> date-str
       (jt/local-date src-fmt)
       (jt/day-of-week)))

;; bar chart showing counts for days of week
(-> workshop-data
    (tc/add-column :DAY_OF_WEEK
                   (fn [ds]
                     (map ->day-of-week (:RECEIVED_DATE ds))))
    (tc/group-by [:DAY_OF_WEEK])
    (tc/aggregate {:COUNT tc/row-count})
    (plotly/layer-bar 
     {:=x :DAY_OF_WEEK
      :=y :COUNT}))



