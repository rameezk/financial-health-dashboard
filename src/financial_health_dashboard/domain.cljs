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
   :2021 36000
   :2022 36000})

(def fi-yearly-withdrawal-rate 0.04)
(def fi-monthly-withdrawal-rate (/ fi-yearly-withdrawal-rate 12))


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
   ["income"
    [:d/name :d/year :d/month :d/amount]
    "Income"]
   ["expense"
    [:d/name :d/year :d/month :d/amount]
    "Expense"]
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
    [:d/year :d/month :d/amount]]
   ["fi-expense"
    [:d/amount]]])

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


;; DATA EXTRACTORS
(defn income [data]
  (->> data (filter (type-of-f? :income))
       (map timestamped)
       (sort-by :timestamp)))

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
       (into (sorted-map))))

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
              [g (->> t (map :amount) (reduce +))]))
       (into {})))

(defn tfsa-contributions-over-lifetime [contributions]
  (let [total (->> contributions (map :amount) (reduce +))]
    {:amount total :limit tfsa-lifetime-limit}))

(defn map-tfsa-yearly-limits [contributions]
  (->> contributions (map #(assoc %
                                  :limit (get tfsa-limits
                                              (keyword (str (:year %))))))))

(defn expenses [data]
  (->> data (filter (type-of-f? :expense))
       (map timestamped)
       (sort-by :timestamp)))

(defn fi-expense [data]
  (last (->> data (filter (type-of-f? :fi-expense)))))


(defn investments [assets]
  (->> assets
       (filter #(or
                  (= (:name %) "tfsa")
                  (= (:name %) "liberty-ra")
                  (= (:name %) "liberty-preservation-fund")
                  (= (:name %) "sygnia-ra")
                  (= (:name %) "td-ameritrade")
                  (= (:name %) "education-fund")))
       (map #(assoc % :grouping [(:year %) (:month %)]))
       (group-by :grouping)
       (map (fn [[g t]]
              [g (->> t (map :amount) (reduce +))]))
       (into {})))

(defn investments-change [investments]
  (if (> (count investments) 1)
    (let [latest-amount      (:amount (last investments ))
          second-last-amount (:amount (first (take-last 2 investments)))
          delta              (- latest-amount second-last-amount)]
      (if (zero? delta)
        {:direction :same :delta delta :percentage 0}
        (if (neg? delta)
          {:direction :down :delta (* -1 delta) :percentage (* (/ delta second-last-amount) -100)}
          {:direction :up :delta delta :percentage (* (/ delta second-last-amount) 100)})))
    nil))

(defn fi-investments [assets]
  (->> assets
       (filter #(or
                  (= (:name %) "tfsa")
                  (= (:name %) "td-ameritrade")
                  (= (:name %) "valr-btc")
                  (= (:name %) "valr-eth")
                  (= (:name %) "luno-btc")
                  (= (:name %) "liberty-preservation-fund")
                  (= (:name %) "sygnia-ra")))
       (map #(assoc % :grouping [(:year %) (:month %)]))
       (group-by :grouping)
       (map (fn [[g t]]
              [g (->> t (map :amount) (reduce +))]))
       (into {})))

(defn fi-investments-change [fi-investments]
  (if (> (count fi-investments) 1)
    (let [latest-amount      (:amount (last fi-investments ))
          second-last-amount (:amount (first (take-last 2 fi-investments)))
          delta              (- latest-amount second-last-amount)]
      (if (zero? delta)
        {:direction :same :delta delta :percentage 0}
        (if (neg? delta)
          {:direction :down :delta (* -1 delta) :percentage (* (/ delta second-last-amount) -100)}
          {:direction :up :delta delta :percentage (* (/ delta second-last-amount) 100)})))
    nil))

(defn map-fi-withdrawals [investments]
  (->> investments (map #(assoc %
                                :fi-monthly-withdrawal (Math/ceil ( * fi-monthly-withdrawal-rate (:amount %) ))))))

(defn map-fi-percentage [fi-expense investments]
  (->> investments (map #(assoc %
                                :fi-percentage (* (/ (:amount %) (* (:amount fi-expense) 300)) 100)))))



(defn fi-monthly-withdrawal-change [fi-investments]
  (if (> (count fi-investments) 1)
    (let [latest-amount      (:fi-monthly-withdrawal (last fi-investments ))
          second-last-amount (:fi-monthly-withdrawal (first (take-last 2 fi-investments)))
          delta              (- latest-amount second-last-amount)]
      (if (zero? delta)
        {:direction :same :delta delta :percentage 0}
        (if (neg? delta)
          {:direction :down :delta (* -1 delta) :percentage (* (/ delta second-last-amount) -100)}
          {:direction :up :delta delta :percentage (* (/ delta second-last-amount) 100)})))
    nil))


(defn all-your-bucks[data]
  (let [sample                           (sample data)
        income                           (income data)
        expenses                         (->> data (expenses) (net-per-month) (make-series-from-grouped-data))
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
        tfsa-contributions-over-lifetime (tfsa-contributions-over-lifetime tfsa-contributions)
        fi-expense                       (fi-expense data)
        fi-investments                   (map-fi-percentage fi-expense ( map-fi-withdrawals (make-series-from-grouped-data (fi-investments assets)) ))
        investments                      (make-series-from-grouped-data (investments assets))
        investments-change               (investments-change investments)
        fi-investments-change            (fi-investments-change fi-investments)
        fi-monthly-withdrawal-change     (fi-monthly-withdrawal-change fi-investments)
        ]
    {:sample                           sample
     :income                           income
     :expenses                         expenses
     :emergency-fund-months            emergency-fund-months
     :emergency-fund-months-change     emergency-fund-months-change
     :net-worths                       net-worths
     :net-worth                        net-worth
     :net-worth-change                 net-worth-change
     :net-assets                       net-assets-series
     :net-liabilities                  net-liabilities-series
     :tfsa-contributions-per-year      tfsa-contributions-per-year
     :tfsa-contributions-over-lifetime tfsa-contributions-over-lifetime
     :fi-investments                   fi-investments
     :fi-investments-change            fi-investments-change
     :investments                      investments
     :investments-change               investments-change
     :fi-monthly-withdrawal-change     fi-monthly-withdrawal-change}))

