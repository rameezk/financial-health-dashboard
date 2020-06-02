(ns financial-health-dashboard.example
  (:require [cljs-time.core :as time]))

(def now (time/now))

(def this-year (time/year now))

(def last-year (dec this-year))

(def date-of-birth
  [["date-of-birth" 1985 11 6]])
