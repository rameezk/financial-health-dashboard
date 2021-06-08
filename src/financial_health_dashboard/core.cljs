(ns financial-health-dashboard.core
  (:require [reagent.dom :as rd]))

(defn ^:dev/after-load start []
  (js/console.log "start"))

(defn ^:export init []
  (js/console.log "init")
  (start))

(defn ^:dev/before-load stop []
  (js/console.log "stop"))
