(ns financial-health-dashboard.core
  (:require [reagent.dom :as rd]
            [financial-health-dashboard.components :as c]))

(defn app
  "DOM entrypoint"
  []
  [:div.section
   [c/nav]])

(defn mount-reagent []
  (rd/render
    app
    (js/document.getElementById "app")))

(defn ^:dev/after-load start []
  (js/console.log "start")
  (mount-reagent))

(defn ^:export init []
  (js/console.log "init")
  (start))

(defn ^:dev/before-load stop []
  (js/console.log "stop"))
