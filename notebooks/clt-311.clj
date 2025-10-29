;; ## Setup & Data Loading
(ns clt-311
  (:require [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]
            [scicloj.tableplot.v1.plotly :as plotly]
            [java-time :as jt]
            [java-time.format :as fmt]
            [clojure.core :as c]))

(comment
  ;; Evaluate this when you need to reload data
  (ns-unmap *ns* 'raw-data))

;; from: https://data.charlottenc.gov/datasets/charlotte::service-requests-311/about
(defonce raw-data
  (tc/dataset "notebooks/data/Service_Requests_311.csv"
              {:key-fn keyword}))  ; Convert column names to keywords for easier access

;; ## Exploring the Raw Data

(tc/info raw-data)

(tc/head raw-data)

;; ## Creating Workshop Subset

;; Filter to recent years (2020+)
(def recent-311
  (tc/select-rows raw-data
                  (fn [row]
                    (>= (:FISCAL_YEAR row) 2020))))

;; Check the distribution by year
(-> recent-311
    (tc/group-by :FISCAL_YEAR)
    (tc/aggregate {:count tc/row-count})
    (tc/order-by :$group-name))

;; Create balanced sample: 50k from each year
;; This keeps data manageable while preserving variety
(def workshop-data
  (-> recent-311
      (tc/group-by :FISCAL_YEAR)
      (tc/random 50000 {:seed 42 :repeat? false})  ; Add seed for reproducibility
      (tc/ungroup)))

;; Verify what we got
(-> workshop-data
    (tc/group-by :FISCAL_YEAR)
    (tc/aggregate {:count tc/row-count})
    (tc/order-by :$group-name))

;; Save for workshop use
;; (tc/write! workshop-data "notebooks/data/clt-311-workshop.csv")

;; ## Exploratory Analysis

;; What are the most common request types?
(-> workshop-data
    (tc/group-by :REQUEST_TYPE)
    (tc/aggregate {:count tc/row-count})
    (tc/order-by :count :desc)
    (tc/head 10)
    (tc/info :columns))

;; Now SciNoj also has a plotting feature. Lets' plot these distributions.
(-> workshop-data
    (tc/group-by :REQUEST_TYPE)
    (tc/aggregate {:count tc/row-count})
    (tc/order-by :count :desc)
    (tc/head 10)
    (plotly/base {:=title "Top 10 Request Types"})
    (plotly/layer-bar {:=x :$group-name
                       :=mark-text :$group-name
                       :=y :count}))

;; How about least common?
(-> workshop-data
    (tc/group-by :REQUEST_TYPE)
    (tc/aggregate {:count tc/row-count})
    (tc/order-by :count :asc)
    (tc/head 10)
    (plotly/layer-bar {:=x :$group-name
                       :=mark-text :$group-name
                       :=y :count}))

;; What does FISCAL_MONTH look like?
(take 5 (workshop-data :FISCAL_MONTH))

;; This is a bit odd. Why 02-Aug? This is because it's the 
;; municipal fiscal calendar. Maybe we can look for another
;; date field, e.g. RECEIVED_DATE


(-> workshop-data
    (tc/select-columns :RECEIVED_DATE)
    (tc/head 10))

(def src-fmt
  (jt/formatter "yyyy/MM/dd HH:mm:ssX"))

;; Let's see how we can compress to a year - month representation
(-> workshop-data
    (tc/head 10)
    :RECEIVED_DATEf
    (->> (map
          (fn [s]
            (-> s
                (->> (jt/offset-date-time src-fmt))
                (jt/local-date)
                (jt/adjust :first-day-of-month))))))

;; we'll do that now in a fn
(defn to-year-month [datestr]
  (-> datestr
      (->> (jt/offset-date-time src-fmt))
      (jt/local-date)
      (jt/adjust :first-day-of-month)))

;; Okay lets' get the top five request types as a set
(def top-five-requests
  (-> workshop-data
      (tc/group-by :REQUEST_TYPE)
      (tc/aggregate {:count tc/row-count})
      (tc/order-by :count :desc)
      (tc/head 5)
      (tc/column :$group-name)
      set))

top-five-requests

(defn get-top5-data [time-converter]
  (-> workshop-data
      (tc/select-rows
       (fn [row]
         (top-five-requests (:REQUEST_TYPE row))))
      (tc/group-by (fn [row]
                     {:request-type (:REQUEST_TYPE row)
                      :date (-> row
                                :RECEIVED_DATE
                                time-converter)}))
      (tc/aggregate {:count tc/row-count})
      (tc/order-by :date)))

(-> (get-top5-data to-year-month) 
    (plotly/base
     {:=layout {:xaxis {:type "date"
                        :tickformat "%Y-%m"
                        :dtick "M1"
                        :tickangle -45
                        :hoverformat "%Y-%m"
                        :automargin true}
                :margin {:b 80}
                :legend {:orientation "h"
                         :xanchor "center" :x 0.5
                         :yanchor "top" :y 0.0}}})
    (plotly/layer-line {:=x :$group-name
                        :=color :request-type
                        :=y :count}))


;; Here we can see a bit of a pattern, but it's hard t
;; to tell. 


;; Group by month-of-year (1-12) instead of specific calendar month
(defn to-month-number [datestr]
  (-> datestr
      (->> (jt/offset-date-time src-fmt))
      (jt/local-date)
      (jt/month)
      .getValue))

(-> (get-top5-data to-month-number)
    (plotly/base
     {:=layout {:xaxis {:tickmode "array"
                        :tickvals [1 2 3 4 5 6 7 8 9 10 11 12]
                        :ticktext ["Jan" "Feb" "Mar" "Apr" "May" "Jun"
                                   "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]
                        :title "Month"}
                :yaxis {:title "Number of Requests"}
                :legend {:orientation "h"
                         :xanchor "center" :x 0.5
                         :yanchor "bottom" :y 1.02}}})  ; Move above plot
    (plotly/layer-line {:=x :$group-name
                        :=color :request-type
                        :=y :count}))

;; The peak in non_recyclable_items is interesting.

;; ## Concentration in council districts?

(-> workshop-data
    (tc/column-names))
;;=> (:X
;;    :Y
;;    :OBJECTID
;;    :DEPARTMENT
;;    :DIVISION
;;    :REQUEST_NO
;;    :FISCAL_YEAR
;;    :FISCAL_MONTH
;;    :REQUEST_TYPE
;;    :RECEIVED_DATE
;;    :INTERNAL_FIELD_OBSERVATION
;;    :TITLE
;;    :CMPD_DIVISION
;;    :NEIGHBORHOOD_PROFILE_AREA
;;    :BLOCK_NO
;;    :STREET_DIRECTION
;;    :STREET_NAME
;;    :STREET_TYPE
;;    :CITY
;;    :STATE
;;    :ZIP_CODE
;;    :FULL_ADDRESS
;;    :X_COORD
;;    :Y_COORD
;;    :LATITUDE
;;    :LONGITUDE
;;    :COUNCIL_DISTRICT
;;    :GlobalID
;;    :PID)

;; We can look at geographical data

;; let's see what geographic data we have
(-> workshop-data
    (tc/select-columns [:COUNCIL_DISTRICT :CMPD_DIVISION :NEIGHBORHOOD_PROFILE_AREA :ZIP_CODE])
    (tc/head 10))


(defn summer-month? [month-number]
  (and (> month-number 5) (< month-number 9)))

(-> workshop-data
    (tc/select-rows
     (comp #(= "NON_RECYCLABLE ITEMS" %) :REQUEST_TYPE))
    (tc/select-rows
     (comp 
      summer-month?
      to-month-number
      :RECEIVED_DATE))
    (tc/group-by :CMPD_DIVISION)
    (tc/aggregate {:count tc/row-count})
    (tc/order-by :count :desc) 
    (plotly/layer-bar
     {:=x :$group-name
      :=y :count}) 
    )

(-> workshop-data
    (tc/select-rows
     (comp #(= "NON_RECYCLABLE ITEMS" %) :REQUEST_TYPE))
    (tc/group-by (fn [row]
                   {:division (:CMPD_DIVISION row)
                    :season (if (summer-month? (to-month-number (:RECEIVED_DATE row)))
                              "Summer"
                              "Not Summer")}))
    (tc/aggregate {:count tc/row-count})

    ;; Now you have a map in :$group-name with :division and :season
    #_(tc/map-columns :division
                      [:$group-name]
                      (fn [& rows]))
    #_(tc/map-columns :season [:$group-name] :season)
    (plotly/layer-bar
       {:=x :division
        :=y :count
        :=color :season
        :=barmode "group"}))

;; this is interesting but let's look at it in terms
;; of monthly rate since the summer months are longer

;; (-> workshop-data
;;     (tc/select-rows
;;      (comp #(= "NON_RECYCLABLE ITEMS" %) :REQUEST_TYPE))
;;     (tc/group-by (fn [row]
;;                    {:division (:CMPD_DIVISION row)
;;                     :season (if (summer-month? (to-month-number (:RECEIVED_DATE row)))
;;                               "Summer"
;;                               "Not Summer")}))
;;     (tc/aggregate {:count tc/row-count})
;;     (tc/add-column :months-in-season
;;                    (fn [ds]
;;                      (tcc/emap {"Not Summer" 9.0
;;                                 "Summer" 3.0}
;;                                (:season ds))))
;;     (tc/add-column :monthly-rate
;;                    (fn [ds]
;;                      (tcc// (:count ds)
;;                             (:months-in-season ds))))
;;     (plotly/layer-bar
;;      {:=x :division
;;       :=y :monthly-rate
;;       :=color :season
;;       :=barmode "group"}))