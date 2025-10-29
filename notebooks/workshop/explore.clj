(ns workshop.explore
  (:require [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]
            [scicloj.tableplot.v1.plotly :as plotly]
            [java-time :as jt]
            [java-time.format :as fmt]
            [clojure.core :as c]))

(defonce workshop-data
  (tc/dataset "notebooks/data/"))

