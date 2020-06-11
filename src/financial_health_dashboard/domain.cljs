(ns financial-health-dashboard.domain
  (:require
   [clojure.spec.alpha :as s]
   [cljs-time.core :as time]))

(defn not-empty-string? [s] (and (string? s)
                                 (not-empty s)))
(defn year? [n] (boolean (and (number? n) (>= n 1900) (> 3000 n))))
(defn month? [n] (boolean (and (number? n) (>= n 1) (> 13 n))))
(def yes "yes")
(def no "no")

(def tfsa-lifetime-limit 500000)

(def tfsa-limits
  {:2016 30000
   :2017 30000
   :2018 33000
   :2019 33000
   :2020 33000
   :2021 36000})


;; SPECS


(s/def :d/year year?)
(s/def :d/month month?)
(s/def :d/amount number?)
(s/def :d/name not-empty-string?)
(s/def :d/is-sample #(contains? #{yes no} %))


;; DATA TYPES


(def data-types-config
  [["sample"
    [:d/is-sample]
    "Indicate sample data"]
   ["comment"
    []
    "A row used for any kind of comment"]
   ["salary"
    [:d/year :d/month :d/amount]
    "Salary"]
   ["emergency-monthly-expense"
    [:d/year :d/month :d/amount]
    "Monthly expense. This doesn't include contributions to RA's, investments or savings accounts."]
   ["emergency-fund"
    [:d/year :d/month :d/amount]
    "Emergency fund balance."]
   ["asset"
    [:d/name :d/year :d/month :d/amount]]
   ["liability"
    [:d/name :d/year :d/month :d/amount]]
   ["tfsa-contribution"
    [:d/year :d/month :d/amount]]])

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
(defn emergency-monthly-expense [data]
  (->> data (filter (type-of-f? :emergency-monthly-expense))
       (map timestamped)
       (sort-by :timestamp)))

(defn emergency-fund [data]
  (->> data (filter (type-of-f? :emergency-fund))
       (map timestamped)
       (sort-by :timestamp)))

(defn emergency-fund-months [emergency-fund emergency-monthly-expense]
  (let [latest-emergency-monthly-expense (get (last emergency-monthly-expense) :amount)
        latest-emergency-fund-balance    (get (last emergency-fund) :amount)]
    (/ latest-emergency-fund-balance latest-emergency-monthly-expense)))

(defn emergency-fund-months-change [emergency-fund emergency-monthly-expense]
  (if (> (count emergency-fund) 1)
    (let [latest-emergency-monthly-expense (get (last emergency-monthly-expense) :amount)
          latest-em-fund-balance           (get (last emergency-fund) :amount)
          second-last-em-fund-balance      (get (-> emergency-fund (reverse) (nth 1 nil)) :amount)
          change-in-amount                 (- latest-em-fund-balance second-last-em-fund-balance)
          delta                            (/ change-in-amount latest-emergency-monthly-expense)]
      (if (= delta 0.0)
        {:direction :same :delta delta :percentage 0}
        (if (< delta 0)
          {:direction :down :delta (* -1 delta) :percentage (* (/ change-in-amount second-last-em-fund-balance) -100)}
          {:direction :up :delta delta :percentage (* (/ change-in-amount second-last-em-fund-balance) 100)})))
    nil))

(defn sample [data]
  (->> data (filter (type-of-f? :sample))))

(defn assets [data]
  (->> data (filter (type-of-f? :asset))
       (map timestamped)
       (sort-by :timestamp)))

(defn net-per-month [data]
  (->> data (map #(assoc % :grouping [(:year %) (:month %)]))
       (group-by :grouping)
       (map (fn [[g t]]
              [g (->> t (map :amount) (reduce +))]))
       (into {})))

(defn liabilities [data]
  (->> data (filter (type-of-f? :liability))
       (map timestamped)
       (sort-by :timestamp)))

(defn net-worth
  ([net-assets net-liabilities] (net-worth net-assets net-liabilities 1))
  ([net-assets net-liabilities n-last]
   (let [latest-monthly-net-assets      (->> net-assets (take-last n-last) (first) (second))
         latest-monthly-net-liabilities (->> net-liabilities (take-last n-last) (first) (second))]
     (- latest-monthly-net-assets latest-monthly-net-liabilities))))

(defn net-worth-change [net-assets-per-month net-liabilities-per-month]
  (if (or (> (count net-assets-per-month) 1) (> (count net-liabilities-per-month) 1))
    (let [latest-net-worth      (net-worth net-assets-per-month net-liabilities-per-month)
          second-last-net-worth (net-worth net-assets-per-month net-liabilities-per-month 2)
          delta                 (- latest-net-worth second-last-net-worth)]
      (if (zero? delta)
        {:direction :same :delta delta :percentage 0}
        (if (neg? delta)
          {:direction :down :delta (* -1 delta) :percentage (* (/ delta second-last-net-worth) -100)}
          {:direction :up :delta delta :percentage (* (/ delta second-last-net-worth) 100)})))
    nil))

(defn flatten-grouped-months [grouped-months]
  (map (fn [[[y m] a]] {:year y :month m :amount a}) grouped-months))

(defn net-worths [assets liabilities]
  (let [l  (->> liabilities (map #(assoc % :amount (* -1 (:amount %)))))
        al (concat assets l)]
    (->> al (map #(assoc % :grouping [(:year %) (:month %)]))
         (group-by :grouping)
         (map (fn [[g t]]
                [g (->> t (map :amount) (reduce +))]))
         (into {}))))

(defn make-series-from-grouped-data [grouped-data]
  (->> grouped-data (flatten-grouped-months)
       (map timestamped)
       (sort-by :timestamp)))

(defn tfsa-contributions [data]
  (->> data (filter (type-of-f? :tfsa-contribution))
       (map timestamped)
       (sort-by :timestamped)))

(defn tax-year [date]
  (let [year  (time/year date)
        month (time/month date)]
    (if (> month 2) (inc year) year)))

(defn tfsa-contributions-per-year [contributions]
  (->> contributions (map #(assoc % :tax-year [(tax-year (:cljs-date %))]))
       (group-by :tax-year)
       (map (fn [[g t]]
              (println g)
              [g (->> t (map :amount) (reduce +))]))
       (into {})))

(defn tfsa-contributions-over-lifetime [contributions]
  (let [total (->> contributions (map :amount) (reduce +))
        limit tfsa-lifetime-limit]
    {:amount total :limit limit}))

(defn map-tfsa-yearly-limits [contributions]
  (->> contributions (map #(assoc %
                                  :limit (get tfsa-limits
                                              (keyword (str (:year %))))))))

(defn all-your-bucks [data]
  (let [sample                           (sample data)
        salaries                         (salaries data)
        emergency-fund                   (emergency-fund data)
        emergency-monthly-expense        (emergency-monthly-expense data)
        emergency-fund-months            (emergency-fund-months emergency-fund emergency-monthly-expense)
        emergency-fund-months-change     (emergency-fund-months-change emergency-fund emergency-monthly-expense)
        assets                           (assets data)
        net-assets-per-month             (net-per-month assets)
        net-assets-series                (make-series-from-grouped-data net-assets-per-month)
        liabilities                      (liabilities data)
        net-liabilities-per-month        (net-per-month liabilities)
        net-liabilities-series           (make-series-from-grouped-data net-liabilities-per-month)
        net-worth                        (net-worth net-assets-per-month net-liabilities-per-month)
        net-worths                       (make-series-from-grouped-data (net-worths assets liabilities))
        net-worth-change                 (net-worth-change net-assets-per-month net-liabilities-per-month)
        tfsa-contributions               (tfsa-contributions data)
        tfsa-contributions-per-year      (->> tfsa-contributions
                                              (tfsa-contributions-per-year)
                                              (make-series-from-grouped-data)
                                              (map-tfsa-yearly-limits))
        tfsa-contributions-over-lifetime (tfsa-contributions-over-lifetime tfsa-contributions)]
    {:sample                           sample
     :salaries                         salaries
     :emergency-fund-months            emergency-fund-months
     :emergency-fund-months-change     emergency-fund-months-change
     :net-worths                       net-worths
     :net-worth                        net-worth
     :net-worth-change                 net-worth-change
     :net-assets                       net-assets-series
     :net-liabilities                  net-liabilities-series
     :tfsa-contributions-per-year      tfsa-contributions-per-year
     :tfsa-contributions-over-lifetime tfsa-contributions-over-lifetime}))

