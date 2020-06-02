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

(def example-comment
  [["comment" "some random comment"]])

(def data-piped
  (->> [date-of-birth
        year-goals]
       (reduce into)
       (map #(->> % (map str) (str/join "|")))
       (str/join "\n")))

