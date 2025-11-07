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

;; And we'll want to roll these request counts up by some cadence. Let's
;; try monthly by year.
;; (def to-year-month [datetime]
;;   (java-time/year-month datetime))

;; Now we want to plot these
(def year-month-data
  (-> workshop-data
      (tc/select-rows
       (fn [row] (-> row
                     :REQUEST_TYPE
                     top-five-reqests)))
      (tc/add-column
       :YEAR_MONTH
       (fn [ds]
         (map (fn [str]
                (str (jt/adjust
                      (jt/local-date src-fmt str)
                      :first-day-of-month)))
              (:RECEIVED_DATE ds))))
      (tc/group-by [:REQUEST_TYPE :YEAR_MONTH])
      (tc/aggregate {:COUNT tc/row-count})))

(tc/head year-month-data)

;; Now let's plot it using tableplot + plotly. We'll try the layer-line function
;; first
(-> year-month-data
    (plotly/layer-line
     {:=x :YEAR_MONTH
      :=color :REQUEST_TYPE
      :=y :COUNT}))

;; Okay that's off a bit. We just need to order the data.
(-> year-month-data
    (tc/order-by :YEAR_MONTH)
    (plotly/layer-line
     {:=x :YEAR_MONTH
      :=color :REQUEST_TYPE
      :=y :COUNT}))

;; Still maybe we can improve the formatting a bit, move the legend
;; above.
(-> year-month-data
    (tc/order-by :YEAR_MONTH)
    (plotly/base
     {:=layout {:xaxis {:tickformat "%Y-%m"
                        :dtick "M6"
                        :tickangle -45}
                :legend
                {:orientation "h"
                 :y 1.5}}})
    (plotly/layer-line
     {:=x :YEAR_MONTH
      :=color :REQUEST_TYPE
      :=y :COUNT}))

;; One can see a pattern here, but it's a bit hard to tell.  Let's try
;; collapsing the year data and simply grouping by the month.
(def month-data
  (-> workshop-data
      (tc/select-rows
       (fn [row] (-> row
                     :REQUEST_TYPE
                     top-five-requests)))
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


;; Okay so this shows a peak in the summer for NON_RECYCLABLE_ITEMS.
;; Maybe we can find a geographical concentration that might give us 
;; a hint.

;; let's see what geographic data we have
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

;; That's interesting b/c we see a big bump in SOUTH. Looking this up, this is
;; an area of relative affluence. Also single-family homes. Could be there's an
;; association of recycling with affluence and/or home types? Maybe also if
;; these are homes the acquire more and throw out more?

;; Let's see if there's any difference with respect to summer. 

;; First we'll just package up the request type we are looking at
(def non-recyclable-data
  (-> month-data
      (tc/select-rows
       ;; we can try another method use comp here
       (comp #(= % "NON_RECYCLABLE ITEMS") :REQUEST_TYPE))))

;; Then we will define a helper to let us distinguish summer
(defn summer-month? [month-number]
  (and (>= month-number 6)
       (< month-number 10)))

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

;; But this isn't fair since there are more mnon-summer months. We can use
;; a monthly rate of calls instead.

;; Monthly rate is the avg rate during summer months versus non-summer. 
;; So we'll do a roll up of data
(-> non-recyclable-data
    (tc/add-column
     :SUMMER_MONTH?
     (comp #(map summer-month? %) :MONTH))
    (tc/group-by [:SUMMER_MONTH? :CMPD_DIVISION])
    (tc/aggregate {:COUNT tc/row-count})
    ;; now we can add a column for convenience with number of
    ;; months in the season
    (tc/add-column
     :MONTHS_IN_SEASON
     (fn [ds]
       (map {true 4 false 8}
            (:SUMMER_MONTH? ds))))
    ;; Then we can do the calculation
    (tc/add-column
     :MONTHLY_RATE
     (fn [ds]
       (tcc// (ds :COUNT)
              (ds :MONTHS_IN_SEASON))))
    (tc/order-by :MONTHLY_RATE)
    (plotly/layer-bar
     {:=x :CMPD_DIVISION
      :=y :MONTHLY_RATE
      :=color :SUMMER_MONTH?
      :=barmode "group"})
    )
