(ns workshop.question-1
  (:require [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]
            [scicloj.tableplot.v1.plotly :as plotly]
            [java-time :as jt]))



(def workshop-data
  (tc/dataset "notebooks/data/clt-311-workshop.csv" {:key-fn keyword}))


looks like and that stands to
;; reason. What can we find in the geographical concentration of these requests?

;; Let's see what geographic data we have
(-> workshop-data
    (tc/select-columns [:COUNCIL_DISTRICT :CMPD_DIVISION :NEIGHBORHOOD_PROFILE_AREA :ZIP_CODE])
    (tc/head 10))

;; Zip code could be useful, but the CMPD_DIVISION is more human understandable
;; These are the Charlotte Police divisons.

(-> month-data
    (tc/select-rows
     ;; we can try another method use comp here
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

;; First we'll just package up the request type we are looking at
(def non-recyclable-data
  (-> month-data
      (tc/select-rows
       ;; we can try another method use comp here
       (comp #(= % "NON_RECYCLABLE ITEMS") :REQUEST_TYPE))))

;; Then we will define a helper to let us distinguish summer
(defn summer-month? [month-number]
  (and (>= month-number 6)
       (< month-number 9)))

;; We'll group by summer months as well
(-> non-recyclable-data
    (tc/add-column
     :SUMMER_MONTH?
     (comp #(map summer-month? %) :MONTH))
    (tc/group-by [:CMPD_DIVISION :SUMMER_MONTH?])
    (tc/aggregate {:COUNT tc/row-count})
    (tc/order-by :COUNT)
    (plotly/layer-bar
     {:=x :CMPD_DIVISION
      :=y :COUNT
      :=color :SUMMER_MONTH?
      :=barmode "group"}))

;; Okay but this is strange right? What's wrong here? 

;; Summer numbers are way less, but this isn't supriring. There are fewer summer
;; months! So what can we do? Well, we can use a monthly rate, i.e. the avg rate
;; during summer months versus non-summer. 

(-> non-recyclable-data

    ;; we did this above
    (tc/add-column
     :SUMMER_MONTH?
     (comp #(map summer-month? %) :MONTH))
    (tc/group-by [:CMPD_DIVISION :SUMMER_MONTH?])
    (tc/aggregate {:COUNT tc/row-count})
    (tc/order-by :COUNT)

    ;; now we can add a column for convenience with number of
    ;; months in the season 
    (tc/add-column
     :MONTHS_IN_SEASON
     (fn [ds]
       (map {true 3 false 9}
            (:SUMMER_MONTH? ds))))

    ;; Then we can do the calculation using tablecloth's column api
    (tc/add-column
     :SEASONAL_RATE
     (fn [ds]
       (tcc// (ds :COUNT)
              (ds :MONTHS_IN_SEASON))))
    (tc/order-by :SEASONAL_RATE)

    ;; now we plot
    (plotly/layer-bar
     {:=x :CMPD_DIVISION
      :=y :SEASONAL_RATE
      :=color :SUMMER_MONTH?
      :=barmode "group"}))

