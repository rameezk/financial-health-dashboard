(ns financial-health-dashboard.parse
  (:require
   [financial-health-dashboard.domain :as d]
   [cljs.spec.alpha :as s]
   [clojure.string :as str]
   [cljs.reader :as reader]
   [testdouble.cljs.csv :as csv]))

(def pipe "|")

(defn- not-empty-row? [row]
  (->> row
       (filter (comp not empty?))
       count
       (not= 0)))

(defn- parse-field
  [s]
  (let [value (reader/read-string s)]
    (if (symbol? value) s value)))

(defn- parse-row [row]
  (map
   (fn [field]
     (-> field
         str/trim
         parse-field))
   row))

(defn- validate-row-data [key data]
  (let [specs (get d/data-types key)]
    (->> specs
         (map-indexed
          (fn [i spec]
            (let [field (nth data i)]
              {:valid? (s/valid? spec field)
               :spec   spec
               :field  field}))))))

(defn- validate-row [[key & data]]
  (if-not (contains? d/data-types key)
    {:valid? false :error (str key " is an invalid data type") :data data}
    (let [validation (validate-row-data key data)
          specs (->> (get d/data-types key))]
      (if (empty? (->> validation (map :valid?) (filter false?)))
        {:valid? true :specs specs :data data :key (keyword key)}
        (let [failed-specs (->> validation
                                (filter (comp false? :valid?))
                                (map :spec))]
          {:valid false :data data
           :specs specs
           :key (keyword key)
           :failed-specs failed-specs
           :error (str key " has invalid values for "
                       (map name failed-specs)
                       ". Expected " (map name specs))})))))

(defn parse
  [seperator data]
  (->> (csv/read-csv data :separator seperator)
       (filter not-empty-row?)
       (map-indexed (fn [i r]
                      (-> r parse-row validate-row (assoc :i (inc i)))))))


(defn- as-domain-value [{:keys [specs key data]}]
  (-> (zipmap
        (map (comp keyword name) specs)
        (take (count specs) data))
      (assoc :data-type key)))

(defn as-domain-values
  "Converts a collection of parsed and valid data rows into domain data."
  [rows]
  (->> rows
       (filter :valid?)
       (map as-domain-value)))

