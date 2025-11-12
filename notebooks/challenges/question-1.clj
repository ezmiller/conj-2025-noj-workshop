(ns workshop.question-1
  (:require [tablecloth.api :as tc] 
            [scicloj.tableplot.v1.plotly :as plotly]))

;; ## 1. What is the geographical distribution of requests? Any concentrations?

;; Let's see what geographic data we have
(-> workshop-data
    (tc/select-columns [:COUNCIL_DISTRICT :CMPD_DIVISION :NEIGHBORHOOD_PROFILE_AREA :ZIP_CODE])
    (tc/head))

;; Zip code could be useful, but the CMPD_DIVISION is more human understandable
;; These are the Charlotte Police divisons.

(-> workshop-data 
    (tc/group-by [:CMPD_DIVISION])
    (tc/aggregate {:COUNT tc/row-count})
    (tc/order-by :COUNT)
    (plotly/layer-bar
     {:=x :CMPD_DIVISION
      :=y :COUNT}))

;; That's interesting b/c we see a big bump in SOUTH. Looking up South, this is
;; an area of relative affluence and single-family homes. What would happen if
;; we excluded the recyclable items requests?

(-> workshop-data
    (tc/select-rows
       (comp #(not= % "NON_RECYCLABLE ITEMS") :REQUEST_TYPE))
    (tc/group-by [:CMPD_DIVISION])
    (tc/aggregate {:COUNT tc/row-count})
    (tc/order-by :COUNT)
    (plotly/layer-bar
     {:=x :CMPD_DIVISION
      :=y :COUNT}))

;; So this concentration in South does seem to come from recyclable items.


