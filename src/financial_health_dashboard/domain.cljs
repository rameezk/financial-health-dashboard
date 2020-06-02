(ns financial-health-dashboard.domain
  (:require
   [clojure.spec.alpha :as s]
   [cljs-time.core :as time]
   [cljs-time.coerce :as time.coerce]
   [clojure.string :as string]))

(defn year? [n] (boolean (and (number? n) (>= n 1900) (> 3000 n))))
(defn month? [n] (boolean (and (number? n) (>= n 1) (> 13 n))))

;; SPECS
(s/def :d/year year?)
(s/def :d/month month?)
(s/def :d/amount number?)


;; DATA TYPES


(def data-types-config
  [["comment"
    []
    "A row used for any kind of comment"]
   ["salary"
    [:d/year :d/month :d/amount]
    "Salary"]
   ["monthly-expense"
    [:d/year :d/month :d/amount]
    "Monthly expense. This doesn't include contributions to RA's, investments or savings accounts."]
   ["emergency-fund"
    [:d/year :d/month :d/amount]
    "Emergency fund balance."]])

(def data-types (->> data-types-config
                     (map (juxt first second))
                     (into {})))

(defn type-of? [data-type m]
  (= data-type (:data-type m)))

(defn type-of-f? [data-type] (partial type-of? data-type))

(defn types-of-f? [& types]
  (fn [m] (->> types
              (map #(type-of? % m))
              (filter true?)
              not-empty)))

(defn timestamped [{:keys [year month day] :as m}]
  (let [date (js/Date. year (dec month) day)] ;;js months start at 0
    (assoc m
           :date date
           :cljs-date (time/date-time year month day)
           :timestamp (.getTime date))))

(defn salaries [data]
  (->> data (filter (type-of-f? :salary))
       (map timestamped)
       (sort-by :timestamp)))

;; DATA EXTRACTORS
(defn monthly-expense [data]
  (->> data (filter (type-of-f? :monthly-expense))
       (map timestamped)
       (sort-by :timestamp)))

(defn emergency-fund [data]
  (->> data (filter (type-of-f? :emergency-fund))
       (map timestamped)
       (sort-by :timestamp)))

(defn emergency-fund-months [emergency-fund monthly-expense]
  (let [latest-monthly-expense        (get (last monthly-expense) :amount)
        latest-emergency-fund-balance (get (last emergency-fund) :amount)]
    (/ latest-emergency-fund-balance latest-monthly-expense)))

(defn all-your-bucks [data]
  (let [salaries              (salaries data)
        emergency-fund        (emergency-fund data)
        monthly-expense       (monthly-expense data)
        emergency-fund-months (emergency-fund-months emergency-fund monthly-expense)]
    {:salaries              salaries
     :emergency-fund-months emergency-fund-months}))

