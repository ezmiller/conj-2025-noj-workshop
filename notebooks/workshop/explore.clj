(ns workshop.explore
  (:require [tablecloth.api :as tc] 
            [scicloj.tableplot.v1.plotly :as plotly]
            [scicloj.kindly.v4.kind :as kind]
            [java-time :as jt]
            [clojure.core :as c]))

(def workshop-data
  (tc/dataset "data/clt-311-workshop.csv" {:key-fn keyword}))

;; Let's first check out what are the most common
;; request types.
(-> workshop-data
    (tc/group-by :REQUEST_TYPE)
    (tc/aggregate {:COUNT tc/row-count})
    (tc/order-by :COUNT :desc)
    (tc/head 10))

;; To visualize this we can plot them using Noj's tableplot tool.
;; Tableplot is a wrapper around plotting libraries. The backend we will
;; use is plotly, which is a popular plotting library in Javascript.
;; Here too the plotly api expects the resulting object in the first
;; position, and we can override the result. 
(-> workshop-data
    (tc/group-by [:REQUEST_TYPE])
    (tc/aggregate {:COUNT tc/row-count})
    (tc/order-by :COUNT :desc)
    (tc/head 10)
    (plotly/base {:=title "Top 10 Request Types"})
    (plotly/layer-bar {:=x :REQUEST_TYPE
                       :=y :COUNT}))

;; What might be interseting now is to look at some of these requests over time. 

;; What time fields do we have?
(tc/column-names workshop-data)

;; Let's take :RECEIVED_DATA. We need to parse it as a data. For that we can use
;; java-time which is included in Noj as well. Java time is a wrapper over Java
;; 8's date time API.
(def src-fmt
  (jt/formatter "yyyy/MM/dd HH:mm:ssX"))

;; Let's test it out
(-> workshop-data
    :RECEIVED_DATE
    first
    (->> (jt/local-date src-fmt)))

;; So now let's try to plot some of these request types over time. We'll take
;; the top five request types as set. 
(def top-five-requests
  (-> workshop-data
      (tc/group-by [:REQUEST_TYPE])
      (tc/aggregate {:COUNT tc/row-count})
      (tc/order-by :COUNT :desc)
      (tc/head 5)
      :REQUEST_TYPE
      set))

;; And we'll want to roll these request counts up by some cadence. Let's try
;; monthly by year. To do that we roll up date around the first day of each
;; month.

;; Let's first build a function that can convert time that way
(defn ->first-day-of_month [str]
  (->> str
       (jt/local-date src-fmt)
       (#(jt/adjust % :first-day-of-month))))

;; Test that it works. 
(-> workshop-data
    :RECEIVED_DATE
    first
    ->first-day-of_month)

;; Now let's work on the data prep
(-> workshop-data
    ;; first we want to select the samples that interest us
    (tc/select-rows
     (fn [row]
       (-> row
           :REQUEST_TYPE
           top-five-requests)))
    ;; now let's add a column for the roll up
    (tc/add-column
     :FIRST_DAY_OF_MONTH
     (fn [ds]
       (map ->first-day-of_month (:RECEIVED_DATE ds))))
    ;; then we can do our grouping
    (tc/group-by [:REQUEST_TYPE :FIRST_DAY_OF_MONTH])
    (tc/aggregate {:COUNT tc/row-count}))

;; now we can plot but let's package this data up
(def year-month-data
  (-> workshop-data
      ;; first we want to select the samples that interest us
      (tc/select-rows
       (fn [row]
         (-> row
             :REQUEST_TYPE
             top-five-requests)))
      ;; now let's add a column for the roll up
      (tc/add-column
       :FIRST_DAY_OF_MONTH
       (fn [ds]
         (map ->first-day-of_month (:RECEIVED_DATE ds))))
      ;; then we can do our grouping
      (tc/group-by [:REQUEST_TYPE :FIRST_DAY_OF_MONTH])
      (tc/aggregate {:COUNT tc/row-count})
      (tc/order-by :FIRST_DAY_OF_MONTH)))

;; Now let's plot it using tableplot + plotly. We'll try the layer-line function
;; first
(-> year-month-data
    (tc/order-by :FIRST_DAY_OF_MONTH)
    (plotly/layer-line
     {:=x :FIRST_DAY_OF_MONTH
      :=color :REQUEST_TYPE
      :=y :COUNT}))

;; Still maybe we can improve the formatting a bit, move the legend
;; above.
(-> year-month-data 
    (plotly/base
     {:=layout  {:legend
                {:orientation "h"
                 :y 1.5}}})
    (plotly/layer-line
     {:=x :FIRST_DAY_OF_MONTH
      :=color :REQUEST_TYPE
      :=y :COUNT}))

;; One can see a pattern here, but it's a bit hard to tell.  Let's try
;; collapsing the year data and simply grouping by the month. We could also
;; exclude 2020 (Covid!)
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
    (tc/group-by [:REQUEST_TYPE :MONTH])
    (tc/aggregate {:COUNT tc/row-count})
    (plotly/base
     {:=layout {:xaxis {:dtick "M2"}
                :legend
                {:orientation "h"
                 :y 1.5}}})
    (plotly/layer-line
     {:=x :MONTH
      :=color :REQUEST_TYPE
      :=y :COUNT}))

;; We'll wrap this intitial exploration up soon so there is time for you all to
;; get your hands dirty with this, but one further observation here that is
;; interesting. We saw above that the COVID year 2020 caused a great a large
;; irregularity. Interestingly, we can see that show up here by excluding the
;; outlier data from that year, we can see that the composite pattern of the
;; yearly cycles changes here rather significantly.

(-> month-data
    (tc/select-rows
     (fn [row]
       (-> row
           :FISCAL_YEAR
           (not= 2020))))
    (tc/group-by [:REQUEST_TYPE :MONTH])
    (tc/aggregate {:COUNT tc/row-count})
    (plotly/base
     {:=layout {:xaxis {:dtick "M2"}
                :legend
                {:orientation "h"
                 :y 1.5}}})
    (plotly/layer-line
     {:=x :MONTH
      :=color :REQUEST_TYPE
      :=y :COUNT}))
