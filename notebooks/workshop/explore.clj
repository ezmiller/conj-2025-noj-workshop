(ns workshop.explore
  (:require [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]
            [scicloj.tableplot.v1.plotly :as plotly]
            [java-time :as jt]
            [clojure.core :as c]))

(def workshop-data
  (tc/dataset "notebooks/data/clt-311-workshop.csv" {:key-fn keyword}))

;; Let's first check out what are the most common
;; request types.
(-> workshop-data
    (tc/group-by :REQUEST_TYPE)
    (tc/aggregate {:count tc/row-count})
    (tc/order-by :count :desc)
    (tc/head 10))

;; To visualize this we can plot them using Noj's tableplot tool.
(-> workshop-data
    (tc/group-by :REQUEST_TYPE)
    (tc/aggregate {:count tc/row-count})
    (tc/order-by :count :desc)
    (tc/head 10)
    (plotly/base {:=title "Top 10 Request Types"})
    (plotly/layer-bar {:=x :$group-name
                       :=mark-text :$group-name
                       :=y :count}))

;; So what might be interseting now is to look at some of these 
;; requests over time. 

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
    (->> (jt/offset-date-time src-fmt)))

;; So now let's try to plot some of these request types over time. We'll take
;; the top five request types as set. 
(def top-five-requests
  (-> workshop-data
      (tc/group-by :REQUEST_TYPE)
      (tc/aggregate {:count tc/row-count})
      (tc/order-by :count :desc)
      (tc/head 5)
      :$group-name
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
      #_(tc/select-rows
         (fn [row] (-> row :FISCAL_YEAR (not= 2020))))
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

;; Okay so this shows a peak in the spring it looks like and that stands to
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
