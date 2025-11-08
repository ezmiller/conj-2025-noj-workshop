(ns workshop.live-explore
  (:require [tablecloth.api :as tc]
            [scicloj.tableplot.v1.plotly :as plotly]
            [java-time :as jt]))

(def workshop-data
  (tc/dataset "notebooks/data/clt-311-workshop.csv"
              {:key-fn keyword}))

(tc/head workshop-data)

(-> workshop-data
    (tc/group-by [:REQUEST_TYPE])
    (tc/aggregate {:COUNT tc/row-count})
    (tc/order-by :COUNT :desc))

(-> workshop-data
    (tc/group-by [:REQUEST_TYPE])
    (tc/aggregate {:COUNT tc/row-count})
    (tc/order-by :COUNT :desc)
    (tc/head 10)
    (plotly/base {:=title "Top 10 Request Types"})
    (plotly/layer-bar {:=x :REQUEST_TYPE
                       :=y :COUNT}))

(tc/column-names workshop-data)

(-> workshop-data
    :RECEIVED_DATE
    first)

(def src-fmt
  (jt/formatter "yyyy/MM/dd HH:mm:ssX"))

(-> workshop-data
    :RECEIVED_DATE
    first
    (->> (jt/local-date src-fmt)))

(def top-five-requests
  (-> workshop-data
      (tc/group-by [:REQUEST_TYPE])
      (tc/aggregate {:COUNT tc/row-count})
      (tc/order-by :COUNT :desc)
      (tc/head 5)
      :REQUEST_TYPE
      set))

top-five-requests


(top-five-requests "NONSENSE")
(top-five-requests "NON_RECYCLABLE ITEMS")

(defn ->first-day-of_month [str]
  (->> str
       (jt/local-date src-fmt)
       (#(jt/adjust % :first-day-of-month))))

(-> workshop-data
    :RECEIVED_DATE
    first
    ->first-day-of_month)

(def year-month-data
  (-> workshop-data
      (tc/select-rows
       (fn [row]
         (-> row
             :REQUEST_TYPE
             top-five-requests)))
      ;; roll up the data around each year month
      (tc/add-column
       :FIRST_DAY_OF_MONTH
       (fn [ds]
         (map ->first-day-of_month (:RECEIVED_DATE ds))))
      (tc/group-by [:REQUEST_TYPE :FIRST_DAY_OF_MONTH])
      (tc/aggregate {:COUNT tc/row-count})
      (tc/order-by :FIRST_DAY_OF_MONTH)))

(-> year-month-data
    #_(tc/order-by :FIRST_DAY_OF_MONTH)
    (plotly/layer-line
     {:=x :FIRST_DAY_OF_MONTH
      :=y :COUNT
      :=color :REQUEST_TYPE}))

(-> year-month-data
    (plotly/base {:=layout {:legend {:orientation "h"
                                 :y 1.5}}})
    (plotly/layer-line
     {:=x :FIRST_DAY_OF_MONTH
      :=y :COUNT
      :=color :REQUEST_TYPE}))

(def month-data
  (-> workshop-data
      (tc/select-rows
       (fn [row] (-> row
                     :REQUEST_TYPE
                     top-five-requests)))
      
      ;; this time we'll use map-columns, another way to add a column
      (tc/map-columns :MONTH
                      [:RECEIVED_DATE]
                      (fn [datestr]
                        (->> datestr
                             (jt/local-date src-fmt)
                             jt/month
                             jt/value)))
      
      (tc/order-by :MONTH)))

(-> month-data
    (tc/select-rows
     (fn [row]
       (-> row
           :FISCAL_YEAR
           (not= 2020))))
    
    (tc/group-by [:REQUEST_TYPE :MONTH])
    (tc/aggregate {:COUNT tc/row-count})
    (plotly/base {:=layout {:xaxis {:dtick "M2"}
                            :legend {:orientation "h"
                                     :y 1.5}}}) 
    (plotly/layer-line
     {:=x :MONTH
      :=y :COUNT
      :=color :REQUEST_TYPE}))


