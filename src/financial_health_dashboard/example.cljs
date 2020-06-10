(ns financial-health-dashboard.example
  (:require [cljs-time.core :as time]
            [clojure.string :as str]))

(def now (time/now))

(def this-year (time/year now))

(def last-year (dec this-year))

(def date-of-birth
  [["date-of-birth" 1985 11 6]])

(def year-goals
  [["year-goal" this-year 15]
   ["year-goal" this-year 13]
   ["year-goal" this-year 11]])

(def salary
  [["salary" this-year 1 20000]
   ["salary" this-year 2 18000]
   ["salary" this-year 3 22000]])

(def monthly-expense
  [["monthly-expense" this-year 1 20000]])

(def emergency-fund
  [["emergency-fund" this-year 3 50000]
   ["emergency-fund" this-year 4 60000]])

(def example-comment
  [["comment" "some random comment"]])

(def sample
  [["sample" "yes"]])

(def assets
  [["asset" "emergency-fund" this-year 1 10000]
   ["asset" "emergency-fund" this-year 2 20000]
   ["asset" "TFSA" this-year 1 30000]
   ["asset" "TFSA" this-year 2 40000]])

(def liabilities
  [["liability" "home-loan" this-year 1 8000]
   ["liability" "home-loan" this-year 2 6000]])

(def data-piped
  (->> [sample
        date-of-birth
        year-goals
        salary
        monthly-expense
        emergency-fund
        assets
        liabilities]
       (reduce into)
       (map #(->> % (map str) (str/join "|")))
       (str/join "\n")))

