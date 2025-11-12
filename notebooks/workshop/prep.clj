(ns workshop.prep
  (:require [tablecloth.api :as tc]))

;; ## Loading Charlotte 311 Data
;; We're using Charlotte's public 311 service request data
;; Source: https://data.charlottenc.gov/datasets/charlotte::service-requests-311/about

(def raw-data
  (tc/dataset "data/Service_Requests_311.csv"
              {:key-fn keyword}))

;; Quick look at what we have. We can use this decriptive statistics function.
;; And we can start to use another tool here called Clay that allows us to render
;; the result of our code in a browser. 
(tc/info raw-data)

;; So from this we can see that we have data from 2017 until (oddly) 2028. 
;; We'll filter out the magical future call data. 

;; ## Creating Our Workshop Dataset

;; Here we use our first processing function: select-rows. Observations:
;;   - The dataset is the first argument
;;   - The second argument is a selector function that takes each row
;;   - And look: the row is a map. We are accessing it using the standard
;;     Clojure keyword accessor. 
(def workshop-data
  (tc/select-rows raw-data
                  (fn [row]
                    (<= (:FISCAL_YEAR row) 2024))))

;; How many requests per year? Our first tablecloth processing expression!
(-> workshop-data
    (tc/group-by [:FISCAL_YEAR])
    (tc/aggregate {:COUNT tc/row-count}))

;; Filtering down & writing
(-> workshop-data
    (tc/group-by :FISCAL_YEAR)
    (tc/random 30000 {:seed 42 :repeat? false})
    (tc/ungroup)
    (tc/write! "data/clt-311-workshop.csv"))

;; Lets' stop for a second and think about these expressions  We are using
;; Clojure's arrow macro to pipe data through mutiple functions. This is a
;; pattern you will see frequently from hear on out. And this is actually
;; amazing! 
;; 
;; At each step what is returned is a dataset. By design, each function takes
;; the dataset as the first argument. And its optimized. This is what we call
;; "Functional Data Science". Nothing is being mutated, and we avoid polluting
;; the global environment with variables.
;; 
;;  By constrast, in Pandas you would often do the above this way:
;;   df = pd.read_csv ('charlotte_311.csv')
;;   df = df.groupby ('FISCAL_YEAR').size().reset_index(name='COUNT')
;;   df = df.sort_values ('FISCAL_YEAR')
;; 
;; This is fine, but you've lost the original dataset. To preserve it, you'll need
;; to create a new copy. We do this in Clojure too, as you'll see, but we don't
;; need to do it as often.
