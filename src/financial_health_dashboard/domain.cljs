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
  [
   ["comment"
    []
    "A row used for any kind of comment"]
   ["salary"
    [:d/year :d/month :d/amount]
    "Salary"]
   ["year-goal"
    [:d/year :d/amount]]])

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
;; (defn year-goals [data]
;;   (->> (filter (type-of-f? :year-goal) data)
;;        (map (fn [{:keys [percentage] :as m}]
;;               (assoc m :name (str percentage "%"))))
;;        (group-by :year)))

(defn year-goals [data]
  (->> (filter (type-of-f? :year-goal) data)))

(defn all-your-bucks [data]
  (let [salaries (salaries data)]
    {:salaries salaries}))
