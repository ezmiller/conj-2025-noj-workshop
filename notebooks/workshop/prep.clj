(ns workshop.prep
  (:require [tablecloth.api :as tc]))

;; ## Loading Charlotte 311 Data
;; We're using Charlotte's public 311 service request data
;; Source: https://data.charlottenc.gov/datasets/charlotte::service-requests-311/about

(def raw-data
  (tc/dataset "notebooks/data/Service_Requests_311.csv"
              {:key-fn keyword}))

;; Quick look at what we have
(tc/info raw-data)

;; ~2M rows. That's manageable but let's focus on recent years.

;; ## Creating Our Workshop Dataset

;; Filter to recent years (2020-2025)
(def workshop-data
  (tc/select-rows raw-data
                  (fn [row]
                    (and (>= (:FISCAL_YEAR row) 2020)
                         (<= (:FISCAL_YEAR row) 2025)))))

;; How many requests per year?
(-> workshop-data
    (tc/group-by :FISCAL_YEAR)
    (tc/aggregate {:count tc/row-count}))

;; Pretty balanced! Between 295k-355k per year.
;; That's about 2M rows total - good size for the workshop
;; bug a big file. Let's slim it down a bit and write to disk

(-> workshop-data
    (tc/group-by :FISCAL_YEAR)
    (tc/random 100000 {:seed 42 :repeat? false})
    (tc/ungroup)
    (tc/write! "notebooks/data/clt-311-workshop.csv"))
