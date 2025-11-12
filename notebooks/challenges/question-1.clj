(ns workshop.question-1
  (:require [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]
            [scicloj.tableplot.v1.plotly :as plotly]
            [java-time :as jt]))

;; ## 1. What is the geographical distribution of requests? Any concentrations?

(def src-fmt
  (jt/formatter "yyyy/MM/dd HH:mm:ssX"))

(defn ->first-day-of_month [str]
  (->> str
       (jt/local-date src-fmt)
       (#(jt/adjust % :first-day-of-month))))

(def workshop-data
  (tc/dataset "data/clt-311-workshop.csv" {:key-fn keyword}))

;; Let's see what geographic data we have
(-> workshop-data
    (tc/select-columns [:COUNCIL_DISTRICT :CMPD_DIVISION :NEIGHBORHOOD_PROFILE_AREA :ZIP_CODE])
    (tc/head 10))

(def top-five-requests
  (-> workshop-data
      (tc/group-by [:REQUEST_TYPE])
      (tc/aggregate {:count tc/row-count})
      (tc/order-by :count :desc)
      (tc/head 5)
      :REQUEST_TYPE
      set))

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


;; Zip code could be useful, but the CMPD_DIVISION is more human understandable
;; These are the Charlotte Police divisons.

(-> month-data
    (tc/select-rows
     (comp #(= % "NON_RECYCLABLE ITEMS") :REQUEST_TYPE))
    (tc/group-by [:CMPD_DIVISION])
    (tc/aggregate {:COUNT tc/row-count})
    (tc/order-by :COUNT)
    (plotly/layer-bar
     {:=x :CMPD_DIVISION
      :=y :COUNT}))

;; That's interesting b/c we see a big bump in SOUTH. Looking up South, this is
;; an area of relative affluence and single-family homes. Could be there's an
;; association of recycling with affluence and/or home types? Maybe also if
;; these are homes the acquire more and throw out more? 


