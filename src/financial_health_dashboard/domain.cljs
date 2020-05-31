(ns financial-health-dashboard.domain
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as string]))


(defn year? [n] (boolean (and (number? n) (>= n 1900) (> 3000 n))))
(defn month? [n] (boolean (and (number? n) (>= n 1) (> 13 n))))

;; SPECS
(s/def :d/year year?)
(s/def :d/month month?)
(s/def :d/amount number?)


;; DATA TYPES
(def data-types-config
  [
   ["comment"
    []
    "A row used for any kind of comment"]
   ["salary"
    [:d/year :d/month :d/amount]
    "Salary"]])

(def data-types (->> data-types-config
                     (map (juxt first second))
                     (into {})))


(defn all-your-bucks [coll]
  (let [salaries coll]
    {:salaries salaries}))
