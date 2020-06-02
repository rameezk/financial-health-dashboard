(ns financial-health-dashboard.scratch
  (:require
   [financial-health-dashboard.parse :as parse]
   [financial-health-dashboard.example :as example]))

(defn load-example-data []
  (parse/as-domain-values (parse/parse "|" example/data-piped)))
