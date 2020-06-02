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

(def monthly-expense
  [["monthly-expense" this-year 1 20000]])

(def emergency-fund
  [["emergency-fund" this-year 5 60000]])

(def example-comment
  [["comment" "some random comment"]])

(def data-piped
  (->> [date-of-birth
        year-goals
        monthly-expense
        emergency-fund]
       (reduce into)
       (map #(->> % (map str) (str/join "|")))
       (str/join "\n")))

